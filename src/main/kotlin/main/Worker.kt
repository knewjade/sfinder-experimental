package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import core.mino.Piece
import lib.Stopwatch
import percent.Index
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun workSQS(
        bucketName: String,
        receiverQueryName: String,
        senderQueryName: String,
        minimumSuccessRate: Double
) {
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
            Thread.sleep(TimeUnit.SECONDS.toMillis(5L))
            continue
        }

        println("message-id: ${message.messageId}")

        val stopwatch = Stopwatch.createStartedStopwatch()

        val split = message.body.trim().split(",")
        println(split)
        val input = Input(index, split[0], split[1], split[2])
        println(input)

        search(aws, input, threshold, factories, index)

        stopwatch.stop()
        println(stopwatch.toMessage(TimeUnit.SECONDS))
        println(stopwatch.toMessage(TimeUnit.MINUTES))

        aws.deleteMessage(message)
    }
}

private fun search(aws: AWS, input: Input, threshold: Double, factories: Factories, index: Index) {
    fun path(cycle: Int): String {
        return "$cycle/" + input.prefixPath
    }

    val invoker = LoadBaseMessageInvoker(input, factories, index)

    val store = mutableMapOf<Int, Results>()
    (1..8).forEach { cycle ->
        println("Cycle: $cycle")

        val path = path(cycle)
        if (aws.existsObject(path)) {
            println("[skip] result exists already")
            return@forEach
        }

        val results = invoker.invoke(cycle)
        results?.also {
            store[cycle] = results
        } ?: println("[skip] no invoke")
    }

    store.values.flatMap { value ->
        val (allCount, details) = value
        val nextSearchCount = allCount * threshold
        details.filter { nextSearchCount <= it.success }
    }.toSet().forEach {
        send(input, it, aws)
    }

    store.entries.forEach { entry ->
        val cycle = entry.key
        val results = entry.value

        val path = path(cycle)
        put(aws, results, path)
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

private fun put(aws: AWS, results: Results, path: String) {
    val (allCount, details) = results

    val output = details.joinToString(";") {
        "${it.mino},${it.success}"
    }

    val content = "$allCount?$output"
    aws.putObject(path, content)
}