package main.aws

import com.amazonaws.services.sqs.model.Message
import java.util.concurrent.TimeUnit

class SQSMessage(private val sqs: SQS, message: Message, private val timeoutInSec: Int?) {
    val receiptHandle = message.receiptHandle!!
    val messageId: String = message.messageId
    val body = message.body!!
    private val startTime = System.nanoTime()

    init {
        timeoutInSec?.let { changeMessageVisibility(it) }
    }

    private fun changeMessageVisibility(timeoutInSec: Int) {
        sqs.changeMessageVisibility(this, timeoutInSec)
    }

    fun progressTimer(): Double? {
        return timeoutInSec?.let {
            val progress = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            progress / it.toDouble()
        }
    }

    fun delete() {
        sqs.deleteMessage(this)
    }

    fun duplicate() {
        sqs.sendMessage(body)
    }
}