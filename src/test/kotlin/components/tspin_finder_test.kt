package components

import TSpinFinderThreadLocal
import core.field.FieldFactory
import core.mino.Piece
import org.junit.jupiter.api.Test

class TSpinFinderTest {
    @Test
    fun main() {
        val height = 12
        val finderThreadLocal = TSpinFinderThreadLocal.create(3, height)
        val finder = finderThreadLocal.get()
        val initField = FieldFactory.createField(
            "__________" +
                    "__________" +
                    "__________" +
                    "___X______" +
                    "___XX____X" +
                    "_XXXXXXX_X" +
                    "XXXXXXXXXX" +
                    "_XXXX___XX" +
                    "", height
        )
        initField.clearLine()
        val results = finder.searchWithoutHold(initField, listOf(Piece.L, Piece.Z, Piece.S, Piece.J, Piece.O, Piece.I))
        println(results)
    }
}