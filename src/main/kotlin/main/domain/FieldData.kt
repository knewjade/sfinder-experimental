package main.domain

class FieldData(data: String) {
    val raw = data.replace("_", "/").replace("?", "")
    val representation = raw.replace("/", "_")
    val messageId = raw.replace("+", "-")
            .replace("/", "_")
            .replace("?", "")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FieldData
        return raw == other.raw
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }

    override fun toString(): String {
        return "FieldData(raw='$raw')"
    }
}