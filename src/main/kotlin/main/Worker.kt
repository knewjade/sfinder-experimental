package main

import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import common.parser.StringEnumTransform
import core.mino.Piece
import exceptions.FinderExecuteCancelException
import lib.Stopwatch
import main.aws.AWS
import main.aws.SQSMessage
import main.caller.ContentCaller
import main.caller.InvokerCaller
import main.domain.*
import main.invoker.CalculatorMessageInvoker
import main.invoker.FileBaseMessageInvoker
import main.percent.CachedSolutionLoader
import main.percent.Index
import main.percent.ResultsSerializer
import main.percent.SolutionLoader
import java.util.concurrent.TimeUnit

class Worker(
        private val aws: AWS,
        private val minimumSuccessRate: Double,
        private val isService: Boolean,
        private val factories: Factories,
        private val index: Index,
        private val allMinoIndexes: AllMinoIndexes,
        private val calculate: Boolean
) {
    fun invoke() {
        while (true) {
            val message = aws.receiveMessage()

            if (message == null) {
                println("[skip] no message -> sleep")
                if (!isService)
                    return
                Thread.sleep(TimeUnit.SECONDS.toMillis(20L))
                continue
            }

            println("message-id: ${message.messageId}")
            println("message-body: ${message.body}")
            println("Memory: ${getMemoryInfo()}")

            val stopwatch = Stopwatch.createStartedStopwatch()

            search(message)

            message.delete()

            stopwatch.stop()
            println(stopwatch.toMessage(TimeUnit.SECONDS))
            println(stopwatch.toMessage(TimeUnit.MINUTES))
        }
    }

    @Throws(FinderExecuteCancelException::class)
    private fun search(message: SQSMessage) {
        val split = message.body.trim().split(",")
        val cycle = Cycle(split[0].toInt())
        val fieldData = FieldData(split[1])
        val numbers = split[2].takeIf { it.isNotBlank() }?.split("_")?.map { it.toInt() } ?: listOf()
        val minos = numbers.map { index.get(it)!! }
        val current = StringEnumTransform.toPiece(split[3])
        val headPieces = HeadPieces(minos, current)

        if (10 <= headPieces.headMinos.size) {
            println("[skip] over piece")
            return
        }

        val resultPath = ResultPath(cycle, headPieces, fieldData)
        val caller = if (aws.existsObject(resultPath.path)) {
            println("already exists: ${resultPath.path}")
            val content = aws.getObject(resultPath.path)!!
            ContentCaller(content)
        } else {
            val invoker = if (calculate) {
                CalculatorMessageInvoker(headPieces, factories, index)
            } else {
                val headIndexes = headPieces.headMinos.map { index.get(it)!! }.toSet()
                val solutionLoader: SolutionLoader = CachedSolutionLoader(allMinoIndexes, index, headIndexes)
                FileBaseMessageInvoker(headPieces, factories, index, solutionLoader)
            }
            val serializer = ResultsSerializer()
            InvokerCaller(aws, invoker, resultPath, cycle, serializer)
        }

        val results = caller.call()
        results.takeIf { 0 < it.allCount.value }?.let { value ->
            val (allCount, details) = value
            val nextSearchCount = allCount.value * minimumSuccessRate
            details.filter { nextSearchCount <= it.success.value }.forEach { send(cycle, headPieces, it) }
        }
    }

    private fun send(cycle: Cycle, headPieces: HeadPieces, detail: Result) {
        val entries = Piece.values().map {
            val field = detail.fieldData

            val minos = headPieces.headMinos.map { index.get(it)!! } + detail.mino.index
            val numbers = minos.sorted().joinToString("_")

            val batchId = String.format("%d-%s-%s-%s", cycle.number, field.messageId, numbers, it.name)
            val body = String.format("%d,%s,%s,%s", cycle.number, field.raw, numbers, it.name)
            SendMessageBatchRequestEntry(batchId, body)
        }

        println(entries.toString())

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