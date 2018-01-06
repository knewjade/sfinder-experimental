package main.invoker

import common.datastore.MinimalOperationWithKey
import core.mino.Mino
import core.mino.Piece
import core.srs.Rotate
import main.domain.createFactories
import main.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import percent.Index
import java.nio.file.Paths


class CalculatorMessageInvokerTest {
    @Test
    fun test1() {
        val factories = createFactories()
        val index = Index(factories.minoFactory, factories.minoShifter, Paths.get("input/index.csv"))

        val headMinos = listOf(
                MinimalOperationWithKey(Mino(Piece.I, Rotate.Left), 0, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.L, Rotate.Right), 1, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 3, 0, 0L)
        )
        val headPieces = HeadPieces(headMinos, Piece.S)
        val invoker = CalculatorMessageInvoker(headPieces, factories, index)
        val results = invoker.invoke(Cycle(1))

        assertThat(results.allCount).isEqualTo(Counter(5040))
        assertThat(results.details).hasSize(12)
                .contains(Result(MinoIndex(572), Counter(2790), FieldData("9gA8IeB8HeD8BeB8BeG8MeAgWBAUAAAA")))
                .contains(Result(MinoIndex(578), Counter(1422), FieldData("9gA8IeB8HeD8CeB8AeE8AeB8LeAgWBAUAAAA")))
                .contains(Result(MinoIndex(584), Counter(1966), FieldData("9gA8IeB8HeD8DeG8BeB8KeAgWBAUAAAA")))
    }

    @Test
    fun fill() {
        val factories = createFactories()
        val index = Index(factories.minoFactory, factories.minoShifter, Paths.get("input/index.csv"))

        val headMinos = listOf(
                MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 1, 2, 0L),
                MinimalOperationWithKey(Mino(Piece.T, Rotate.Right), 0, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.S, Rotate.Spawn), 2, 0, 0L),
                MinimalOperationWithKey(Mino(Piece.I, Rotate.Left), 9, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.L, Rotate.Spawn), 7, 0, 0L),
                MinimalOperationWithKey(Mino(Piece.O, Rotate.Spawn), 6, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.J, Rotate.Reverse), 7, 3, 0L),
                MinimalOperationWithKey(Mino(Piece.T, Rotate.Spawn), 4, 0, 0L),
                MinimalOperationWithKey(Mino(Piece.J, Rotate.Reverse), 4, 2, 0L)
        )
        val headPieces = HeadPieces(headMinos, Piece.I)
        val invoker = CalculatorMessageInvoker(headPieces, factories, index)
        val results = invoker.invoke(Cycle(1))

        assertThat(results.allCount).isEqualTo(Counter(4))
        assertThat(results.details).hasSize(1)
                .contains(Result(MinoIndex(178), Counter(4), FieldData("vhAAgWBAQAAAA")))
    }

    @Test
    fun empty() {
        val factories = createFactories()
        val index = Index(factories.minoFactory, factories.minoShifter, Paths.get("input/index.csv"))

        val headMinos = listOf(
                MinimalOperationWithKey(Mino(Piece.I, Rotate.Left), 0, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.L, Rotate.Right), 1, 1, 0L),
                MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 3, 0, 0L),
                MinimalOperationWithKey(Mino(Piece.T, Rotate.Left), 5, 1, 0L)
        )
        val headPieces = HeadPieces(headMinos, Piece.T)
        val invoker = CalculatorMessageInvoker(headPieces, factories, index)
        val results = invoker.invoke(Cycle(1))

        assertThat(results.allCount).isEqualTo(Counter(0))
        assertThat(results.details).isEmpty()
    }
}