import main.workSQS

fun main(args: Array<String>) {
    val bucketName = System.getenv("BUCKET_NAME")
    println("Bucket Name: ${bucketName}")

    val receiverQueryName = System.getenv("RECEIVER_QUERY_NAME")
    println("Receiver Query Name: ${receiverQueryName}")

    val shortSenderQueryName = System.getenv("SHORT_SENDER_QUERY_NAME")
    println("Short Sender Query Name: ${shortSenderQueryName}")

    val longSenderQueryName = System.getenv("LONG_SENDER_QUERY_NAME")
    println("Long Sender Query Name: ${longSenderQueryName}")

    val threshold = System.getenv("THRESHOLD")?.toDouble() ?: 0.95
    println("Threshold: ${threshold}")

    val multiThread = System.getenv("MULTI_THREAD")?.toBoolean() ?: false
    println("Multi Thread: ${multiThread}")

    workSQS(bucketName, receiverQueryName, shortSenderQueryName, longSenderQueryName, threshold)
}