package main

import common.parser.StringEnumTransform
import common.tetfu.common.ColorConverter
import core.field.Field
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import percent.Index
import searcher.common.validator.PerfectValidator

fun createFactories(): Factories {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()
    val colorConverter = ColorConverter()
    val perfectValidator = PerfectValidator()
    return Factories(minoFactory, minoShifter, minoRotation, colorConverter, perfectValidator)
}

data class Factories(
        val minoFactory: MinoFactory,
        val minoShifter: MinoShifter,
        val minoRotation: MinoRotation,
        val colorConverter: ColorConverter,
        val perfectValidator: PerfectValidator
)

data class State(val field: Field, val maxClearLine: Int) {
    override fun hashCode(): Int {
        return field.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return field.equals(other)
    }
}

data class Input(val index: Index,
                 val headFieldData: String,
                 val headPiecesData: String,
                 val next: String) {
    val headPiecesInt = if (headPiecesData.isBlank()) setOf() else headPiecesData.split("_").map { it.toInt() }.toSet()
    val headPiecesMinos = headPiecesInt.map { index.get(it)!! }
    val nextPiece = StringEnumTransform.toPiece(next)!!
    private val headPieceString = (headPiecesMinos.map { it.piece } + nextPiece).sorted().joinToString("")
    val prefixPath = "$headPieceString/$headFieldData"
}

data class Result(val mino: Int, val success: Int, val fieldData: String) {
    override fun equals(other: Any?): Boolean {
        if (other !is Result)
            return false
        return fieldData == other.fieldData
    }

    override fun hashCode(): Int {
        return fieldData.hashCode()
    }
}

data class Results(val allCount: Int, val details: List<Result>)

