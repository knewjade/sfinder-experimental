package main.domain

import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import core.mino.Mino
import core.mino.Piece
import core.srs.Rotate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HeadPiecesTest {
    @Test
    fun test1() {
        val headMinos = listOf(
                MinimalOperationWithKey(Mino(Piece.I, Rotate.Left), 0, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.L, Rotate.Right), 1, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 3, 0, 0L),
                MinimalOperationWithKey(Mino(Piece.T, Rotate.Left), 5, 1, 0L)
        )
        val headPieces = HeadPieces(headMinos, Piece.T)
        assertThat(headPieces.representation).isEqualTo("TILZT")
        assertThat(headPieces.allPieceCounter).isEqualTo(PieceCounter(listOf(Piece.I, Piece.L, Piece.Z, Piece.T, Piece.T)))
    }
}