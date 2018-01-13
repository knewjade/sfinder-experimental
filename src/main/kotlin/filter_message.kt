import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClient
import main.aws.SQS

fun main(args: Array<String>) {
    val sqsClient = AmazonSQSClient.builder()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val receiverQueryName = "perfect"
    val receiverSQS = SQS(sqsClient, receiverQueryName)
    val senderSQS1 = SQS(sqsClient, "perfect-4")
    val senderSQS2 = SQS(sqsClient, "perfect-5")

    while (true) {
        val message = receiverSQS.receiveMessage() ?: return

        val split = message.body.trim().split(",")
        val numberSize = split[2].takeIf { it.isNotBlank() }?.split("_")!!.size

        if (numberSize == 3) {
            senderSQS1.sendMessage(message.body)
        } else {
            senderSQS2.sendMessage(message.body)
        }

        message.delete()
    }
}