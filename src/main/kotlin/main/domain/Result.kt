package main.domain

data class Result(val mino: MinoIndex, val success: Counter, val fieldData: FieldData) {
    override fun equals(other: Any?): Boolean {
        if (other !is Result)
            return false
        return fieldData == other.fieldData
    }

    override fun hashCode(): Int {
        return fieldData.hashCode()
    }
}

