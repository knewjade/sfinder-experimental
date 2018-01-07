package main.domain

import common.datastore.MinimalOperationWithKey
import core.mino.Mino
import core.mino.Piece
import core.srs.Rotate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResultPathTest {
    @Test
    fun test1() {
        val cycle = Cycle(1)

        val headMinos = listOf(
                MinimalOperationWithKey(Mino(Piece.I, Rotate.Left), 0, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.L, Rotate.Right), 1, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 3, 0, 0L),
                MinimalOperationWithKey(Mino(Piece.T, Rotate.Left), 5, 1, 0L)
        )
        val headPieces = HeadPieces(headMinos, Piece.T)

        val fieldData = FieldData("9gA8IeB8CeA8DeF8DeF8NeAgH")

        val resultPath = ResultPath(cycle, headPieces, fieldData)
        assertThat(resultPath.path).isEqualTo("1/TILZT/9gA8IeB8CeA8DeF8DeF8NeAgH")
    }
}