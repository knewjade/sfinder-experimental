package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry

class SQS(val client: AmazonSQS, queryName: String) {
    val queueUrl = client.getQueueUrl(queryName).queueUrl

    fun receiveMessage(): Message? {
        val request = ReceiveMessageRequest(queueUrl)
        val receive = client.receiveMessage(request)

        val messages = receive.messages

        if (messages.size != 1)
            return null

        return messages[0]
    }

    fun sendMessages(entries: List<SendMessageBatchRequestEntry>) {
        var index = 0
        while (index < entries.size) {
            val batch = mutableListOf<SendMessageBatchRequestEntry>()
            for (count in 0 until 10) {
                try {
                    entries[index]
                } catch (e: Exception) {
                    null
                }?.let { batch.add(it) }
                index += 1
            }
            client.sendMessageBatch(queueUrl, batch)
        }
    }

    fun deleteMessage(message: Message) {
        client.deleteMessage(queueUrl, message.receiptHandle)
    }
}

class Bucket(val client: AmazonS3, val bucketName: String) {
    fun putObject(path: String, content: String) {
        client.putObject(bucketName, path, content)
    }

    fun existsObject(path: String): Boolean {
        return client.doesObjectExist(bucketName, path)
    }
}

class AWS(val receiver: SQS, val shortSender: SQS, val longSender: SQS, val s3: Bucket) {
    fun receiveMessage(): Message? {
        return receiver.receiveMessage()
    }

    fun sendShortMessages(entries: List<SendMessageBatchRequestEntry>) {
        return shortSender.sendMessages(entries)
    }

    fun sendLongMessages(entries: List<SendMessageBatchRequestEntry>) {
        return longSender.sendMessages(entries)
    }

    fun deleteMessage(message: Message) {
        receiver.deleteMessage(message)
    }

    fun putObject(path: String, content: String) {
        s3.putObject(path, content)
    }

    fun existsObject(path: String): Boolean {
        return s3.existsObject(path)
    }
}

fun main(args: Array<String>) {
    val sqsClient = AmazonSQSClient.builder()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val sqs = SQS(sqsClient, "dev-test")
    val message = sqs.receiveMessage()
    println(message)

    val entries = (0..25).map { SendMessageBatchRequestEntry(it.toString(), it.toString()) }
    sqs.sendMessages(entries)
}