package percent

import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import core.action.reachable.LockedReachable
import core.action.reachable.Reachable
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.Rotate
import helper.Patterns
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class SuccessTest {
    @Nested
    class HeadL {
        companion object {
            private val height = 4
            private val minoFactory = MinoFactory()
            private val minoShifter = MinoShifter()
            private val successCalculator = createSuccess()
            private val reachable = createReachable()

            private fun createSuccess(): Success {
                val usingPieces = listOf(Piece.L)

                val path = Paths.get("output/index.csv")
                val index = Index(minoFactory, minoShifter, path)

                val allSolutionsPath = Paths.get("output/indexed_solutions_10x4_SRS.csv")
                val solutionLoader = SolutionLoader(allSolutionsPath, index, setOf())

                val searchingPieces = SearchingPieces(Patterns.hold(1), PieceCounter(usingPieces))

                return Success(solutionLoader, index, searchingPieces, height)
            }

            private fun createReachable(): Reachable {
                val minoShifter = MinoShifter()
                val minoRotation = MinoRotation()
                return LockedReachable(minoFactory, minoShifter, minoRotation, height)
            }
        }

        @Test
        fun testRight1() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Right), 0, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(604800)
        }

        @Test
        fun testRight2() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Right), 2, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(603416)
        }

        @Test
        fun testRight3() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Right), 6, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(567980)
        }

        @Test
        fun testLeft1() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Left), 9, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(604800)
        }

        @Test
        fun testLeft2() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Left), 8, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(580374)
        }

        @Test
        fun testSpawn1() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Spawn), 8, 0, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(604800)
        }

        @Test
        fun testSpawn2() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Spawn), 2, 0, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(599024)
        }

        @Test
        fun testReverse1() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Reverse), 8, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(603824)
        }

        @Test
        fun testReverse2() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Reverse), 1, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(604800)
        }
    }

    @Nested
    class HeadI {
        companion object {
            private val height = 4
            private val minoFactory = MinoFactory()
            private val minoShifter = MinoShifter()
            private val successCalculator = createSuccess()
            private val reachable = createReachable()

            private fun createSuccess(): Success {
                val usingPieces = listOf(Piece.I)

                val path = Paths.get("output/index.csv")
                val index = Index(minoFactory, minoShifter, path)

                val allSolutionsPath = Paths.get("output/indexed_solutions_10x4_SRS.csv")
                val solutionLoader = SolutionLoader(allSolutionsPath, index, setOf())

                val searchingPieces = SearchingPieces(Patterns.hold(1), PieceCounter(usingPieces))

                return Success(solutionLoader, index, searchingPieces, height)
            }

            private fun createReachable(): Reachable {
                val minoShifter = MinoShifter()
                val minoRotation = MinoRotation()
                return LockedReachable(minoFactory, minoShifter, minoRotation, height)
            }
        }

        @Test
        fun testRight1() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.I, Rotate.Right), 1, 2, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(298576)
        }

        @Test
        fun testLeft1() {
            val minos = listOf(
                    MinimalOperationWithKey(minoFactory.create(Piece.I, Rotate.Left), 7, 1, 0L)
            )

            val success = successCalculator.success(minos, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(598152)
        }
    }

    @Nested
    class HeadLT {
        companion object {
            private val height = 4
            private val minoFactory = MinoFactory()
            private val minoShifter = MinoShifter()
            private val successCalculator = createSuccess()
            private val reachable = createReachable()

            private fun createSuccess(): Success {
                val usingPieces = listOf(Piece.L, Piece.T)

                val path = Paths.get("output/index.csv")
                val index = Index(minoFactory, minoShifter, path)

                val allSolutionsPath = Paths.get("output/indexed_solutions_10x4_SRS.csv")
                val solutionLoader = SolutionLoader(allSolutionsPath, index, setOf(254))

                val searchingPieces = SearchingPieces(Patterns.hold(1), PieceCounter(usingPieces))

                return Success(solutionLoader, index, searchingPieces, height)
            }

            private fun createReachable(): Reachable {
                val minoShifter = MinoShifter()
                val minoRotation = MinoRotation()
                return LockedReachable(minoFactory, minoShifter, minoRotation, height)
            }
        }

        @Test
        fun testRight1() {
            val mino = MinimalOperationWithKey(minoFactory.create(Piece.T, Rotate.Reverse), 2, 1, 0L)

            val success = successCalculator.success(mino, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(-1)  // 85122?
        }
    }

    @Nested
    class HeadLTZ {
        companion object {
            private val height = 4
            private val minoFactory = MinoFactory()
            private val minoShifter = MinoShifter()
            private val successCalculator = createSuccess()
            private val reachable = createReachable()

            private fun createSuccess(): Success {
                val usingPieces = listOf(Piece.L, Piece.T, Piece.Z)

                val path = Paths.get("output/index.csv")
                val index = Index(minoFactory, minoShifter, path)

                val allSolutionsPath = Paths.get("output/indexed_solutions_10x4_SRS.csv")
                val solutionLoader = SolutionLoader(allSolutionsPath, index, setOf(254, 90))

                val searchingPieces = SearchingPieces(Patterns.hold(1), PieceCounter(usingPieces))

                return Success(solutionLoader, index, searchingPieces, height)
            }

            private fun createReachable(): Reachable {
                val minoShifter = MinoShifter()
                val minoRotation = MinoRotation()
                return LockedReachable(minoFactory, minoShifter, minoRotation, height)
            }
        }

        @Test
        fun testSpawn1() {
            val mino = MinimalOperationWithKey(minoFactory.create(Piece.Z, Rotate.Spawn), 1, 2, 0L)

            val success = successCalculator.success(mino, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(-1)  // 16576?
        }

        @Test
        fun testSpawn2() {
            val mino = MinimalOperationWithKey(minoFactory.create(Piece.Z, Rotate.Spawn), 5, 0, 0L)

            val success = successCalculator.success(mino, reachable)
            println(100.0 * success / successCalculator.allCount)
            assertThat(success).isEqualTo(-1)  // 11670?
        }
    }
}