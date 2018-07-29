import core.field.FieldFactory
import core.field.FieldView
import core.mino.Mino
import core.mino.Piece
import core.srs.Rotate

fun main(args: Array<String>) {
    val field = FieldFactory.createField(4)
    field.put(Mino(Piece.I, Rotate.Spawn), 4, 0)
    println(FieldView.toString(field))
}
