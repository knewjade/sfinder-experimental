package main

import common.datastore.PieceCounter
import common.datastore.action.Action
import common.datastore.blocks.LongPieces
import common.datastore.blocks.Pieces
import common.pattern.LoadedPatternGenerator
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
import concurrent.checker.invoker.using_hold.ConcurrentCheckerUsingHoldInvoker
import concurrent.checker.invoker.using_hold.SingleCheckerUsingHoldInvoker
import core.action.candidate.LockedCandidate
import core.field.Field
import core.field.FieldFactory
import core.mino.Mino
import core.mino.MinoFactory
import core.mino.Piece
import exceptions.FinderExecuteCancelException
import helper.Patterns
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors

interface MessageInvoker {
    fun invoke(input: Input): Results?

    fun shutdown()
}

class SingleThreadMessageInvoker(val factories: Factories, minimumSuccessRate: Double) : MessageInvoker {
    val invoker: ConcurrentCheckerInvoker = createInvoker(factories.minoFactory, minimumSuccessRate)

    private fun createInvoker(minoFactory: MinoFactory, minimumSuccessRate: Double, maxY: Int = 4): ConcurrentCheckerInvoker {
        val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
        val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
        val reachableThreadLocal = LockedReachableThreadLocal(maxY)
        val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
        return SingleCheckerUsingHoldInvoker(commonObj, minimumSuccessRate)
    }

    override fun invoke(input: Input): Results? {
        return search(factories, invoker, input)
    }

    override fun shutdown() {
    }
}

class MultiThreadMessageInvoker(val factories: Factories, val minimumSuccessRate: Double) : MessageInvoker {
    val executorService = createExecutorService()
    val invoker = createInvoker(factories.minoFactory, executorService, minimumSuccessRate)

    private fun createExecutorService(): ExecutorService {
        val core = Runtime.getRuntime().availableProcessors()
        println("available processors: ${core}")
        return Executors.newFixedThreadPool(core)
    }

    private fun createInvoker(minoFactory: MinoFactory, executorService: ExecutorService, minimumSuccessRate: Double, maxY: Int = 4): ConcurrentCheckerInvoker {
        val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
        val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
        val reachableThreadLocal = LockedReachableThreadLocal(maxY)
        val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
        return ConcurrentCheckerUsingHoldInvoker(executorService, commonObj, minimumSuccessRate)
    }

    override fun invoke(input: Input): Results? {
        return search(factories, invoker, input)
    }

    override fun shutdown() {
        executorService.shutdown()
    }
}

fun search(factories: Factories, invoker: ConcurrentCheckerInvoker, input: Input): Results? {
    val cycle = input.cycleNumber
    val fieldData = input.data
    val headPieces = input.headPieces
    val next = input.nextPiece

    val initState = parseToField(fieldData, factories)
    val moves = move(initState, next, factories)
    println("moves: ${moves.size}")

    val pattern = Patterns.hold(cycle)
    val headPieceCounter = parseHeadBlockCounter(headPieces)
    val searchPieces = createSearchPieces(pattern, headPieceCounter, next).toList()

    val allCount = searchPieces.size
    println("pieces: ${allCount}")

    if (allCount == 0)
        return null

    val details = moves.map {
        println("searching: ${it}")
        val successCount = try {
            calculateCount(it, invoker, searchPieces)
        } catch (e: FinderExecuteCancelException) {
            -1
        }

        val data = encodeToFumen(factories, it)

        val result = Result(it, successCount, data)

        println("  -> ${result.success}")

        result
    }

    return Results(allCount, details)
}

internal fun encodeToFumen(factories: Factories, state: State): String {
    val tetfu = Tetfu(factories.minoFactory, factories.colorConverter)
    return tetfu.encode(listOf(TetfuElement(parseColoredField(state.field), state.maxClearLine.toString())))
}

private fun calculateCount(state: State, invoker: ConcurrentCheckerInvoker, searchPieces: List<Pieces>): Int {
    val maxDepth = (state.maxClearLine * 10 - state.field.numOfAllBlocks) / 4
    val results = invoker.search(state.field, searchPieces, state.maxClearLine, maxDepth)
    return results.count { it.value!! }
}

private fun parseToField(data: String, factories: Factories): State {
    val minoFactory = factories.minoFactory
    val colorConverter = factories.colorConverter
    val tetfu = Tetfu(minoFactory, colorConverter)

    val decode = tetfu.decode(data)
    val page = decode[0]

    val coloredField = page.field
    if (page.isPutMino) {
        val piece = colorConverter.parseToBlock(page.colorType)
        val mino = Mino(piece, page.rotate)
        coloredField.putMino(mino, page.x, page.y)
    }

    val height = if (page.comment != "") Integer.valueOf(page.comment) else 4
    val empty = ColorType.Empty.number
    val field = FieldFactory.createField(height)
    for (y in 0 until height)
        for (x in 0..9)
            if (coloredField.getBlockNumber(x, y) != empty)
                field.setBlock(x, y)

    return State(field, height)
}

private fun move(init: State, next: Piece, factories: Factories): List<State> {
    val maxClearLine = init.maxClearLine
    val minoFactory = factories.minoFactory
    val field = init.field

    val candidate = LockedCandidate(minoFactory, factories.minoShifter, factories.minoRotation, maxClearLine)
    val actions = candidate.search(field, next, maxClearLine)
    return actions.map {
        val freeze = field.freeze(maxClearLine)
        val mino = minoFactory.create(next, it.rotate)
        freeze.put(mino, it.x, it.y)
        val deleteLine = freeze.clearLine()
        State(freeze, maxClearLine - deleteLine)
    }
}

private fun parseHeadBlockCounter(headPieces: String): PieceCounter {
    val takeIf = headPieces.takeIf { it != "" }
    return takeIf?.let { LoadedPatternGenerator(it).blockCountersStream().findFirst().get() } ?: PieceCounter.EMPTY
}

internal fun createSearchPieces(pattern: String, headPieceCounter: PieceCounter, next: Piece): Set<Pieces> {
    val generator = LoadedPatternGenerator(pattern)

    val needPieceCounter = headPieceCounter.addAndReturnNew(listOf(next))
    val numOfHead = needPieceCounter.blockStream.count() + 1

    return generator.blocksStream().parallel()
            .filter {
                val pieceCounter = PieceCounter(it.blockStream().limit(numOfHead))
                pieceCounter.containsAll(needPieceCounter)
            }
            .map {
                val pieces = it.pieces
                for (entry in needPieceCounter.enumMap.entries)
                    for (x in 0 until entry.value)
                        pieces.remove(entry.key)
                LongPieces(pieces)
            }
            .collect(Collectors.toSet())
}

private fun parseColoredField(field: Field): ColoredField {
    val coloredField = ArrayColoredField(24)
    for (y in 0 until field.maxFieldHeight)
        for (x in 0 until 10)
            if (!field.isEmpty(x, y))
                coloredField.setColorType(ColorType.Gray, x, y)
    return coloredField
}