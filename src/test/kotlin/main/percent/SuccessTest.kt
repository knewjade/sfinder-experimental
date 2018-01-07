package main.percent

import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import core.action.reachable.LockedReachable
import core.action.reachable.Reachable
import core.mino.Mino
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.Rotate
import helper.Patterns
import main.domain.AllMinoIndexes
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class SuccessTest {
    @Nested
    class Direct {
        @Nested
        class HeadLZ {
            companion object {
                private val height = 4
                private val minoFactory = MinoFactory()
                private val minoShifter = MinoShifter()
                private var successCalculator: Success? = createSuccess()
                private val reachable = createReachable()

                private fun createSuccess(): Success {
                    val minos = listOf(
                            MinimalOperationWithKey(Mino(Piece.L, Rotate.Right), 0, 1, 0L),
                            MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 2, 0, 0L)
                    )

                    val path = Paths.get("input/index.csv")
                    val index = Index(minoFactory, minoShifter, path)

                    val allSolutionsPath = Paths.get("input/indexed_solutions_10x4_SRS.csv")
                    val solutionLoader = DirectSolutionLoader(allSolutionsPath, index, minos.map { index.get(it)!! }.toSet())

                    val searchingPieces = SearchingPieces(Patterns.hold(1), PieceCounter(minos.map { it.piece }))

                    return Success(solutionLoader, index, searchingPieces, height)
                }

                private fun createReachable(): Reachable {
                    val minoShifter = MinoShifter()
                    val minoRotation = MinoRotation()
                    return LockedReachable(minoFactory, minoShifter, minoRotation, height)
                }
            }

            @AfterClass
            fun tearDownClass() {
                successCalculator = null
                println("tearDown")
            }

            @Test
            fun testTReverse() {
                val mino = MinimalOperationWithKey(minoFactory.create(Piece.T, Rotate.Reverse), 1, 3, 0L)

                val success = successCalculator!!.success(mino, reachable)
                println(100.0 * success / successCalculator!!.allCount)
                assertThat(success).isEqualTo(100800)
            }

            @Test
            fun testSLeft() {
                val mino = MinimalOperationWithKey(minoFactory.create(Piece.S, Rotate.Left), 9, 1, 0L)

                val success = successCalculator!!.success(mino, reachable)
                println(100.0 * success / successCalculator!!.allCount)
                assertThat(success).isEqualTo(25000)
            }
        }
    }

    @Nested
    class Cached {
        @Nested
        class HeadLZ {
            companion object {
                private val height = 4
                private val minoFactory = MinoFactory()
                private val minoShifter = MinoShifter()
                private var successCalculator: Success? = createSuccess()
                private val reachable = createReachable()

                private fun createSuccess(): Success {
                    val minos = listOf(
                            MinimalOperationWithKey(Mino(Piece.L, Rotate.Right), 0, 1, 0L),
                            MinimalOperationWithKey(Mino(Piece.Z, Rotate.Spawn), 2, 0, 0L)
                    )

                    val path = Paths.get("input/index.csv")
                    val index = Index(minoFactory, minoShifter, path)

                    val allSolutionsPath = Paths.get("input/indexed_solutions_10x4_SRS.csv")
                    val allMinoIndexes = AllMinoIndexes(allSolutionsPath)
                    val solutionLoader = CachedSolutionLoader(allMinoIndexes, index, minos.map { index.get(it)!! }.toSet())

                    val searchingPieces = SearchingPieces(Patterns.hold(1), PieceCounter(minos.map { it.piece }))

                    return Success(solutionLoader, index, searchingPieces, height)
                }

                private fun createReachable(): Reachable {
                    val minoShifter = MinoShifter()
                    val minoRotation = MinoRotation()
                    return LockedReachable(minoFactory, minoShifter, minoRotation, height)
                }
            }

            @AfterClass
            fun tearDownClass() {
                successCalculator = null
                println("tearDown")
            }

            @Test
            fun testReverse() {
                val mino = MinimalOperationWithKey(minoFactory.create(Piece.T, Rotate.Reverse), 1, 3, 0L)

                val success = successCalculator!!.success(mino, reachable)
                println(100.0 * success / successCalculator!!.allCount)
                assertThat(success).isEqualTo(100800)
            }

            @Test
            fun testSLeft() {
                val mino = MinimalOperationWithKey(minoFactory.create(Piece.S, Rotate.Left), 9, 1, 0L)

                val success = successCalculator!!.success(mino, reachable)
                println(100.0 * success / successCalculator!!.allCount)
                assertThat(success).isEqualTo(25000)
            }
        }
    }
}