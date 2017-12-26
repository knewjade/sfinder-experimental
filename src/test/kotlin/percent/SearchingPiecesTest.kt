package percent

import common.datastore.PieceCounter
import core.mino.Piece
import helper.Patterns
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SearchingPiecesTest {
    @Test
    fun create1atCycle1() {
        val head = SearchingPieces(Patterns.hold(1), PieceCounter(listOf(Piece.T)))
        assertThat(head.allCount).isEqualTo(604800)
        assertThat(head.piecesMap.keys.first().pieces).hasSize(9)
        assertThat(head.piecesMap.values.first().first().pieces).hasSize(10)
    }

    @Test
    fun create2atCycle1() {
        val head = SearchingPieces(Patterns.hold(1), PieceCounter(listOf(Piece.I, Piece.J)))
        assertThat(head.allCount).isEqualTo(100800)
        assertThat(head.piecesMap.keys.first().pieces).hasSize(8)
        assertThat(head.piecesMap.values.first().first().pieces).hasSize(9)
    }

    @Test
    fun create3atCycle1() {
        val head = SearchingPieces(Patterns.hold(1), PieceCounter(listOf(Piece.S, Piece.Z, Piece.O)))
        assertThat(head.allCount).isEqualTo(20160)
        assertThat(head.piecesMap.keys.first().pieces).hasSize(7)
        assertThat(head.piecesMap.values.first().first().pieces).hasSize(8)
    }

    @Test
    fun create4atCycle1() {
        val head = SearchingPieces(Patterns.hold(1), PieceCounter(listOf(Piece.L, Piece.L)))
        assertThat(head.allCount).isEqualTo(0)
    }

    @Test
    fun create1atCycle2() {
        val head = SearchingPieces(Patterns.hold(2), PieceCounter(listOf(Piece.I, Piece.J)))
        assertThat(head.allCount).isEqualTo(226800)
        assertThat(head.piecesMap.keys.first().pieces).hasSize(8)
        assertThat(head.piecesMap.values.first().first().pieces).hasSize(9)
    }
}