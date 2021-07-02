package functions

import common.datastore.BlockField
import common.tetfu.TetfuElement
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ColoredField
import common.tetfu.field.ColoredFieldFactory
import core.field.Field
import core.mino.Piece
import core.srs.Rotate

fun blockFieldToColoredField(
    initField: Field, colorConverter: ColorConverter, blockField: BlockField,
): ColoredField {
    val coloredField = ColoredFieldFactory.createGrayField(initField)
    for (piece in Piece.values()) {
        val target = blockField[piece]
        val colorType = colorConverter.parseToColorType(piece)
        fillInField(coloredField, colorType, target)
    }
    return coloredField
}

fun blockFieldToTetfuElement(
    initField: Field, colorConverter: ColorConverter, blockField: BlockField, comment: String
): TetfuElement {
    val coloredField = blockFieldToColoredField(initField, colorConverter, blockField)
    return TetfuElement(coloredField, ColorType.Empty, Rotate.Reverse, 0, 0, comment)
}

private fun fillInField(coloredField: ColoredField, colorType: ColorType, target: Field) {
    for (y in 0 until target.maxFieldHeight) {
        for (x in 0..9) {
            if (!target.isEmpty(x, y)) coloredField.setColorType(colorType, x, y)
        }
    }
}
