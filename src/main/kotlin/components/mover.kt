package components

import common.datastore.MinoOperation
import common.datastore.SimpleMinoOperation
import common.datastore.action.Action
import concurrent.LockedCandidateThreadLocal
import core.action.candidate.Candidate
import core.field.Field
import core.mino.MinoFactory
import core.mino.Piece

class LockedCandidateMoverThreadLocal(
    private val minoFactory: MinoFactory,
    private val lockedCandidateThreadLocal: LockedCandidateThreadLocal,
    private val height: Int
) : ThreadLocal<Mover>() {
    override fun initialValue(): Mover {
        val lockedCandidate = lockedCandidateThreadLocal.get()
        return Mover(minoFactory, lockedCandidate, height)
    }

    companion object {
        fun create(height: Int): LockedCandidateMoverThreadLocal {
            return LockedCandidateMoverThreadLocal(MinoFactory(), LockedCandidateThreadLocal(height), height)
        }
    }
}

class Mover(
    private val minoFactory: MinoFactory,
    private val candidate: Candidate<Action>,
    private val height: Int
) {
    fun moveWithoutClearNoHold(
        field: Field, piece: Piece, prev: Op?
    ): List<Op> {
        return candidate.search(field.freeze(), piece, height)
            .map {
                val mino = minoFactory.create(piece, it.rotate)
                SimpleMinoOperation(mino, it.x, it.y)
            }
            .filter {
                val freeze = field.freeze()
                freeze.put(it.mino, it.x, it.y)
                freeze.filledLine == 0L
            }
            .map {
                Op(prev, it)
            }
    }
}

data class Op(
    private val prev: Op?,
    private val operation: MinoOperation
) {
    fun field(initField: Field): Field {
        val freeze = initField.freeze()
        field(freeze, freeze.maxFieldHeight)
        return freeze
    }

    private fun field(field: Field, height: Int) {
        prev?.field(field, height)
        field.put(operation.mino, operation.x, operation.y)
        field.clearLine()
    }

    fun operations(): List<MinoOperation> {
        val list = mutableListOf<MinoOperation>()
        operations(list)
        return list
    }

    private fun operations(list: MutableList<MinoOperation>) {
        prev?.operations(list)
        list.add(operation)
    }
}
