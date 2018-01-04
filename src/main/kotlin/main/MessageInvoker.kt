package main

import common.datastore.MinimalOperationWithKey
import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import common.datastore.action.Action
import common.tetfu.Tetfu
import common.tetfu.TetfuElement
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import common.tetfu.field.ColoredField
import concurrent.LockedCandidateThreadLocal
import concurrent.LockedReachableThreadLocal
import concurrent.checker.CheckerUsingHoldThreadLocal
import concurrent.checker.invoker.CheckerCommonObj
import concurrent.checker.invoker.ConcurrentCheckerInvoker
import concurrent.checker.invoker.using_hold.SingleCheckerUsingHoldInvoker
import core.action.candidate.LockedCandidate
import core.action.reachable.LockedReachable
import core.field.Field
import core.field.FieldFactory
import core.mino.MinoFactory
import exceptions.FinderExecuteCancelException
import helper.Patterns
import percent.Index
import percent.SearchingPieces
import percent.SolutionLoader
import percent.Success
import java.nio.file.Paths

interface MessageInvoker {
    fun invoke(cycle: Int): Results

    fun shutdown()
}

class LoadBaseMessageInvoker(val input: Input, private val factories: Factories, val index: Index) : MessageInvoker {
    private val solutionLoader: SolutionLoader = SolutionLoader(Paths.get("input/indexed_solutions_10x4_SRS.csv"), index, input.headPiecesInt)

    override fun invoke(cycle: Int): Results {
        val height = 4
        val next = input.nextPiece

        val usingPieces = input.headPiecesMinos.map { it.piece } + input.nextPiece

        val searchingPieces = SearchingPieces(Patterns.hold(cycle), PieceCounter(usingPieces))

        val allCount = searchingPieces.allCount
        println("pieces: ${allCount}")

        if (allCount == 0)
            return Results(0, emptyList())

        val initState = parseToField(input.headPiecesMinos, height)

        val successCalculator = Success(solutionLoader, index, searchingPieces, height)

        val minoFactory = factories.minoFactory
        val minoShifter = factories.minoShifter
        val minoRotation = factories.minoRotation
        val reachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)

        val candidate = LockedCandidate(minoFactory, minoShifter, minoRotation, initState.maxClearLine)
        val actions = candidate.search(initState.field, next, initState.maxClearLine)
        println("moves: ${actions.size}")

        val details = actions.map {
            println("searching: ${it}")
            val mino = MinimalOperationWithKey(minoFactory.create(next, it.rotate), it.x, it.y, 0L)

            val success = successCalculator.success(mino, reachable)
            val state = minoToField(mino, height)
            val fieldData = encodeToFumen(factories, state)

            val result = Result(index.get(mino)!!, success, fieldData)

            println("  -> $result")

            result
        }

        return Results(allCount, details)
    }

    private fun minoToField(mino: MinimalOperationWithKey, height: Int): State {
        return parseToField(solutionLoader.requires + mino, height)
    }

    override fun shutdown() {
    }
}

class SingleThreadMessageInvoker(val input: Input, private val factories: Factories, val index: Index) : MessageInvoker {

    val invoker: ConcurrentCheckerInvoker = createInvoker(factories.minoFactory)

    private fun createInvoker(minoFactory: MinoFactory, maxY: Int = 4): ConcurrentCheckerInvoker {
        val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
        val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
        val reachableThreadLocal = LockedReachableThreadLocal(maxY)
        val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
        return SingleCheckerUsingHoldInvoker(commonObj)
    }

    override fun invoke(cycle: Int): Results {
        val height = 4
        val next = input.nextPiece

        val usingPieces = input.headPiecesMinos.map { it.piece } + input.nextPiece

        val searchingPieces = SearchingPieces(Patterns.hold(cycle), PieceCounter(usingPieces))

        val allPieces = searchingPieces.piecesMap.values.flatMap { it }

        val allCount = searchingPieces.allCount
        println("pieces: ${allCount}")

        if (allCount == 0)
            return Results(0, emptyList())

        val initState = parseToField(input.headPiecesMinos, height)

        val minoFactory = factories.minoFactory
        val minoShifter = factories.minoShifter
        val minoRotation = factories.minoRotation

        val candidate = LockedCandidate(minoFactory, minoShifter, minoRotation, initState.maxClearLine)
        val actions = candidate.search(initState.field, next, initState.maxClearLine)
        println("moves: ${actions.size}")

        val maxDepth = 10 - input.headPiecesMinos.size - 1
        println("Max depth: $maxDepth")

        val details = actions.map {
            println("searching: ${it}")
            val mino = MinimalOperationWithKey(minoFactory.create(next, it.rotate), it.x, it.y, 0L)

            val state = minoToField(mino, height)
            val success = if (0 < maxDepth) {
                try {
                    val list = invoker.search(state.field, allPieces, state.maxClearLine, maxDepth)
                    list.count { it.value }
                } catch (e: FinderExecuteCancelException) {
                    -1
                }
            } else {
                allPieces.size
            }

            val fieldData = encodeToFumen(factories, state)

            val result = Result(index.get(mino)!!, success, fieldData)

            println("  -> $result")

            result
        }

        return Results(allCount, details)
    }

    private fun minoToField(mino: MinimalOperationWithKey, height: Int): State {
        return parseToField(input.headPiecesMinos + mino, height)
    }

    override fun shutdown() {
    }
}

class DummyInvoker(val input: Input, private val factories: Factories, val index: Index) : MessageInvoker {
    private val solutionLoader: SolutionLoader = SolutionLoader(Paths.get("input/indexed_solutions_10x4_SRS.csv"), index, input.headPiecesInt)

    override fun invoke(cycle: Int): Results {
        val height = 4
        val next = input.nextPiece

        val usingPieces = input.headPiecesMinos.map { it.piece } + input.nextPiece

        val searchingPieces = SearchingPieces(Patterns.hold(cycle), PieceCounter(usingPieces))

        val allCount = searchingPieces.allCount
        println("pieces: ${allCount}")

        if (allCount == 0)
            return Results(0, emptyList())

        val initState = parseToField(input.headPiecesMinos, height)

        val minoFactory = factories.minoFactory
        val minoShifter = factories.minoShifter
        val minoRotation = factories.minoRotation

        val candidate = LockedCandidate(minoFactory, minoShifter, minoRotation, initState.maxClearLine)
        val actions = candidate.search(initState.field, next, initState.maxClearLine)
        println("moves: ${actions.size}")

        val details = actions.map {
            println("searching: ${it}")
            val mino = MinimalOperationWithKey(minoFactory.create(next, it.rotate), it.x, it.y, 0L)

            val success = (allCount - 100).takeIf { 0 < it } ?: allCount
            val state = minoToField(mino, height)
            val fieldData = encodeToFumen(factories, state)

            val result = Result(index.get(mino)!!, success, fieldData)

            println("  -> $result")

            result
        }

        return Results(allCount, details)
    }

    private fun minoToField(mino: MinimalOperationWithKey, height: Int): State {
        return parseToField(solutionLoader.requires + mino, height)
    }

    override fun shutdown() {
    }
}

private fun parseToField(minos: List<MinoOperationWithKey>, height: Int): State {
    val field = FieldFactory.createField(height)
    minos.forEach {
        val minoField = FieldFactory.createField(height)
        minoField.put(it.mino, it.x, it.y)
        minoField.insertWhiteLineWithKey(it.needDeletedKey)
        field.merge(minoField)
    }
    val deleteKey = field.clearLineReturnKey()

    return State(field, height - java.lang.Long.bitCount(deleteKey))
}

private fun encodeToFumen(factories: Factories, state: State): String {
    fun parseGrayField(field: Field): ColoredField {
        val coloredField = ArrayColoredField(24)
        for (y in 0 until field.maxFieldHeight)
            for (x in 0 until 10)
                if (!field.isEmpty(x, y))
                    coloredField.setColorType(ColorType.Gray, x, y)
        return coloredField
    }

    val tetfu = Tetfu(factories.minoFactory, factories.colorConverter)
    return tetfu.encode(listOf(TetfuElement(parseGrayField(state.field), state.maxClearLine.toString())))
}