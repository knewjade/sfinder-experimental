package components

import common.datastore.FullOperationWithKey
import common.datastore.PieceCounter
import core.action.reachable.LockedReachable
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.Rotate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExpanderTest {
    @Test
    fun move() {
        val minoFactory = MinoFactory()
        val minoShifter = MinoShifter()
        val height = 12
        val mover = MoverForExpand(minoFactory, minoShifter, height)

        val field = FieldFactory.createField(
            "" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "X_________"
        )
        val moves = mover.move(field, Piece.L, field.filledLine)
        assertThat(moves).hasSize(68)
    }

    @Test
    fun move2() {
        val minoFactory = MinoFactory()
        val minoShifter = MinoShifter()
        val height = 5
        val mover = MoverForExpand(minoFactory, minoShifter, height)

        val field = FieldFactory.createField(
            "" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "X_________"
        )
        val moves = mover.move(field, Piece.T, field.filledLine)
        assertThat(moves).hasSize(33)
    }

    @Test
    fun moves() {
        val minoFactory = MinoFactory()
        val minoRotation = MinoRotation.create()
        val minoShifter = MinoShifter()
        val height = 12
        val lockedReachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)
        val expander = Expander(minoFactory, minoShifter, minoRotation, lockedReachable, height)
        val operations = MinoOperationWithKeysList(
            listOf(
                FullOperationWithKey.create(minoFactory.create(Piece.O, Rotate.Spawn), 0, 0, 0L, height),
                FullOperationWithKey.create(minoFactory.create(Piece.Z, Rotate.Left), 4, 1, 0L, height),
                FullOperationWithKey.create(minoFactory.create(Piece.L, Rotate.Reverse), 6, 1, 0L, height),
                FullOperationWithKey.create(minoFactory.create(Piece.S, Rotate.Right), 8, 1, 0L, height),
                FullOperationWithKey.create(minoFactory.create(Piece.J, Rotate.Reverse), 4, 3, 0L, height),
                FullOperationWithKey.create(minoFactory.create(Piece.T, Rotate.Reverse), 2, 2, 0L, height),
            )
        )
        val moves = expander.moveWithTSpin(
            FieldFactory.createField(height), operations, PieceCounter.getSinglePieceCounter(Piece.I)
        )
        assertThat(moves).hasSize(19)

        val initField = FieldFactory.createField(height)
        for (move in moves) {
            println(FieldView.toReducedString(move.field(initField, height)))
            println()
        }
    }
}