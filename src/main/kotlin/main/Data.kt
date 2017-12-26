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
                 val cycle: String,
                 val headPiecesData: String,
                 val next: String,
                 val prevPercent: String) {
    val cycleNumber = cycle.toInt()
    val headPiecesInt = if (headPiecesData.isBlank()) setOf() else headPiecesData.split(";").map { it.toInt() }.toSet()
    val headPiecesMinos = headPiecesInt.map { index.get(it)!! }
    val headPieces = headPiecesMinos.map { it.piece.name }.joinToString("")
    val nextPiece = StringEnumTransform.toPiece(next)
    val prevPercentValue = prevPercent.toDouble()
    val prefixPath = "${cycle}/${headPieces}${next}/${headPiecesData}"
    val allPiece = headPieces + next
}

data class Result(val mino: Int, val success: Int)

data class Results(val allCount: Int, val details: List<Result>)

