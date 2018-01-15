package main.domain

data class ResultPath(val path: String) {
    constructor(cycle: Cycle, headPieces: HeadPieces, fieldData: FieldData) : this(
            "${cycle.number}/${headPieces.representation}/${fieldData.representation}"
    )
}