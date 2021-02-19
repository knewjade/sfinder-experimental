import core.field.FieldFactory
import core.field.FieldView

fun main() {
    val field = FieldFactory.createField(24)
    field.setBlock(0, 0)
    println(FieldView.toReducedString(field))
}