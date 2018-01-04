import main.Worker

// export BUCKET_NAME=fumen-dev
// export RECEIVER_QUERY_NAME=dev-test
// export SENDER_QUERY_NAME=dev-test

// vhAAgWBAUAAAA,,I
// ???,254;90,J
fun main(args: Array<String>) {
    Worker("fumen",
            "perfect",
            "perfect",
            0.95,
            1L,
            false,
            true).work()
}