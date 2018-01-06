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

// export BUCKET_NAME=fumen-dev
// export RECEIVER_QUERY_NAME=dev-test
// export SENDER_QUERY_NAME=dev-test

// vhAAgWBAUAAAA,,I
// ???,254;90,J
fun main(args: Array<String>) {
    val receiverQueryName = "perfect-1"
    val senderQueryName = "perfect-1"
    val timeoutHour = null

    val bucketName = "fumen"

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

    val minimumSuccessRate = 0.95
    val service = false
    val calculate = false
    try {
        Worker(aws, minimumSuccessRate, service, factories, index, calculate).invoke()
    } finally {
        s3Client.shutdown()
        sqsClient.shutdown()
    }
}