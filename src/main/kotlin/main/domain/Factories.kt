package main.domain

import common.tetfu.common.ColorConverter
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
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