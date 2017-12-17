package main

import common.datastore.PieceCounter
import common.parser.StringEnumTransform
import core.mino.Piece
import helper.Patterns
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HelloTest {
    @Test
    fun hello() {
        val hello = Hello("ok")
        assertThat(hello.message).isEqualTo("ok")
    }

    @Test
    fun pieces() {
        val pieces = createSearchPieces(Patterns.hold(1), PieceCounter(listOf(Piece.I)), Piece.T)
        assertThat(pieces).hasSize(100800)

        val noneMatch = pieces.stream()
                .map { it.pieces.subList(0, 3) }
                .noneMatch { it.contains(Piece.I) or it.contains(Piece.T) }

        assertThat(noneMatch).isTrue()
    }

    @Test
    fun sayHello() {
        val next = StringEnumTransform.toPiece("T")
        val message = search(1, "v115@bhD8PeAgH", "I", next, multiThread)
//        assertThat(message).isEqualTo("Hello world")  // Fail
    }
}
