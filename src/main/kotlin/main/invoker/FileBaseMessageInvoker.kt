package main.invoker

import common.datastore.MinimalOperationWithKey
import common.datastore.action.Action
import core.action.candidate.LockedCandidate
import core.action.reachable.LockedReachable
import core.mino.Piece
import main.domain.*
import percent.Index
import percent.SolutionLoader
import percent.Success
import java.nio.file.Paths

class FileBaseMessageInvoker(private val headPieces: HeadPieces, private val factories: Factories, val index: Index) : MessageInvoker {
    private val headIndexes = headPieces.headMinos.map { index.get(it)!! }.toSet()
    private val solutionLoader: SolutionLoader = SolutionLoader(Paths.get("input/indexed_solutions_10x4_SRS.csv"), index, headIndexes)

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

        println("loading solution ...")
        val successCalculator = Success(solutionLoader, index, searchingPieces, fieldHeight)
        val reachable = LockedReachable(factories.minoFactory, factories.minoShifter, factories.minoRotation, fieldHeight)

        val details = actions.map {
            println("searching: $it")
            val mino = factories.minoFactory.create(current, it.rotate)
            val operationWithKey = MinimalOperationWithKey(mino, it.x, it.y, 0L)

            val freeze = initState.field.freeze(initState.maxClearLine)
            freeze.put(mino, it.x, it.y)
            val clearLine = freeze.clearLine()
            val state = State(freeze, initState.maxClearLine - clearLine)

            val success = successCalculator.success(operationWithKey, reachable)

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