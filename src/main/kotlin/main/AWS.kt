package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class SQSMessage(private val sqs: SQS, message: Message, val timeoutInSec: Int) {
    val receiptHandle = message.receiptHandle!!
    val messageId: String = message.messageId
    val body = message.body!!
    private val startTime = System.nanoTime()

    init {
        changeMessageVisibility(timeoutInSec)
    }

    private fun changeMessageVisibility(timeoutInSec: Int) {
        sqs.changeMessageVisibility(this, timeoutInSec)
    }

    fun progressTimer(): Double {
        val progress = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
        return progress / timeoutInSec.toDouble()
    }

    fun delete() {
        sqs.deleteMessage(this)
    }

    fun duplicate() {
        sqs.sendMessage(body)
    }
}

class SQS(private val client: AmazonSQS, queryName: String) {
    private val queueUrl = client.getQueueUrl(queryName).queueUrl

    fun receiveMessage(): SQSMessage? {
        val request = ReceiveMessageRequest(queueUrl)
        val receive = client.receiveMessage(request)

        val messages = receive.messages

        if (messages.size != 1)
            return null

        val timeout = TimeUnit.SECONDS.convert(6L, TimeUnit.SECONDS).toInt()
        return SQSMessage(this, messages[0], timeout)
    }

    fun sendMessage(body: String) {
        client.sendMessage(queueUrl, body)
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

    fun deleteMessage(message: SQSMessage) {
        client.deleteMessage(queueUrl, message.receiptHandle)
    }

    fun changeMessageVisibility(message: SQSMessage, timeout: Int) {
        client.changeMessageVisibility(queueUrl, message.receiptHandle, timeout)
    }
}

class Bucket(private val client: AmazonS3, private val bucketName: String) {
    fun putObject(path: String, content: String) {
        client.putObject(bucketName, path, content)
    }

    fun existsObject(path: String): Boolean {
        return client.doesObjectExist(bucketName, path)
    }

    fun getObject(path: String): String? {
        return client.getObject(bucketName, path)?.objectContent?.let { convert(it) }
    }

    @Throws(IOException::class)
    private fun convert(inputStream: InputStream): String {
        val reader = InputStreamReader(inputStream)
        return StringBuilder().run {
            val buffer = CharArray(512)

            while (true) {
                reader.read(buffer).takeIf { 0 <= it }
                        ?.let { append(buffer, 0, it) }
                        ?: break
            }

            toString()
        }
    }
}

class AWS(private val receiver: SQS, private val sender: SQS, private val s3: Bucket) {
    fun receiveMessage(): SQSMessage? {
        return receiver.receiveMessage()
    }

    fun sendShortMessages(entries: List<SendMessageBatchRequestEntry>) {
        return sender.sendMessages(entries)
    }

    fun putObject(path: String, content: String) {
        s3.putObject(path, content)
    }

    fun existsObject(path: String): Boolean {
        return s3.existsObject(path)
    }

    fun getObject(path: String): String? {
        return s3.getObject(path)
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