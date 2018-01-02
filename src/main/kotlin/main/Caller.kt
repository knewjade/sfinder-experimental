package main

class Caller(
        private val aws: AWS,
        private val input: Input,
        private val invoker: MessageInvoker
) {
    fun search(cycle: Int): Results {
        val path = path(cycle)
        return if (aws.existsObject(path)) {
            println("result exists already: Loading from $path")
            val content = aws.getObject(path)!!
            content.indexOf("?").takeIf { 0 <= it }?.let {
                val allCount = content.substring(0, it).toInt()
                val details = content.substring(it + 1).split(";")
                        .map {
                            val split = it.split(",")
                            assert(split.size == 2)
                            val (mino, success, fieldData) = split
                            Result(mino.toInt(), success.toInt(), fieldData)
                        }
                Results(allCount, details)
            } ?: Results(0, emptyList())
        } else {
            val results = invoker.invoke(cycle)
            put(aws, results, path)
            results
        }
    }

    private fun path(cycle: Int): String {
        return "$cycle/" + input.prefixPath
    }

    private fun put(aws: AWS, results: Results, path: String) {
        val (allCount, details) = results

        if (0 < allCount) {
            val output = details.joinToString(";") {
                "${it.mino},${it.success},${it.fieldData}"
            }
            val content = "$allCount?$output"
            aws.putObject(path, content)
        } else {
            aws.putObject(path, "0")
        }
    }
}