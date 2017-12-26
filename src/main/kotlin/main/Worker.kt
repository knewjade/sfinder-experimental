package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import core.mino.Piece
import lib.Stopwatch
import percent.Index
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun workSQS(
        bucketName: String,
        receiverQueryName: String,
        shortSenderQueryName: String,
        longSenderQueryName: String,
        minimumSuccessRate: Double
) {
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
    val path = Paths.get(ClassLoader.getSystemResource("index.csv").toURI())

    val index = Index(factories.minoFactory, factories.minoShifter, path)
    val invoker = LoadBaseMessageInvoker(factories, index, minimumSuccessRate)

    try {
        run(aws, invoker, minimumSuccessRate, index)
    } finally {
        s3Client.shutdown()
        sqsClient.shutdown()
        invoker.shutdown()
    }
}

private fun run(aws: AWS, invoker: MessageInvoker, threshold: Double, index: Index) {
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
        val input = Input(index, split[0], split[1], split[2], split[3])
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
        "${it.mino},${it.success}"
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
            val numbers = (input.headPiecesInt + detail.mino).joinToString("_")
            val batchId = String.format("%s-%s-%s", input.cycle, numbers, it.name)
            val body = String.format("%s,%s,%s,%.5f", input.cycle, numbers, it.name, percent)
            SendMessageBatchRequestEntry(batchId, body)
        }

        println(entries)

        if (input.allPiece.length <= 1)
            aws.sendLongMessages(entries)
        else
            aws.sendShortMessages(entries)
    }
}
