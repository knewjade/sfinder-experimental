package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import common.tetfu.common.ColorConverter
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import lib.Stopwatch
import searcher.common.validator.PerfectValidator
import java.util.concurrent.TimeUnit

fun workSQS(bucketName: String, receiverQueryName: String, shortSenderQueryName: String, longSenderQueryName: String, minimumSuccessRate: Double, multiThread: Boolean) {
    val s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val sqsClient = AmazonSQSClient.builder()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val receiverSQS = SQS(sqsClient, receiverQueryName)
    val shortSenderSQS = SQS(sqsClient, shortSenderQueryName)
    val longSenderSQS = SQS(sqsClient, longSenderQueryName)
    val bucket = Bucket(s3Client, bucketName)
    val aws = AWS(receiverSQS, shortSenderSQS, longSenderSQS, bucket)

    val factories = createFactories()
    val invoker = if (multiThread) MultiThreadMessageInvoker(factories, minimumSuccessRate) else SingleThreadMessageInvoker(factories, minimumSuccessRate)
//    val invoker = ManualInvoker(factories)

    try {
        run(aws, invoker, minimumSuccessRate)
    } finally {
        s3Client.shutdown()
        sqsClient.shutdown()
        invoker.shutdown()
    }
}

private fun createFactories(): Factories {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()
    val colorConverter = ColorConverter()
    val perfectValidator = PerfectValidator()
    return Factories(minoFactory, minoShifter, minoRotation, colorConverter, perfectValidator)
}

//data class TempMessage(val messageId: String, val body: String)
//
//class ManualInvoker(val factories: Factories) : MessageInvoker {
//    override fun invoke(input: Input): Results? {
//        val states = listOf(
//                Pair(State(SmallField(30), 4), 604800),
//                Pair(State(SmallField(120), 4), 604800),
//                Pair(State(SmallField(480), 4), 604800),
//                Pair(State(SmallField(1074791425), 4), 604800),
//                Pair(State(SmallField(4299165700), 4), 598152),
//                Pair(State(SmallField(17196662800), 4), 568428),
//                Pair(State(SmallField(68786651200), 4), 589600),
//                Pair(State(SmallField(275146604800), 4), 298576),
//                Pair(State(SmallField(15), 4), 604800),
//                Pair(State(SmallField(60), 4), 604800),
//                Pair(State(SmallField(240), 4), 604800),
//                Pair(State(SmallField(960), 4), 604800),
//                Pair(State(SmallField(2149582850), 4), 298576),
//                Pair(State(SmallField(8598331400), 4), 589600),
//                Pair(State(SmallField(34393325600), 4), 568428),
//                Pair(State(SmallField(137573302400), 4), 598152),
//                Pair(State(SmallField(550293209600), 4), 604800)
//        )
//
//        return Results(604800, states.map {
//            Result(it.first, it.second, encodeToFumen(factories, it.first))
//        })
//    }
//
//    override fun shutdown() {
//    }
//}

private fun run(aws: AWS, invoker: MessageInvoker, threshold: Double) {
    while (true) {
        val message = aws.receiveMessage()
//        val message = TempMessage("calculateCount-result", "1,vhAAgWBAUAAAA,,I,1.0")

        if (message == null) {
            println("[skip] no message -> sleep")
            Thread.sleep(TimeUnit.SECONDS.toMillis(5L))
            continue
        }

        println("message-id: ${message.messageId}")

        val stopwatch = Stopwatch.createStartedStopwatch()

        val split = message.body.split(",")
        val input = Input(split[0], split[1], split[2], split[3], split[4])
        println(input)

        search(aws, input, threshold, invoker)

        stopwatch.stop()
        println(stopwatch.toMessage(TimeUnit.SECONDS))
        println(stopwatch.toMessage(TimeUnit.MINUTES))

        aws.deleteMessage(message)
    }
}

private fun search(aws: AWS, input: Input, threshold: Double, invoker: MessageInvoker) {
    if (aws.existsObject(input.prefixPath)) {
        println("[skip] result exists already")
        return
    }

    if (input.prevPercentValue < threshold) {
        println("[skip] under threshold")
        return
    }

    val results = invoker.invoke(input)
    results?.also {
        put(aws, input, it)
        send(aws, input, it, threshold)
    } ?: println("[skip] no invoke")
}

private fun put(aws: AWS, input: Input, results: Results) {
    val (allCount, details) = results

    val output = details.joinToString(";") {
        "${it.data},${it.success}"
    }

    val content = "$allCount?$output"
    aws.putObject(input.prefixPath, content)
}

private fun send(aws: AWS, input: Input, results: Results, threshold: Double) {
    val (allCount, details) = results

    val nextSearchCount = allCount * threshold
    val requests = details.filter { nextSearchCount <= it.success }
    println("send ${requests.size}*7 messages")

    requests.forEach { detail ->
        val entries = Piece.values().map {
            val percent = detail.success.toDouble() / allCount
            val replacedData = detail.data.replace('+', '_').replace('/', '-')
            val batchId = String.format("%s-%s-%s-%s", input.cycle, replacedData, input.allPiece, it.name)
            val body = String.format("%s,%s,%s,%s,%.3f", input.cycle, detail.data, input.allPiece, it.name, percent)
            SendMessageBatchRequestEntry(batchId, body)
        }

        println(entries)

        if (input.allPiece.length <= 1)
            aws.sendLongMessages(entries)
        else
            aws.sendShortMessages(entries)
    }
}
