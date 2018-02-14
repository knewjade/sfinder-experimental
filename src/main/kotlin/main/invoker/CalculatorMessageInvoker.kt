package main.invoker

import common.datastore.MinimalOperationWithKey
import common.datastore.action.Action
import concurrent.LockedCandidateThreadLocal
import concurrent.LockedReachableThreadLocal
import concurrent.checker.CheckerUsingHoldThreadLocal
import concurrent.checker.invoker.CheckerCommonObj
import concurrent.checker.invoker.ConcurrentCheckerInvoker
import concurrent.checker.invoker.using_hold.SingleCheckerUsingHoldInvoker
import core.action.candidate.LockedCandidate
import core.mino.MinoFactory
import core.mino.Piece
import main.domain.*
import main.percent.Index

class CalculatorMessageInvoker(private val headPieces: HeadPieces, private val factories: Factories, val index: Index) : MessageInvoker {
    val invoker: ConcurrentCheckerInvoker = createInvoker(factories.minoFactory)

    private fun createInvoker(minoFactory: MinoFactory, maxY: Int = 4): ConcurrentCheckerInvoker {
        val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
        val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
        val reachableThreadLocal = LockedReachableThreadLocal(maxY)
        val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
        return SingleCheckerUsingHoldInvoker(commonObj)
    }

    override fun invoke(cycle: Cycle): Results {
        val searchingPieces = toSearchingPieces(cycle, headPieces)
        val allCount = searchingPieces.allCount
        println("pieces: $allCount")

        if (allCount == 0)
            return Results(Counter(0), emptyList())

        val fieldHeight = 4
        val initState = parseToField(headPieces.headMinos, fieldHeight)

        val current = headPieces.current
        val actions = nextActions(initState, current)

        println("moves: ${actions.size}")

        val maxDepth = 10 - headPieces.headMinos.size - 1
        val allPieces = searchingPieces.piecesMap.values.flatMap { it }
        println("Max depth: $maxDepth")

        val details = actions.map {
            println("searching: $it")
            val mino = factories.minoFactory.create(current, it.rotate)
            val operationWithKey = MinimalOperationWithKey(mino, it.x, it.y, 0L)

            val freeze = initState.field.freeze(initState.maxClearLine)
            freeze.put(mino, it.x, it.y)
            val clearLine = freeze.clearLine()
            val state = State(freeze, initState.maxClearLine - clearLine)

            val success = if (0 < maxDepth) {
                val list = invoker.search(state.field, allPieces, state.maxClearLine, maxDepth)
                list.count { it.value }
            } else {
                allPieces.size
            }

            val fieldData = encodeToFumen(factories, state)

            val result = Result(MinoIndex(index.get(operationWithKey)!!), Counter(success), fieldData)

            println("  -> $result")

            result
        }

        return Results(Counter(allCount), details)
    }

    private fun nextActions(initState: State, current: Piece): Set<Action> {
        val minoFactory = factories.minoFactory
        val minoShifter = factories.minoShifter
        val minoRotation = factories.minoRotation

        val maxClearLine = initState.maxClearLine

        val candidate = LockedCandidate(minoFactory, minoShifter, minoRotation, maxClearLine)
        return candidate.search(initState.field, current, maxClearLine)
    }

    override fun shutdown() {
    }
}