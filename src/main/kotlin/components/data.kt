package components

import common.datastore.BlockField
import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import common.parser.OperationTransform
import core.field.Field

data class MinoOperationWithKeysList(
    val operationWithKeys: List<MinoOperationWithKey>
) {
    fun field(initField: Field, height: Int): Field {
        val freeze = initField.freeze(height)
        operationWithKeys.forEach {
            freeze.merge(it.createMinoField(height))
        }
        return freeze
    }

    fun usedPieceCounter(): PieceCounter {
        return PieceCounter(operationWithKeys.map { it.piece })
    }

    fun toMutableList(): MutableList<MinoOperationWithKey> {
        return operationWithKeys.toMutableList()
    }

    fun blockField(height: Int): BlockField {
        val blockField = BlockField(24)
        operationWithKeys.forEach {
            blockField.merge(it.createMinoField(height), it.piece)
        }
        return blockField
    }
}