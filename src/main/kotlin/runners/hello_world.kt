import common.tetfu.Tetfu
import common.tetfu.common.ColorConverter
import common.tetfu.field.ColoredFieldView
import core.mino.MinoFactory

fun main() {
    println("Hello, world")

    val tetfu = Tetfu(MinoFactory(), ColorConverter())
    val pages = tetfu.decode("9gBtDewhilwwBtCewhglRpxwR4Bewhg0RpwwR4Cewh?i0JeAgH")
    println(ColoredFieldView.toString(pages.first().field))
}