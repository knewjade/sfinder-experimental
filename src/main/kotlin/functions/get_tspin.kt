package functions

import common.SpinChecker
import components.MinoOperationWithKeysList
import core.field.Field
import core.mino.Piece
import searcher.spins.spin.Spin
import java.lang.Long.bitCount

// Tミノを含む手順を入力して、Tスピンを探す
fun getTSpin(
    initField: Field, operationsWithKey: MinoOperationWithKeysList, checker: SpinChecker, height: Int
): Spin? {
    // Tミノ以外のフィールド
    val withoutT = initField.freeze()
    operationsWithKey.operationWithKeys.filter { it.piece != Piece.T }.forEach {
        withoutT.merge(it.createMinoField(height))
    }
    val clearLineWithoutT = bitCount(withoutT.filledLine)

    // Tミノ
    val t = operationsWithKey.operationWithKeys.find { it.piece == Piece.T } ?: error("Not found T-mino")

    // Tミノで削除されるライン数
    val clearedLine = run {
        val freeze = withoutT.freeze()
        freeze.put(t.mino, t.x, t.y)
        freeze.clearLine()
    }

    // Tスピンできるか
    val result = checker.check(withoutT, t, height, clearedLine - clearLineWithoutT)
    return result.orElse(null)
}