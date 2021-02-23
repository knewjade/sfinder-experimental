package components

import common.datastore.MinimalOperationWithKey
import common.datastore.MinoOperation
import common.datastore.MinoOperationWithKey
import common.datastore.SimpleMinoOperation
import core.mino.MinoFactory
import core.mino.Piece
import core.srs.Rotate

class Indexer(
    private val minoFactory: MinoFactory,
    private val height: Int
) {
    val maxIndex: Int = 10 * height * Piece.getSize() * Rotate.getSize()

    fun toIndex(piece: Piece, rotate: Rotate, x: Int, y: Int): Int {
        return x +
                y * 10 +
                rotate.number * 10 * height +
                piece.number * 10 * height * 4
    }

    private fun toMinoOperation(index: Int): MinoOperation {
        var value = index
        val x = value % 10
        value /= 10
        val y = value % height
        value /= height
        val rotate = Rotate.getRotate(value % 4)
        value /= 4
        val piece = Piece.getBlock(value)
        return SimpleMinoOperation(minoFactory.create(piece, rotate), x, y)
    }

    fun toMinoOperationWithKey(index: Int): MinoOperationWithKey {
        val operation = toMinoOperation(index)
        return MinimalOperationWithKey(operation.mino, operation.x, operation.y, 0L)
    }
}