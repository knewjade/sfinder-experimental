package main.aws

import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry

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