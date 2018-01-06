package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClient
import main.aws.AWS
import main.aws.Bucket
import main.aws.SQS
import main.domain.createFactories
import percent.Index
import java.nio.file.Paths

fun main(args: Array<String>) {
    val bucketName = System.getenv("BUCKET_NAME")
    println("Bucket Name: $bucketName")

    val receiverQueryName = System.getenv("RECEIVER_QUERY_NAME")
    println("Receiver Query Name: $receiverQueryName")

    val senderQueryName = System.getenv("SENDER_QUERY_NAME")
    println("Sender Query Name: $senderQueryName")

    val threshold = System.getenv("THRESHOLD")?.toDouble() ?: 0.95
    println("Threshold: $threshold")

    val timeoutHour = System.getenv("TIMEOUT")?.toLong()
    println("Timeout/Hour: $timeoutHour")

    val service = System.getenv("SERVICE")?.toBoolean() ?: false
    println("Service: $service")

    val calculate = System.getenv("CALCULATE")?.toBoolean() ?: true
    println("Calculate: $calculate")

    val s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val sqsClient = AmazonSQSClient.builder()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val receiverSQS = SQS(sqsClient, receiverQueryName, timeoutHour)
    val senderSQS = SQS(sqsClient, senderQueryName)
    val bucket = Bucket(s3Client, bucketName)
    val aws = AWS(receiverSQS, senderSQS, bucket)

    val factories = createFactories()

    val index = Index(factories.minoFactory, factories.minoShifter, Paths.get("input/index.csv"))

    try {
        Worker(aws, threshold, service, factories, index, calculate).invoke()
    } finally {
        s3Client.shutdown()
        sqsClient.shutdown()
    }
}