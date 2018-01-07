package main.invoker

import common.datastore.MinoOperationWithKey
import common.tetfu.Tetfu
import common.tetfu.TetfuElement
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import common.tetfu.field.ColoredField
import core.field.Field
import core.field.FieldFactory
import helper.Patterns
import main.domain.*
import main.percent.SearchingPieces

fun toSearchingPieces(cycle: Cycle, headPieces: HeadPieces): SearchingPieces {
    return SearchingPieces(Patterns.hold(cycle.number), headPieces.allPieceCounter)
}

fun parseToField(minos: List<MinoOperationWithKey>, fieldHeight: Int): State {
    val field = FieldFactory.createField(fieldHeight)
    minos.forEach {
        val minoField = FieldFactory.createField(fieldHeight)
        minoField.put(it.mino, it.x, it.y)
        minoField.insertWhiteLineWithKey(it.needDeletedKey)
        field.merge(minoField)
    }
    val deleteKey = field.clearLineReturnKey()

    return State(field, fieldHeight - java.lang.Long.bitCount(deleteKey))
}

fun encodeToFumen(factories: Factories, state: State): FieldData {
    fun parseGrayField(field: Field): ColoredField {
        val coloredField = ArrayColoredField(24)
        for (y in 0 until field.maxFieldHeight)
            for (x in 0 until 10)
                if (!field.isEmpty(x, y))
                    coloredField.setColorType(ColorType.Gray, x, y)
        return coloredField
    }

    val tetfu = Tetfu(factories.minoFactory, factories.colorConverter)
    val encode = tetfu.encode(listOf(TetfuElement(parseGrayField(state.field), state.maxClearLine.toString())))
    return FieldData(encode)
}