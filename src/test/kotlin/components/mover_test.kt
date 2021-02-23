package components

import core.field.FieldFactory
import core.mino.Piece
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MoverTest {
    @Test
    fun moveWithoutClearNoHold() {
        val height = 12
        val moverThreadLocal = LockedCandidateMoverThreadLocal.create(height)
        val mover = moverThreadLocal.get()

        val field = FieldFactory.createField(
            "" +
                    "XXXXXXXX__" +
                    "XXXXXXXX__" +
                    "XXXXXXXX__"
        )
        val moves = mover.moveWithoutClearNoHold(field, Piece.O, null)
        assertThat(moves).hasSize(8)
    }
}