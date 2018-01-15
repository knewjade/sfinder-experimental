package main.caller

import main.domain.*

class ContentCaller(private val content: String) : Caller {
    override fun call(): Results {
        if (content.contains('?')) {
            val index = content.indexOf("?")

            assert(0 <= index)

            val allCount = Counter(content.substring(0, index).toInt())
            val substring = content.substring(index + 1)
            val details = substring.takeIf { it.isNotEmpty() }?.let {
                toList(substring)
            } ?: emptyList()

            return Results(allCount, details)
        } else {
            return Results(Counter(content.toInt()), emptyList())
        }
    }

    private fun toList(substring: String): List<Result> {
        assert(substring.isNotEmpty())

        return substring.split(";").map {
            val split = it.split(",")

            assert(split.size == 3)

            val (mino, success, fieldData) = split
            Result(MinoIndex(mino.toInt()), Counter(success.toInt()), FieldData(fieldData))
        }
    }
}