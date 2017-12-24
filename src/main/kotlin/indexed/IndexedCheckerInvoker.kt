package indexed

import common.datastore.MinimalOperationWithKey
import common.datastore.OperationWithKey
import common.datastore.Pair
import common.datastore.blocks.Pieces
import concurrent.checker.invoker.ConcurrentCheckerInvoker
import core.field.Field

class IndexedCheckerInvoker(index: Map<Int, MinimalOperationWithKey>) : ConcurrentCheckerInvoker {
    fun search(operationWithKey: List<OperationWithKey>, searchingPieces: List<Pieces>, maxClearLine: Int, maxDepth: Int): List<Pair<Pieces, Boolean>> {
//        operationWithKey
        TODO("not implemented")
    }

    override fun search(field: Field, searchingPieces: List<Pieces>, maxClearLine: Int, maxDepth: Int): List<Pair<Pieces, Boolean>> {
        TODO("not implemented")
    }
}