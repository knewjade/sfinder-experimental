import common.datastore.PieceCounter
import common.datastore.action.Action
import common.parser.StringEnumTransform
import common.pattern.LoadedPatternGenerator
import common.tetfu.Tetfu
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tree.AnalyzeTree
import concurrent.LockedCandidateThreadLocal
import concurrent.LockedReachableThreadLocal
import concurrent.checker.CheckerUsingHoldThreadLocal
import concurrent.checker.invoker.CheckerCommonObj
import concurrent.checker.invoker.using_hold.ConcurrentCheckerUsingHoldInvoker
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.Piece
import util.fig.Bag
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors
import java.util.stream.Stream

fun main(args: Array<String>) {
    val maxHeight = 4

    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()
    val executorService = Executors.newFixedThreadPool(4)

    val invoker = createInvoker(minoFactory, executorService, maxHeight)

    // sakana897
//    val fumen = "vh/AgW+BFLDmClcJSAVDEHBEooRBKoAVBviLuCp+TW?CKXNFD0/lFDTX9VCTuKxCs3/wCK9aFDpSltCv3/VC0/dgCz?n9VCJNMgC03PPCUnbMCM9jFDsvKxCpPNFDM3TxCTnPFDU+D?xCpHcgCs/NMCat/VCPePFD0yytC6/AAA2OJTtB0qBvkBSoB?RmBFgBvrBusBasBpoBzsB1uB0qBOkBVmBXgBziB0rBasBxx?B3rBznBJkB6oBtqBUhBeqBFnBupBXqBMpBpoBifBzsBvrBJ?nB0qBGrBzsBVyB6oB2uBzkBXsBtmB0iBifBJmBplBSyBTpB?XsBMoBtmBmqBlnBTkBsqBOqBSyBXsBJkBvhkMoB3iBumBzq?BtpBRlBiqB2uB0qBpoBzsBvkBamBdsBznBurBUqBVyBXsB6?oBJkBtmB0iBRvBTqBarBupBXqBpoBMpB3gBerBifBzrBdtB?GsBRxB"

    // tomokun
//    val fumen = "vh/AgW+BFLDmClcJSAVDEHBEooRBKoAVBveNFDMt/V?CT+dgC6yaFDKuTxCsuntCvn9wCKnLuCMuLMCvujWCqHLxCJ?NUPCMt/wCaX1LCq+KWCaN8LCvvTxC6P9VCPtjxCUX9VCzfr?gCMtzPCUuLMCaHUPC0vaFDUd9VCpCBAA2OJzkBplB0sBifB?9tB3mB2wBXtBxwBSyBTpBMoBNsBSwBMmBulBTfBJlBlsBqs?BvtBGjBXqBNpB0fBRwBllB2wBzrBTpBXsBSyBMoB3iBJlBT?fBplBMrBirB2uB0sBzkB9tBplBXjBifBNrB6rBOsBXqBNpB?0fBWyBJnBTrB+oBxlBznBCrB0sB9tByuBvhkXjBTkBmfBpl?BNrB0mBXsB2uBSwBTlBvkBMmBRgBNsB9tBxnBzsBSwB2uBM?mBTlBvkBSyBMoBOsBlsBRvBXqBpoBMpB3gBifBerBdtBzrB?zmBusB"

    // gongon
//    val fumen = "vh/AgW+BFLDmClcJSAVDEHBEooRBaoAVB0vTWCPuaF?DKuTxCs3/wCPN8LCKNExCaX9wCKeLuCMXNPC0iLuCv/DxCJ?nLuC0HcgCsPFgCpuzPCzSNFDMt/wCvCmFDvvLMCsHztC0PF?gC0i3LCz3ntCMXNFD0HLWCz/NMCT+AAA0sB9tB2uBXjBzkB?ifBplBTrBmrBxwBXsBNrBSyBMoBeqBMpBlhB3iBifBplBTp?BvtBGjBCrB0sBTfBJlBNsBFrBGrBRvBMpBifBTlBvrBGhBN?sBxiBUoBSyBTpB+tB3sBKqBFjBTfBUrBXsBlrB0qBXqBTtB?pnBerBClBTjBJkBRgBdsBurBXsBSyBMoBvhkMrBihB3iB2u?BplBzpBipBNpB0fBzsBOsBXqB9rB6tBxnB0lBXrBTpB2vBp?oBTfB9rBllB2xBxlBvsBatB2uBvkB0qBamBpmBUgBNsBTtB?ToB6sB"

    // nilgiri
    val fumen = "vh/AgW+BFLDmClcJSAVDEHBEooRBJoAVBvvjFDse9V?CqXegCzPltC6+KWCT+dgCqXUFDve9VCzPltCsfrgCUejWC0?vbgCPezPCUNEWCveltCqHDMCsXmPCzOMgC6vLMC6yLMCq+C?MC0iLuCv/rtCvn9wCq+CMCz3/VCzCBAARPJTFJvLJMJJi/I?GBJJHJCNJGNY2BFLDmClcJSAVztSAVG88AYP88AZXegCzPl?tC6+KWCT+dgCqXUFDve9VCzPltCsfrgCUejWC0vbgCPezPC?UNEWCveltCqHDMCsXmPCzOMgC6vLMC6yLMCq+CMC0iLuCv/?rtCvn9wCq+CMCz3/VCzCBAAFNJ1OJ0KJXAJOEJzMJpIJ0LJ?dMJzMJXLJXKJSSJNJJzHJGDJ0/ISQJMGJJHJlKJxRJSNJXD?J2LJxOJzEJ6FJOEJXHJTLJVRJUNJ0CJ6IJ1GJJEJXLJzJJz?EJuKJpIJ2OJ0KJvEJyRJKHJzCJdGJFAJpGJ2OJ0KJvEJvhk?xRJzHJ6IJUAJlLJXMJFLJSSJzJJMIJOJJXMJm/IMLJpFJ3M?JiLJ2OJpFJzEJVRJNLJi/I0BJPNJpIJmMJSSJTJJT/IMIJx?PJdMJuMJNKJ3GJzKJ"

    val tetfu = Tetfu(minoFactory, colorConverter)
    val pages = tetfu.decode(fumen)

    val orders = pages[0].comment.mapNotNull {
        try {
            StringEnumTransform.toPiece(it)
        } catch (e: Exception) {
            null
        }
    }
    val bag = Bag(orders, null)

    val next = 5
    var droppedPiece = PieceCounter()

    val leftPieceCounterInBag = { used: PieceCounter ->
        val max = used.enumMap.values.max() ?: 0
        var pieceCounter = PieceCounter()
        (0 until max).forEach { _ -> pieceCounter = pieceCounter.addAndReturnNew(Piece.valueList()) }
        pieceCounter.removeAndReturnNew(used)
    }

    var maxClearLine = maxHeight
    for (page in pages) {
        // フィールドにおくミノ
        val piece = colorConverter.parseToBlock(page.colorType)

        // これまでにおいたミノ個数
        droppedPiece = droppedPiece.addAndReturnNew(Stream.of(piece).filter { it != null })

        println("####################")
        println(droppedPiece.blockStream.count())
        println("####################")

        // 現在のカラーフィールドの状態（ミノ接着後）
        val freeze = page.field.freeze(maxHeight)
        if (piece != null) {
            freeze.putMino(minoFactory.create(piece, page.rotate), page.x, page.y)
        }

        // 現在のフィールドの状態（ミノ接着後）
        val field = FieldFactory.createField(maxHeight)
        for (y in 0 until maxHeight) {
            for (x in 0 until 10) {
                freeze.getColorType(x, y)
                        ?.takeIf { ColorType.isMinoBlock(it) }
                        ?.let { field.setBlock(x, y) }
            }
        }
        val clearedLine: Int = field.clearLine()
        maxClearLine -= clearedLine
        println(FieldView.toString(field))

        // パフェの開始
        if (field.isPerfect) {
            maxClearLine = maxHeight
        }

        // ネクスト・ホールドの状態
        bag.use(piece)

        // これまでにひいたミノ個数
        val drawnPieces = bag.hold?.let { droppedPiece.addAndReturnNew(Stream.of(it)) } ?: droppedPiece

        // みえているミノ
        val nextPieces = bag.getNext(next)
        val visiblePieces = (listOf(bag.hold) + nextPieces).filterNotNull()

        // ネクストより後ろの残りのバッグのミノ一覧
        val settledPieces = drawnPieces.addAndReturnNew(nextPieces)
        val unsettledPiecesInBag = leftPieceCounterInBag(settledPieces)

        // パフェまでの必要なミノの個数
        val leftCount = 10 - droppedPiece.blockStream.count().toInt() % 10
        val leftCountWithHold = leftCount + 1
        val unsettledPieces = unsettledPiecesInBag.blocks

        // 　パターンの作成
        val createPatternGenerator = {
            if (leftCountWithHold <= visiblePieces.size) {
                // ネクストの範囲で完了する
                val pattern = visiblePieces.subList(0, leftCountWithHold).joinToString("") { it.name }

                println("in next: $pattern")
                LoadedPatternGenerator(pattern)
            } else if (leftCountWithHold - visiblePieces.size <= unsettledPieces.size) {
                // ネクストでみえているバッグ内で完了する
                val visible = visiblePieces.joinToString("") { it.name }
                val selectCountInUnsettled = leftCountWithHold - visiblePieces.size
                val unsettled = unsettledPieces.joinToString("") { it.name }
                val pattern = "$visible,[$unsettled]p$selectCountInUnsettled"

                println("in next bag: $pattern")
                LoadedPatternGenerator(pattern)
            } else if (unsettledPieces.isEmpty()) {
                // ネクストでみえているバッグはすべて確定＋ネクストのバッグ外も必要となる
                val visible = visiblePieces.joinToString("") { it.name }
                val selectCountOutOfBag = leftCountWithHold - visiblePieces.size - unsettledPieces.size
                val pattern = "$visible,*p$selectCountOutOfBag"

                println("out of next bag: $pattern")
                LoadedPatternGenerator(pattern)
            } else {
                // ネクストでみえているバッグ＋ネクストのバッグ外も必要となる
                val visible = visiblePieces.joinToString("") { it.name }
                val unsettled = unsettledPieces.joinToString("") { it.name }
                val selectCountOutOfBag = leftCountWithHold - visiblePieces.size - unsettledPieces.size
                val pattern = "$visible,[$unsettled]!,*p$selectCountOutOfBag"

                println("out of next bag: $pattern")
                LoadedPatternGenerator(pattern)
            }
        }
        val patternGenerator = createPatternGenerator()

        // 実行
        val searchingPieces = patternGenerator.blocksStream().collect(Collectors.toList())
        println("maxClearLine = $maxClearLine")
        val resultPairs = invoker.search(field, searchingPieces, maxClearLine, leftCount)

        // 集計する
        val resultTree = AnalyzeTree()
        for (resultPair in resultPairs) {
            resultTree.set(resultPair.value, resultPair.key)
        }

        println(resultTree.show())
        println()
    }

    executorService?.shutdown()
}

fun createInvoker(minoFactory: MinoFactory, executorService: ExecutorService, maxClearLine: Int = 4): ConcurrentCheckerUsingHoldInvoker {
    val candidateThreadLocal = LockedCandidateThreadLocal(maxClearLine)
    val reachableThreadLocal = LockedReachableThreadLocal(maxClearLine)
    val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
    val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
    return ConcurrentCheckerUsingHoldInvoker(executorService, commonObj)
}