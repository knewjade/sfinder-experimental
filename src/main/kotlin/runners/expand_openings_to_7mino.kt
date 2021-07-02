import common.buildup.BuildUp
import common.buildup.BuildUpStream
import common.datastore.BlockField
import common.datastore.PieceCounter
import common.datastore.blocks.LongPieces
import common.iterable.PermutationIterable
import common.parser.OperationInterpreter
import common.parser.OperationTransform
import components.LockedReachableExpanderThreadLocal
import components.MinoOperationWithKeysList
import concurrent.LockedReachableThreadLocal
import core.action.reachable.LockedReachable
import core.field.Field
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import entry.path.output.MyFile
import functions.getTSpin
import searcher.spins.spin.TSpins
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

// 開幕TSS(7ミノとは限らない)を、7ミノの地形に拡張する
fun main() {
    ExpandOpeningsTo7Mino().run(true)
}

class ExpandOpeningsTo7Mino {
    fun run(holdT: Boolean) {
        val height = 12
        val initField = FieldFactory.createField(height)

        // 503634 - 272(TSD) = 503362
        val operationsList = loadOperationsList("resources/tspins.txt", initField, height)
            .filter { it.field(initField, height).clearLine() == 1 }  // TSSのみを抽出
        println(operationsList.size)

        val expanderThreadLocal = LockedReachableExpanderThreadLocal.create(height)
        val allPieceCounter = PieceCounter(Piece.valueList())

        val spinChecker = createLockedReachableSpinChecker(height)

        val expanded = operationsList
            .filter { operations ->
                // TスピンがRegular
                val spin = getTSpin(initField, operations, spinChecker, height)
                spin != null && spin.spin == TSpins.Regular
            }
            .flatMap { operations ->
                val expander = expanderThreadLocal.get()
                val unusedPieces = allPieceCounter.removeAndReturnNew(operations.usedPieceCounter())
                expander.moveWithTSpin(initField, operations, unusedPieces)
            }
        println(expanded.size)  // 349582

        // INFO: expandedには同じ手順が重複している可能性あり

        val results = if (holdT) {
            // Tミノを除いた地形で、2巡目のすべてのツモのTSTをカバーできている
            findThatAllSequencesAreCoveredByTSTInGroup(expanded, height)
        } else {
            // Tミノを後回しにできない地形（Tでライン消去しないと置けない手順や、Tの上に置く必要があるなど）を含んでいる可能性があるので取り除く
            val minoFactory = MinoFactory()
            val minoShifter = MinoShifter()
            val minoRotation = MinoRotation.create()
            val lockedReachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)
            val checked = expanded.filter { canBuildAndGetTSpin(it, lockedReachable, initField, height) }

            // 1地形で、2巡目のすべてのツモのTSTをカバーできている
            findThatAllSequencesAreCoveredByTST(checked, height)
        }

        val lines = results
            .map { OperationTransform.parseToOperations(initField, it.operationWithKeys, height) }
            .map { OperationInterpreter.parseToString(it) }
        MyFile("tss_tst100p_${holdT}.txt").newAsyncWriter().use { it.writeAndNewLine(lines) }
    }

    private fun loadOperationsList(
        fileName: String, initField: Field, height: Int
    ): List<MinoOperationWithKeysList> {
        val minoFactory = MinoFactory()
        return loadLines(fileName)
            .map { OperationInterpreter.parseToOperations(it) }
            .map { operations ->
                OperationTransform.parseToOperationWithKeys(initField, operations, minoFactory, height)
            }
            .map { MinoOperationWithKeysList(it) }
    }

    private fun loadLines(fileName: String): List<String> {
        return Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8).use { reader ->
            reader.lines().filter { it.isNotBlank() }.collect(Collectors.toList())
        }
    }

    fun canBuildAndGetTSpin(
        minoOperationWithKeysList: MinoOperationWithKeysList, lockedReachable: LockedReachable,
        initField: Field, height: Int
    ): Boolean {
        val withoutT = minoOperationWithKeysList.operationWithKeys.filter { it.piece != Piece.T }
        if (!BuildUp.existsValidBuildPatternWithoutKey(initField, withoutT, height, lockedReachable)) {
            return false
        }

        val field = MinoOperationWithKeysList(withoutT).field(initField, height)
        val t = minoOperationWithKeysList.operationWithKeys.find { it.piece == Piece.T } ?: error("Not found T")

        if (t.needDeletedKey != 0L) {
            error("Unexpected T")
        }

        return lockedReachable.checks(field, t.mino, t.x, t.y, height)
    }

    // 1地形で、2巡目のすべてのツモのTSTをカバーできている
    private fun findThatAllSequencesAreCoveredByTST(
        expanded: List<MinoOperationWithKeysList>, height: Int
    ): List<MinoOperationWithKeysList> {
        val progress = ProgressEstimator.start(expanded.size)

        val pieces = Piece.valueList().filter { it != Piece.T }
        val piecesList = PermutationIterable(pieces, pieces.size).toList()
        val piecesMap = piecesList.mapIndexed { index, item -> LongPieces(item) to index }.toMap()

        val finderThreadLocal = TSpinFinderThreadLocal.create(3, height)

        val minoFactory = MinoFactory()
        val lockedReachableThreadLocal = LockedReachableThreadLocal(height)

        return expanded.parallelStream()
            .filter { expandedOperationWithKeys ->
                val emptyField = FieldFactory.createField(height)
                val initField = expandedOperationWithKeys.field(emptyField, height)
                initField.clearLine()

                val success = mutableListOf<Boolean>()
                piecesList.indices.forEach { _ -> success.add(false) }

                val finder = finderThreadLocal.get()

                for (index in success.indices) {
                    // すでに成功しているツモであるか
                    if (success[index]) {
                        continue
                    }

                    // ツモ順をホールドなしで、Tスピンの手順を求める
                    val piecesWithoutT = piecesList[index]
                    val results = finder.searchWithoutHold(initField, piecesWithoutT)
                    if (results.isEmpty()) {
                        // println("No solution for $piecesWithoutT")
                        break
                    }

                    // みつかったTスピンの手順をもとに、カバーできるツモ順に展開する
                    results.forEach { firstOperationList ->
                        val operationWithKeys = OperationTransform.parseToOperationWithKeys(
                            initField, firstOperationList, minoFactory, height
                        )
                        val lockedReachable = lockedReachableThreadLocal.get()
                        BuildUpStream(lockedReachable, height).existsValidBuildPattern(initField, operationWithKeys)
                            .forEach { secondOperationList ->
                                val secondPieces = secondOperationList.map { it.piece }
                                if (secondPieces.size != 6) error("Unexpected second pieces size")
                                val longPieces = LongPieces(secondPieces)
                                val piecesIndex = piecesMap[longPieces] ?: error("Not found sequence")
                                success[piecesIndex] = true
                            }
                    }
                }

                progress.increment()
                success.all { it }
            }
            .collect(Collectors.toList())
    }

    // Tミノを除いた地形で、2巡目のすべてのツモのTSTをカバーできている
    fun findThatAllSequencesAreCoveredByTSTInGroup(
        expanded: List<MinoOperationWithKeysList>, height: Int
    ): List<MinoOperationWithKeysList> {
        val grouping = expanded
            .groupBy { minoOperationWithKeysList ->
                val blockField = BlockField(height)
                minoOperationWithKeysList.operationWithKeys.filter { it.piece != Piece.T }.forEach {
                    blockField.merge(it.createMinoField(height), it.piece)
                }
                blockField
            }
            .values

        println(grouping.size)  // 227728

        val progress = ProgressEstimator.start(grouping.size)

        val pieces = Piece.valueList().filter { it != Piece.T }
        val piecesList = PermutationIterable(pieces, pieces.size).toList()
        val piecesMap = piecesList.mapIndexed { index, item -> LongPieces(item) to index }.toMap()

        val finderThreadLocal = TSpinFinderThreadLocal.create(3, height)

        val minoFactory = MinoFactory()
        val lockedReachableThreadLocal = LockedReachableThreadLocal(height)

        return grouping.parallelStream()
            .filter { group ->
                val emptyField = FieldFactory.createField(height)

                val success = mutableListOf<Boolean>()
                piecesList.indices.forEach { _ -> success.add(false) }

                val finder = finderThreadLocal.get()

                for (index in success.indices) {
                    // すでに成功しているツモであるか
                    if (success[index]) {
                        continue
                    }

                    val piecesWithoutT = piecesList[index]

                    for (expandedOperationWithKeys in group) {
                        val initField = expandedOperationWithKeys.field(emptyField, height)
                        initField.clearLine()

                        // ツモ順をホールドなしで、Tスピンの手順を求める
                        val results = finder.searchWithoutHold(initField, piecesWithoutT)
                        if (results.isEmpty()) {
                            // println("No solution for $piecesWithoutT")
                            continue
                        }

                        success[index] = true

                        // みつかったTスピンの手順をもとに、カバーできるツモ順に展開する
                        results.forEach { firstOperationList ->
                            val operationWithKeys = OperationTransform.parseToOperationWithKeys(
                                initField, firstOperationList, minoFactory, height
                            )
                            val lockedReachable = lockedReachableThreadLocal.get()
                            BuildUpStream(lockedReachable, height).existsValidBuildPattern(initField, operationWithKeys)
                                .forEach { secondOperationList ->
                                    val secondPieces = secondOperationList.map { it.piece }
                                    if (secondPieces.size != 6) error("Unexpected second pieces size")
                                    val longPieces = LongPieces(secondPieces)
                                    val piecesIndex = piecesMap[longPieces] ?: error("Not found sequence")
                                    success[piecesIndex] = true
                                }
                        }

                        // このツモの探索は終了できる
                        break
                    }

                    // 最後まで成功できなかったら、全体の探索を終了する
                    if (!success[index]) {
                        break
                    }
                }

                progress.increment()
                success.all { it }
            }
            .flatMap { it.stream() }
            .collect(Collectors.toList())
    }
}