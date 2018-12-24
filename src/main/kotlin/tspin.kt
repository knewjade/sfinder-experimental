import common.buildup.BuildUpStream
import common.datastore.*
import common.parser.OperationInterpreter
import common.parser.OperationTransform
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import concurrent.LockedReachableThreadLocal
import core.action.candidate.RotateCandidate
import core.field.Field
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import entry.path.output.MyFile
import searcher.pack.separable_mino.AllMinoFactory
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
/*
fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()
    val colorConverter = ColorConverter()

    val maxHeight = 4
    val factory = AllMinoFactory(minoFactory, minoShifter, 10, maxHeight, 0L)
    val all = factory.create().toList()

    val eachMinY = all.asSequence()
            .filter { it != Piece.T }
            .groupBy { it.y + it.mino.minY }
            .toMap()

    // すべてのPieceをおく
    // 下から順に埋めていく
    val initField = FieldFactory.createField(maxHeight)
    val usingPieces = PieceCounter(Piece.values().filter { it != Piece.T })

    val done: MutableList<Result> = mutableListOf()
//    var tasks: MutableList<Result> = mutableListOf(EmptyResult(initField, usingPieces))

    val pieceCounterMap = EnumMap<Piece, PieceCounter>(Piece::class.java)
    Piece.values().forEach { pieceCounterMap.put(it, PieceCounter(Stream.of(it))) }

    fun k(result: Result, startY: Int) {
        (startY..(maxHeight - 1)).forEach { y ->
            val operationWithKeys = eachMinY[y] ?: listOf()

            for (op in operationWithKeys) {
                val pieceCounter = pieceCounterMap[op.piece]

                val field = result.field
                if (result.restPieces.containsAll(pieceCounter) && field.isOnGround(op.mino, op.x, op.y) && field.canPut(op.mino, op.x, op.y)) {
                    val nextPieceCounter = result.restPieces.removeAndReturnNew(pieceCounter)
                    val next = OperationResult(result, op, nextPieceCounter)
                    if (next.restPieces.counter == 0L) {
                        done.add(next)
                    } else {
                        k(next, y)
                    }
                }
            }

            // y行上にブロックがあるため、さらに積むことが出来る
            if (0 < result.field.getBlockCountOnY(y)) {
                return@forEach
            }
        }
    }

    k(EmptyResult(initField, usingPieces), 0)

    /*
    (0..(maxHeight - 1)).forEach { y ->
        val operationWithKeys = eachMinY[y] ?: return@forEach

        println("$y: ${operationWithKeys.size}")

        // 1ミノ進める
        // もし進めた場合、同じ高さのミノをさらに追加出来る可能性があるため、poolに戻す
        val nextTasks = mutableListOf<Result>()

        val pool = LinkedList(tasks)
        while (pool.isNotEmpty()) {
            val result = pool.removeLast()

            for (op in operationWithKeys) {
                val pieceCounter = k[op.piece]

                val field = result.field
                if (result.restPieces.containsAll(pieceCounter) && field.isOnGround(op.mino, op.x, op.y) && field.canPut(op.mino, op.x, op.y)) {
                    val next = OperationResult(result, op)
                    if (next.restPieces.counter == 0L) {
                        done.add(next)
                    } else {
                        pool.addLast(next)
                    }
                }
            }

            // y行上にブロックがあるため、さらに積むことが出来る
            if (0 < result.field.getBlockCountOnY(y)) {
                nextTasks.add(result)
            }
        }

        tasks = nextTasks

        println("  -> Tasks=${tasks.size}, Done=${done.size}")
    }
    */

    val results: List<Result> = done.toList()

    val reachableThreadLocal = LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight)

//    // 実際に置けるミノに絞る (build up)
//    println("filter setup / build")
//    results = results.asSequence().filter { result ->
//        val operationWithKeys = result.toOperationStream().collect(Collectors.toList())
//        val reachable = reachableThreadLocal.get()
//        val stream = BuildUpStream(reachable, maxHeight)
//        stream.existsValidBuildPattern(initField, operationWithKeys as List<MinoOperationWithKey>).findAny().isPresent
//    }.toList()

    // Tスピン出来るものだけ表示（準備）
    val candidateThreadLocal = RotateCandidateThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight)
    val diff = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

    fun isBlock(field: Field, x: Int, y: Int): Boolean {
        if (x < 0 || 10 <= x || y < 0) {
            return true
        }
        return !field.isEmpty(x, y)
    }

    val initColoredField = ArrayColoredField(24)
    (0..(maxHeight - 1)).forEach { y ->
        (0..9).forEach { x ->
            if (!initField.isEmpty(x, y)) {
                initColoredField.setColorType(ColorType.Gray, x, y)
            }
        }
    }

    // Tスピン出来るものだけ表示（出力）
    println("spin-able")
    val myFile = MyFile("output/main.test")
    myFile.newBufferedWriter().use { writer ->
        results.forEach { result ->
            val rotateCandidate = candidateThreadLocal.get()

            // Tを置く前に一度ラインを消去する
            val field = result.field.freeze(maxHeight)
            val clearedLine = field.clearLine()

            val piece = Piece.T

            val nexts = rotateCandidate.search(field, piece, maxHeight - clearedLine)

            // 回転動作で終わるアクションのうち、Spinの条件を満たしている物を抽出
            val spinActions = nexts.asSequence().filter { action ->
                val count = diff.asSequence()
                        .map { (dx, dy) -> isBlock(field, action.x + dx, action.y + dy) }
                        .count { it }
                3 <= count
            }.toList()

            spinActions
                    .filter {
                        // Tをおいた後にライン消去が発生するもの
                        val freeze = field.freeze(maxHeight)
                        freeze.put(minoFactory.create(piece, it.rotate), it.x, it.y)
                        freeze.clearLine() != 0
                    }
                    .forEach { action ->
                        val lastOperation = SimpleOperation(piece, action.rotate, action.x, action.y)

                        val operationWithKeys = result.toOperationStream().collect(Collectors.toList())

//                        // テト譜として出力
//                        val tetfu = Tetfu(minoFactory, colorConverter)
//                        val blockField = OperationTransform.parseToBlockField(operationWithKeys, minoFactory, maxHeight)
//                        val coloredField = initColoredField.freeze(initColoredField.maxHeight)
//                        (0..(blockField.height - 1)).forEach { y ->
//                            (0..9).forEach { x ->
//                                val currentPiece = blockField.getBlock(x, y)
//                                currentPiece?.let { coloredField.setColorType(colorConverter.parseToColorType(it), x, y) }
//                            }
//                        }
//
//                        val colorType = colorConverter.parseToColorType(lastOperation.piece)
//                        val elements = listOf(TetfuElement(coloredField, colorType, lastOperation.rotate, lastOperation.x, lastOperation.y))
//                        val encode = tetfu.encode(elements)
//                        val fumen = "v115@${encode}"
//                        println("http://fumen.zui.jp/?${fumen}")

                        // 結果をファイルに出力
                        val reachable = reachableThreadLocal.get()
                        val stream = BuildUpStream(reachable, maxHeight)
                        val sampleOperationWithKeys = stream.existsValidBuildPattern(initField, operationWithKeys as List<MinoOperationWithKey>).findAny()
                        val operations = OperationTransform.parseToOperations(initField, sampleOperationWithKeys.get(), maxHeight).operations.toMutableList()
                        operations.add(lastOperation)
                        writer.append(OperationInterpreter.parseToString(Operations(operations)))
                        writer.newLine()
                    }
        }
    }
}
*/