import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClient
import core.mino.Piece

fun main(args: Array<String>) {
    val sqs = AmazonSQSClient.builder()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val queueUrl = sqs.getQueueUrl("perfect").queueUrl
//    val queueUrl = sqs.getQueueUrl("dev-test").queueUrl

    (1..8).forEach { cycle ->
        Piece.values().forEach {
            val body = String.format("%d,vhAAgWBAUAAAA,,%s", cycle, it.name)
            println(body)
            sqs.sendMessage(queueUrl, body)
        }
    }

    sqs.shutdown()
}