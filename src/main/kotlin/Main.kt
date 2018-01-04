import main.Worker

fun main(args: Array<String>) {
    val bucketName = System.getenv("BUCKET_NAME")
    println("Bucket Name: $bucketName")

    val receiverQueryName = System.getenv("RECEIVER_QUERY_NAME")
    println("Receiver Query Name: $receiverQueryName")

    val senderQueryName = System.getenv("SENDER_QUERY_NAME")
    println("Sender Query Name: $senderQueryName")

    val threshold = System.getenv("THRESHOLD")?.toDouble() ?: 0.95
    println("Threshold: $threshold")

    val timeoutHour = System.getenv("TIMEOUT")?.toLong() ?: 12L
    println("Timeout/Hour: $timeoutHour")

    val service = System.getenv("SERVICE")?.toBoolean() ?: false
    println("Service: $service")

    val calculate = System.getenv("CALCULATE")?.toBoolean() ?: true
    println("Calculate: $calculate")

    Worker(bucketName, receiverQueryName, senderQueryName, threshold, timeoutHour, service, calculate).work()
}