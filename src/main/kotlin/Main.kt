import main.workSQS

fun main(args: Array<String>) {
    val bucketName = System.getenv("BUCKET_NAME")
    println(bucketName)

    val queryName = System.getenv("QUERY_NAME")
    println(queryName)

    val threshold = System.getenv("THRESHOLD")?.toDouble() ?: 0.95
    println(threshold)

    val multiThread = System.getenv("MULTI_THREAD")?.toBoolean() ?: false
    println(multiThread)

    workSQS(bucketName, queryName, threshold, multiThread)
}