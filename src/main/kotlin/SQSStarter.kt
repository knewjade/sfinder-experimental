import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClient
import core.mino.Piece

fun main(args: Array<String>) {
    val sqs = AmazonSQSClient.builder()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val queueUrl = sqs.getQueueUrl("perfect").queueUrl

    for (cycle in 1..7) {
        Piece.values().forEach {
            val body = String.format("%s,vhAAgWBAUAAAA,,%s,1.0", cycle, it.name)
            println(body)
            sqs.sendMessage(queueUrl, body)
        }
    }

    sqs.shutdown()
}