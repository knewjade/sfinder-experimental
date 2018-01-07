package main.percent

import common.datastore.MinimalOperationWithKey
import core.mino.Mino
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.Rotate
import main.domain.AllMinoIndexes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class SolutionLoaderTest {
    @Nested
    class DirectSolutionLoader {
        companion object {
            private val index by lazy {
                val path = Paths.get("input/index.csv")
                Index(MinoFactory(), MinoShifter(), path)
            }
        }

        @Test
        fun test1() {
            val minos = listOf(
                    MinimalOperationWithKey(Mino(Piece.J, Rotate.Spawn), 1, 0, 0L),
                    MinimalOperationWithKey(Mino(Piece.S, Rotate.Right), 0, 2, 0L)
            )
            val requires = minos.map { index.get(it)!! }.toSet()
            val loader = DirectSolutionLoader(Paths.get("input/indexed_solutions_10x4_SRS.csv"), index, requires)

            val fix = listOf(
                    MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 3, 0, 0L)
            )
            assertThat(loader.load(fix).size).isEqualTo(962)
        }

        @Test
        fun test2() {
            val minos = listOf(
                    MinimalOperationWithKey(Mino(Piece.J, Rotate.Spawn), 1, 0, 0L),
                    MinimalOperationWithKey(Mino(Piece.S, Rotate.Right), 0, 2, 0L)
            )
            val requires = minos.map { index.get(it)!! }.toSet()
            val loader = DirectSolutionLoader(Paths.get("input/indexed_solutions_10x4_SRS.csv"), index, requires)

            val fix = listOf(
                    MinimalOperationWithKey(Mino(Piece.I, Rotate.Left), 9, 1, 0L)
            )
            assertThat(loader.load(fix).size).isEqualTo(1115)
        }

        @Test
        fun test3() {
            val minos = listOf(
                    MinimalOperationWithKey(Mino(Piece.I, Rotate.Spawn), 1, 0, 0L),
                    MinimalOperationWithKey(Mino(Piece.J, Rotate.Spawn), 1, 1, 0L)
            )
            val requires = minos.map { index.get(it)!! }.toSet()
            val loader = DirectSolutionLoader(Paths.get("input/indexed_solutions_10x4_SRS.csv"), index, requires)

            val fix = listOf(
                    MinimalOperationWithKey(Mino(Piece.T, Rotate.Reverse), 1, 3, 0L)
            )
            assertThat(loader.load(fix).size).isEqualTo(1185)
        }
    }

    @Nested
    class CachedSolutionLoader {
        companion object {
            private val index by lazy {
                val path = Paths.get("input/index.csv")
                Index(MinoFactory(), MinoShifter(), path)
            }

            private val allMinoIndexes by lazy {
                val path = Paths.get("input/indexed_solutions_10x4_SRS.csv")
                AllMinoIndexes(path)
            }
        }

        @Test
        fun test1() {
            val minos = listOf(
                    MinimalOperationWithKey(Mino(Piece.J, Rotate.Spawn), 1, 0, 0L),
                    MinimalOperationWithKey(Mino(Piece.S, Rotate.Right), 0, 2, 0L)
            )
            val requires = minos.map { index.get(it)!! }.toSet()
            val loader = CachedSolutionLoader(allMinoIndexes, index, requires)

            val fix = listOf(
                    MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 3, 0, 0L)
            )
            assertThat(loader.load(fix).size).isEqualTo(962)
        }

        @Test
        fun test2() {
            val minos = listOf(
                    MinimalOperationWithKey(Mino(Piece.J, Rotate.Spawn), 1, 0, 0L),
                    MinimalOperationWithKey(Mino(Piece.S, Rotate.Right), 0, 2, 0L)
            )
            val requires = minos.map { index.get(it)!! }.toSet()
            val loader = CachedSolutionLoader(allMinoIndexes, index, requires)

            val fix = listOf(
                    MinimalOperationWithKey(Mino(Piece.I, Rotate.Left), 9, 1, 0L)
            )
            assertThat(loader.load(fix).size).isEqualTo(1115)
        }

        @Test
        fun test3() {
            val minos = listOf(
                    MinimalOperationWithKey(Mino(Piece.I, Rotate.Spawn), 1, 0, 0L),
                    MinimalOperationWithKey(Mino(Piece.J, Rotate.Spawn), 1, 1, 0L)
            )
            val requires = minos.map { index.get(it)!! }.toSet()
            val loader = CachedSolutionLoader(allMinoIndexes, index, requires)

            val fix = listOf(
                    MinimalOperationWithKey(Mino(Piece.T, Rotate.Reverse), 1, 3, 0L)
            )
            assertThat(loader.load(fix).size).isEqualTo(1185)
        }
    }
}