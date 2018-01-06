package main.aws

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import java.util.concurrent.TimeUnit

class SQS(private val client: AmazonSQS, queryName: String, val timeoutInHour: Long? = null) {
    private val queueUrl = client.getQueueUrl(queryName).queueUrl

    fun receiveMessage(): SQSMessage? {
        val request = ReceiveMessageRequest(queueUrl)
        val receive = client.receiveMessage(request)

        val messages = receive.messages

        if (messages.size != 1)
            return null

        val timeout = timeoutInHour?.let { TimeUnit.SECONDS.convert(timeoutInHour, TimeUnit.HOURS).toInt() - 1 }
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