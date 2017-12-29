package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import core.mino.Piece
import exceptions.FinderExecuteCancelException
import lib.Stopwatch
import percent.Index
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


class Worker(
        val bucketName: String,
        val receiverQueryName: String,
        val senderQueryName: String,
        val minimumSuccessRate: Double
) {
    fun work() {
        val s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.AP_NORTHEAST_1)
                .build()

        val sqsClient = AmazonSQSClient.builder()
                .withRegion(Regions.AP_NORTHEAST_1)
                .build()

        val receiverSQS = SQS(sqsClient, receiverQueryName)
        val senderSQS = SQS(sqsClient, senderQueryName)
        val bucket = Bucket(s3Client, bucketName)
        val aws = AWS(receiverSQS, senderSQS, bucket)

        val factories = createFactories()

        val index = Index(factories.minoFactory, factories.minoShifter, Paths.get("input/index.csv"))

        try {
            run(aws, minimumSuccessRate, factories, index)
        } finally {
            s3Client.shutdown()
            sqsClient.shutdown()
        }
    }

    private fun run(aws: AWS, threshold: Double, factories: Factories, index: Index) {
        while (true) {
            val message = aws.receiveMessage()

            if (message == null) {
                println("[skip] no message -> sleep")
                Thread.sleep(TimeUnit.SECONDS.toMillis(10L))
                continue
            }

            println("message-id: ${message.messageId}")

            val stopwatch = Stopwatch.createStartedStopwatch()

            try {
                search(aws, message, threshold, factories, index)
            } catch (e: FinderExecuteCancelException) {
                println("[info] ${e.message} -> duplicate")
                message.duplicate()
            }

            stopwatch.stop()
            println(stopwatch.toMessage(TimeUnit.SECONDS))
            println(stopwatch.toMessage(TimeUnit.MINUTES))

            message.delete()
        }
    }

    @Throws(FinderExecuteCancelException::class)
    private fun search(aws: AWS, message: SQSMessage, threshold: Double, factories: Factories, index: Index) {
        val split = message.body.trim().split(",")
        println(split)
        val input = Input(index, split[0], split[1], split[2])
        println(input)

        val invoker = LoadBaseMessageInvoker(input, factories, index)
//        val invoker = DummyInvoker(input, factories, index)
        val caller = Caller(aws, input, invoker)

        val store = mutableMapOf<Int, Results>()
        (1..8).forEach { cycle ->
            val progressTimer = message.progressTimer()
            println("Progress Time: ${progressTimer * 100} %")
            if (0.8 < progressTimer) {
                throw FinderExecuteCancelException("message timeout")
            }

            println("Memory: ${getMemoryInfo()}")

            println("Cycle: $cycle")

            val results = caller.search(cycle)

            results?.also {
                store[cycle] = it
            } ?: println("[skip] no invoke")
        }

        store.values.flatMap { value ->
            val (allCount, details) = value
            val nextSearchCount = allCount * threshold
            details.filter { nextSearchCount <= it.success }
        }.toSet().forEach {
            send(input, it, aws)
        }
    }

    private fun send(input: Input, detail: Result, aws: AWS) {
        val entries = Piece.values().map {
            val numbers = (input.headPiecesInt + detail.mino).sorted().joinToString("_")
            val batchId = String.format("%s-%s-%s", detail.fieldData, numbers, it.name)
            val body = String.format("%s,%s,%s", detail.fieldData, numbers, it.name)
            SendMessageBatchRequestEntry(batchId, body)
        }

        println(entries)
        aws.sendShortMessages(entries)
    }

    private fun getMemoryInfo(): String {
        class GB(val v: Double) {
            operator fun minus(other: GB): GB {
                return GB(v - other.v)
            }

            override fun toString(): String {
                return String.format("%.3fGB", v)
            }
        }

        fun toGB(v: Long): GB {
            return GB(v.toDouble() / 1024 / 1024 / 1024)
        }

        return Runtime.getRuntime().run {
            val free = toGB(freeMemory())
            val total = toGB(totalMemory())
            val max = toGB(maxMemory())
            val used = total - free
            "used=$used, total=$total, max=$max"
        }
    }
}
