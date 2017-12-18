package main

import common.parser.StringEnumTransform
import common.tetfu.common.ColorConverter
import core.field.Field
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import searcher.common.validator.PerfectValidator

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

data class Input(val cycle: String,
                 val data: String,
                 val headPieces: String,
                 val next: String,
                 val prevPercent: String) {
    val cycleNumber = cycle.toInt()
    val nextPiece = StringEnumTransform.toPiece(next)
    val prevPercentValue = prevPercent.toDouble()
    val prefixPath = "${cycle}/${headPieces}${next}/${data}"
    val allPiece = headPieces + next
}

data class Result(val state: State, val success: Int, val data: String)

data class Results(val allCount: Int, val details: List<Result>)

