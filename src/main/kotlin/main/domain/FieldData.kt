package main.domain

data class FieldData(private val data: String) {
    val raw = data.replace("_", "/").replace("?", "")
    val representation = raw.replace("/", "_")
    val messageId = raw.replace("+", "-")
            .replace("/", "_")
            .replace("?", "")
}