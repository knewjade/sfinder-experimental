import common.cover.AnyTSpinCover
import common.cover.NormalCover
import common.cover.reachable.ReachableForCoverWrapper
import common.datastore.BlockField
import common.iterable.PermutationIterable
import common.parser.OperationInterpreter
import common.parser.OperationTransform
import common.tetfu.Tetfu
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ColoredField
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
import functions.blockFieldToTetfuElement
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

fun main() {
    SummaryAllTssTSTHoldT().run()
}

class SummaryAllTssTSTHoldT {
    /**
     *  Tミノを後回しにできない地形（Tでライン消去しないと置けない手順や、Tの上に置く必要があるなど）を含んでいたので、
     *  それを取り除く専用の処理を実行
     */
//    fun run2() {
//        val height = 12
//        val initField = FieldFactory.createField(height)
//
//        val trueOperations = loadOperationsList("resources/tss_tst100p_true.txt", initField, height)
//        println(trueOperations.size)
//
//        val runner = ExpandOpeningsTo7Mino()
//
//        val minoFactory = MinoFactory()
//        val minoShifter = MinoShifter()
//        val minoRotation = MinoRotation.create()
//        val lockedReachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)
//        val checked = trueOperations.filter { runner.canBuildAndGetTSpin(it, lockedReachable, initField, height) }
//        println(checked.size)
//
//        val results = runner.findThatAllSequencesAreCoveredByTSTInGroup(checked, height)
//        println(results.size)
//
//        val lines = results
//            .map { OperationTransform.parseToOperations(initField, it.operationWithKeys, height) }
//            .map { OperationInterpreter.parseToString(it) }
//        MyFile("tss_tst100p_true2.txt").newAsyncWriter().use { it.writeAndNewLine(lines) }
//    }

    fun run() {
        val height = 12
        val initField = FieldFactory.createField(height)

        val minoFactory = MinoFactory()
        val minoShifter = MinoShifter()
        val minoRotation = MinoRotation.create()

        val lockedReachableThreadLocal = LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, height)
        val normalCover = NormalCover()

        val colorConverter = ColorConverter()

        val trueOperations = loadOperationsList("resources/tss_tst100p_true.txt", initField, height)
        val trueElements = trueOperations
            .map { minoOperationWithKeysList ->
                MinoOperationWithKeysList(minoOperationWithKeysList.operationWithKeys.filter { it.piece != Piece.T })
            }
            .distinctBy { minoOperationWithKeysList ->
                minoOperationWithKeysList.blockField(height)
            }
            .sortedByDescending { minoOperationWithKeysList ->
                val lockedReachable = lockedReachableThreadLocal.get()
                countSetUpTSpinInOpening(
                    lockedReachable, normalCover, initField, minoOperationWithKeysList, height
                )
            }
            .map { minoOperationWithKeysList ->
                val blockField = minoOperationWithKeysList.blockField(height)
                blockFieldToTetfuElement(initField, colorConverter, blockField, "")
            }
        println(trueElements.size)

        val falseOperations = loadOperationsList("resources/tss_tst100p_false.txt", initField, height)
        val falseElements = falseOperations
            .map { minoOperationWithKeysList ->
                MinoOperationWithKeysList(minoOperationWithKeysList.operationWithKeys.filter { it.piece != Piece.T })
            }
            .distinctBy { minoOperationWithKeysList ->
                val blockField = BlockField(24)
                minoOperationWithKeysList.operationWithKeys.forEach {
                    blockField.merge(it.createMinoField(height), it.piece)
                }
                blockField
            }
            .map { minoOperationWithKeysList ->
                val blockField = run {
                    val blockField = BlockField(24)
                    minoOperationWithKeysList.operationWithKeys.forEach {
                        blockField.merge(it.createMinoField(height), it.piece)
                    }
                    blockField
                }

                blockFieldToTetfuElement(initField, colorConverter, blockField, "")
            }
        println(falseElements.size)

        val falseBlockFields = falseElements.map {
            colorFieldToBlockField(colorConverter, it.field.get(), height)
        }

        val trueSolutions = trueElements.groupBy { newPage ->
            val coloredField = newPage.field.get()
            val trueBlockField = colorFieldToBlockField(colorConverter, coloredField, height)
            falseBlockFields.find { it == trueBlockField } == null
        }

        trueSolutions[true]?.let { solutions ->
            println(solutions.size)
            val data = Tetfu(minoFactory, colorConverter).encode(solutions)
            MyFile("fumen_true_new.txt").newAsyncWriter().use { it.writeAndNewLine("v115@${data}") }
        }

        println("###")
        trueSolutions[false]?.chunked(100)?.mapIndexed { index, solutions ->
            println(solutions.size)
            val data = Tetfu(minoFactory, colorConverter).encode(solutions)
            MyFile("fumen_true_already${index}.txt").newAsyncWriter().use { it.writeAndNewLine("v115@${data}") }
        }
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

    private fun countSetUpTSpinInOpening(
        lockedReachable: LockedReachable,
        normalCover: NormalCover,
        initField: Field,
        operationWithKeysList: MinoOperationWithKeysList,
        height: Int
    ): Int {
        val operationWithKeys = operationWithKeysList.operationWithKeys
        val iterable = PermutationIterable(Piece.valueList(), Piece.valueList().size)
        return iterable.count { pieces ->
            val wrapper = ReachableForCoverWrapper(lockedReachable)
            normalCover.canBuildWithHold(
                initField, operationWithKeys.stream(), pieces, height, wrapper, operationWithKeys.size
            )
        }
    }

    private fun colorFieldToBlockField(
        colorConverter: ColorConverter, coloredField: ColoredField, height: Int
    ): BlockField {
        val blockField = BlockField(height)
        for (x in 0 until 10) {
            for (y in 0 until height) {
                val colorType = coloredField.getColorType(x, y)
                if (ColorType.isMinoBlock(colorType)) {
                    val piece = colorConverter.parseToBlock(colorType)
                    blockField.setBlock(piece, x, y)
                }
            }
        }
        return blockField
    }
}