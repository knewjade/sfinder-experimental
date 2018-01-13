package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClient
import main.aws.AWS
import main.aws.Bucket
import main.aws.SQS
import main.domain.AllMinoIndexes
import main.domain.createFactories
import main.percent.Index
import java.nio.file.Paths

// export BUCKET_NAME=fumen-dev
// export RECEIVER_QUERY_NAME=dev-test
// export SENDER_QUERY_NAME=dev-test

// vhAAgWBAUAAAA,,I
// ???,254;90,J
fun main(args: Array<String>) {
    val receiverQueryName = "perfect"
    val senderQueryName = "perfect"
    val timeoutHour = null

    val bucketName = "fumens"

    val minimumSuccessRate = 0.95
    val service = false
    val calculate = true

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

    val allSolutionsPath = Paths.get("input/indexed_solutions_10x4_SRS.csv")
    val allMinoIndexes = AllMinoIndexes(allSolutionsPath)

    try {
        Worker(aws, minimumSuccessRate, service, factories, index, allMinoIndexes, calculate).invoke()
    } finally {
        s3Client.shutdown()
        sqsClient.shutdown()
    }
}