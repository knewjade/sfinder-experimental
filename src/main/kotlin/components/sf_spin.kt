package components

import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import concurrent.RotateReachableThreadLocal
import core.field.Field
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import createLockedReachableSpinChecker
import functions.getTSpin
import searcher.spins.FirstPreSpinRunner
import searcher.spins.FullSpinRunner
import searcher.spins.SecondPreSpinRunner
import searcher.spins.spin.TSpins
import java.util.stream.Collectors

/**
 * solution-finderのspinコマンドのcoreを使って、すべてのTスピンをみつける
 * すべてのミノが使われるとは限らない
 * Tミノ以外でラインが揃う地形は除外する
 */
class SFSpin(
    private val minoFactory: MinoFactory,
    private val minoShifter: MinoShifter,
    minoRotation: MinoRotation,
    private val height: Int
) {
    private val reachableThreadLocal = RotateReachableThreadLocal(minoFactory, minoShifter, minoRotation, height)

    fun run(field: Field, requiredClearLine: Int): List<MinoOperationWithKeysList> {
        return run(field, PieceCounter(Piece.valueList()), requiredClearLine)
    }

    private fun run(
        initField: Field, pieceCounter: PieceCounter, requiredClearLine: Int
    ): List<MinoOperationWithKeysList> {
        val allowFillMaxHeight = height - 2

        val firstPreSpinRunner = FirstPreSpinRunner(
            minoFactory, minoShifter, reachableThreadLocal, 0, allowFillMaxHeight, height, height
        )
        val secondPreSpinRunner = SecondPreSpinRunner(firstPreSpinRunner, initField, pieceCounter, Int.MAX_VALUE)
        val runner = FullSpinRunner()
        val results = runner.search(secondPreSpinRunner, requiredClearLine)

        val spinChecker = createLockedReachableSpinChecker(height)

        return results.parallel()
            .filter { candidate ->
                val field = candidate.allMergedFieldWithoutT
                field.filledLine == 0L
            }
            .map { candidate ->
                val result = candidate.result
                result.operationStream().collect(Collectors.toList<MinoOperationWithKey>())
            }
            .map { MinoOperationWithKeysList(it) }
            .filter { operationWithKeys ->
                val spin = getTSpin(initField, operationWithKeys, spinChecker, height)
                assert(spin != null && requiredClearLine <= spin.clearedLine)
                spin != null && spin.spin == TSpins.Regular
            }
            .collect(Collectors.toList())
    }
}