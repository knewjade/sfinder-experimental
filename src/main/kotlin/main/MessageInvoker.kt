package main

import common.datastore.MinimalOperationWithKey
import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import core.action.candidate.LockedCandidate
import core.action.reachable.LockedReachable
import core.field.FieldFactory
import helper.Patterns
import percent.Index
import percent.SearchingPieces
import percent.SolutionLoader
import percent.Success
import java.nio.file.FileSystems
import java.nio.file.Paths

interface MessageInvoker {
    fun invoke(cycle: Int): Results?

    fun shutdown()
}
//
//class SingleThreadMessageInvoker(val factories: Factories, minimumSuccessRate: Double) : MessageInvoker {
//    val invoker: ConcurrentCheckerInvoker = createInvoker(factories.minoFactory, minimumSuccessRate)
//
//    private fun createInvoker(minoFactory: MinoFactory, minimumSuccessRate: Double, maxY: Int = 4): ConcurrentCheckerInvoker {
//        val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
//        val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
//        val reachableThreadLocal = LockedReachableThreadLocal(maxY)
//        val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
//        return SingleCheckerUsingHoldInvoker(commonObj, minimumSuccessRate)
//    }
//
//    override fun invoke(input: Input): Results? {
//        return search(factories, invoker, input)
//    }
//
//    override fun shutdown() {
//    }
//}
//
//class MultiThreadMessageInvoker(val factories: Factories, minimumSuccessRate: Double) : MessageInvoker {
//    val executorService = createExecutorService()
//    val invoker = createInvoker(factories.minoFactory, executorService, minimumSuccessRate)
//
//    private fun createExecutorService(): ExecutorService {
//        val core = Runtime.getRuntime().availableProcessors()
//        println("available processors: ${core}")
//        return Executors.newFixedThreadPool(core)
//    }
//
//    private fun createInvoker(minoFactory: MinoFactory, executorService: ExecutorService, minimumSuccessRate: Double, maxY: Int = 4): ConcurrentCheckerInvoker {
//        val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
//        val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
//        val reachableThreadLocal = LockedReachableThreadLocal(maxY)
//        val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
//        return ConcurrentCheckerUsingHoldInvoker(executorService, commonObj, minimumSuccessRate)
//    }
//
//    override fun invoke(input: Input): Results? {
//        return search(factories, invoker, input)
//    }
//
//    override fun shutdown() {
//        executorService.shutdown()
//    }
//}

class LoadBaseMessageInvoker(val input: Input, val factories: Factories, val index: Index) : MessageInvoker {
    val solutionLoader: SolutionLoader

    init {
        solutionLoader = SolutionLoader(Paths.get("input/indexed_solutions_10x4_SRS.csv"), index, input.headPiecesInt)
    }

    override fun invoke(cycle: Int): Results? {
        val height = 4
        val next = input.nextPiece

        val usingPieces = input.headPiecesMinos.map { it.piece } + input.nextPiece

        val searchingPieces = SearchingPieces(Patterns.hold(cycle), PieceCounter(usingPieces))

        val allCount = searchingPieces.allCount
        println("pieces: ${allCount}")

        if (allCount == 0)
            return null

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

            val result = Result(index.get(mino)!!, success)

            println("  -> ${result.success}")

            result
        }

        return Results(allCount, details)
    }

    override fun shutdown() {
    }
}

//
//fun search(factories: Factories, invoker: ConcurrentCheckerInvoker, input: Input): Results? {
//    val cycle = input.cycleNumber
//    val headPieces = input.headPieces
//    val next = input.nextPiece
//
//    val initState = parseToField(input.headPiecesMinos)
//    val moves = move(initState, next, factories)
//    println("moves: ${moves.size}")
//
//    val pattern = Patterns.hold(cycle)
//    val headPieceCounter = parseHeadBlockCounter(headPieces)
//    val searchPieces = createSearchPieces(pattern, headPieceCounter, next).toList()
//
//    val allCount = searchPieces.size
//    println("pieces: ${allCount}")
//
//    if (allCount == 0)
//        return null
//
//    val details = moves.map {
//        println("searching: ${it}")
//        val successCount = try {
//            calculateCount(it, invoker, searchPieces)
//        } catch (e: FinderExecuteCancelException) {
//            -1
//        }
//
//        val data = encodeToFumen(factories, it)
//
//        val result = Result(it, successCount, data)
//
//        println("  -> ${result.success}")
//
//        result
//    }
//
//    return Results(allCount, details)
//}
//
//internal fun encodeToFumen(factories: Factories, state: State): String {
//    val tetfu = Tetfu(factories.minoFactory, factories.colorConverter)
//    return tetfu.encode(listOf(TetfuElement(parseColoredField(state.field), state.maxClearLine.toString())))
//}
//
//private fun calculateCount(state: State, invoker: ConcurrentCheckerInvoker, searchPieces: List<Pieces>): Int {
//    val maxDepth = (state.maxClearLine * 10 - state.field.numOfAllBlocks) / 4
//    val results = invoker.search(state.field, searchPieces, state.maxClearLine, maxDepth)
//    return results.count { it.value!! }
//}
//
private fun parseToField(minos: List<MinoOperationWithKey>, height: Int): State {
    val field = FieldFactory.createField(height)
    minos.forEach {
        val minoField = FieldFactory.createField(height)
        minoField.put(it.mino, it.x, it.y)
        minoField.insertWhiteLineWithKey(it.needDeletedKey)
        field.merge(minoField)
    }
    return State(field, height)
}

//private fun move(init: State, next: Piece, factories: Factories): List<State> {
//    val maxClearLine = init.maxClearLine
//    val minoFactory = factories.minoFactory
//    val field = init.field
//
//    val candidate = LockedCandidate(minoFactory, factories.minoShifter, factories.minoRotation, maxClearLine)
//    val actions = candidate.search(field, next, maxClearLine)
//    return actions.map {
//        val freeze = field.freeze(maxClearLine)
//        val mino = minoFactory.create(next, it.rotate)
//        freeze.put(mino, it.x, it.y)
//        val deleteLine = freeze.clearLine()
//        State(freeze, maxClearLine - deleteLine)
//    }
//}
//
//private fun parseHeadBlockCounter(headPieces: String): PieceCounter {
//    val takeIf = headPieces.takeIf { it != "" }
//    return takeIf?.let { LoadedPatternGenerator(it).blockCountersStream().findFirst().get() } ?: PieceCounter.EMPTY
//}
//
//internal fun createSearchPieces(pattern: String, headPieceCounter: PieceCounter, next: Piece): Set<Pieces> {
//    val generator = LoadedPatternGenerator(pattern)
//
//    val needPieceCounter = headPieceCounter.addAndReturnNew(listOf(next))
//    val numOfHead = needPieceCounter.blockStream.count() + 1
//
//    return generator.blocksStream().parallel()
//            .filter {
//                val pieceCounter = PieceCounter(it.blockStream().limit(numOfHead))
//                pieceCounter.containsAll(needPieceCounter)
//            }
//            .map {
//                val pieces = it.pieces
//                for (entry in needPieceCounter.enumMap.entries)
//                    for (x in 0 until entry.value)
//                        pieces.remove(entry.key)
//                LongPieces(pieces)
//            }
//            .collect(Collectors.toSet())
//}
//
//private fun parseColoredField(field: Field): ColoredField {
//    val coloredField = ArrayColoredField(24)
//    for (y in 0 until field.maxFieldHeight)
//        for (x in 0 until 10)
//            if (!field.isEmpty(x, y))
//                coloredField.setColorType(ColorType.Gray, x, y)
//    return coloredField
//}