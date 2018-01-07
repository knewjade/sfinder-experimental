package main.percent

import main.domain.Results

class ResultsSerializer {
    fun str(results: Results): String {
        val (allCount, details) = results

        var content = "${allCount.value}"
        details.takeIf { it.isNotEmpty() }?.let {
            content += "?" + details.joinToString(";") {
                "${it.mino.index},${it.success.value},${it.fieldData.representation}"
            }
        }

        return content
    }
}