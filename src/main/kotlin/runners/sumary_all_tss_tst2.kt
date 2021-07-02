import common.cover.AnyTSpinCover
import common.cover.reachable.ReachableForCoverWrapper
import common.datastore.MinoOperation
import common.datastore.Operations
import common.datastore.PieceCounter
import common.iterable.PermutationIterable
import common.parser.OperationInterpreter
import common.parser.OperationTransform
import common.tetfu.Tetfu
import common.tetfu.common.ColorConverter
import common.tetfu.field.ColoredField
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
import exceptions.FinderInitializeException
import functions.blockFieldToColoredField
import functions.blockFieldToTetfuElement
import util.fig.Bag
import util.fig.FigColors
import util.fig.FigSetting
import util.fig.FrameType
import util.fig.generator.FieldOnlyFigGenerator
import util.fig.generator.FigGenerator
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import javax.imageio.ImageIO

// TSS->TST一覧をテト譜にまとめる。TSTは7ミノ使用されているとは限らないので7ミノに拡張します。
fun main() {
    SummaryAllTssTST2().run()
}

class SummaryAllTssTST2 {
    fun run() {
        val height = 24
        val initField = FieldFactory.createField(height)

        val headSecond = loadTssTst("resources/search_all_tst.txt")
        val head = headSecond.first
        val second = headSecond.second

        println(head.size)

        val minoFactory = MinoFactory()
        val minoShifter = MinoShifter()
        val minoRotation = MinoRotation.create()
        val colorConverter = ColorConverter()

        val expanderThreadLocal = LockedReachableExpanderThreadLocal.create(height)
        val allPieceCounter = PieceCounter(Piece.valueList())

        val anyTSpinCover = AnyTSpinCover(false)

        val runner = ExpandOpeningsTo7Mino()
        val lockedReachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)

        MyFile("output/tsstst.html").newAsyncWriter().use { writer ->
            val progress = ProgressEstimator.start(head.size)
            var sum = 0

            writer.writeAndNewLine("""<html><head><link rel="stylesheet" href="tsstst.css"></head><body>""")

            head.zip(second)
                .distinctBy { pair ->
                    val minoOperationWithKeysList = MinoOperationWithKeysList(
                        OperationTransform.parseToOperationWithKeys(initField, pair.first, minoFactory, height)
                    )
                    minoOperationWithKeysList.blockField(height)
                }
                .sortedByDescending { pair ->
                    val minoOperationWithKeysList = MinoOperationWithKeysList(
                        OperationTransform.parseToOperationWithKeys(initField, pair.first, minoFactory, height)
                    )

                    val count = countSetUpTSpinInOpening(
                        lockedReachable, anyTSpinCover, initField, minoOperationWithKeysList, height
                    )

                    val t =
                        minoOperationWithKeysList.operationWithKeys.find { it.piece == Piece.T } ?: error("Not found T")
                    count * 100 + (height - t.y - 1) * 10 + (10 - t.x - 1)
                }
                .forEachIndexed { firstIndex, pair ->
                    // 1st bag
                    val first = MinoOperationWithKeysList(
                        OperationTransform.parseToOperationWithKeys(initField, pair.first, minoFactory, height)
                    )

                    val firstField = first.field(initField, height)
                    firstField.clearLine()
                    val firstBlockField = first.blockField(height)
                    val firstColoredField = blockFieldToColoredField(initField, colorConverter, firstBlockField)

                    // 2nd bag
                    val operations = run {
                        val field = first.field(initField, height)
                        field.clearLine()

                        pair.second
                            .map { OperationTransform.parseToOperationWithKeys(field, it, minoFactory, height) }
                            .map { MinoOperationWithKeysList(it) }
                            .flatMap { operationsWithKeysList ->
                                val expander = expanderThreadLocal.get()
                                val unusedPieces =
                                    allPieceCounter.removeAndReturnNew(operationsWithKeysList.usedPieceCounter())
                                expander.moveWithTSpin(field, operationsWithKeysList, unusedPieces)
                            }
                            .filter { runner.canBuildAndGetTSpin(it, lockedReachable, firstField, height) }
                    }

                    // 画像の出力
                    val imagePath = "img/${firstIndex}.png"
                    val figGenerator = createFigGenerator(minoFactory, colorConverter, firstColoredField.usingHeight)
                    write(
                        figGenerator, colorConverter, firstColoredField, null, "output/$imagePath", null
                    )

                    // 譜面に変換
                    val fumens = operations
                        .sortedByDescending { minoOperationWithKeysList ->
                            val count = countSetUpTSpinInOpening(
                                lockedReachable, anyTSpinCover, initField, minoOperationWithKeysList, height
                            )

                            val t = minoOperationWithKeysList.operationWithKeys.find { it.piece == Piece.T }
                                ?: error("Not found T")
                            count * 100 + (height - t.y - 1) * 10 + (10 - t.x - 1)
                        }
                        .map { minoOperationWithKeysList ->
                            val blockField = minoOperationWithKeysList.blockField(height)
                            blockFieldToTetfuElement(firstField, colorConverter, blockField, "")
                        }
                        .chunked(80)
                        .map { chunked ->
                            val data = Tetfu(minoFactory, colorConverter).encode(chunked)
                            "v115@${data}"
                        }

                    // 出力
                    var str = ""
                    str += "<div>"
                    str += "<h4>No.${firstIndex + 1}</h4>"
                    str += """<img border="0" width="200" src="$imagePath">"""
                    str += """<div class="fp">"""
                    fumens.forEachIndexed { index, data ->
                        str += """<div class="fi"><a href="https://fumen.zui.jp/?${data}">fumen${index + 1}</a></div>"""
                    }
                    str += "</div>"
                    str += "</div>"
                    str += "<hr/>"
                    writer.writeAndNewLine(str)
//                    println(operations.size)

                    sum += operations.size

                    progress.increment()
                }

            writer.writeAndNewLine("""</body></html>""")

            println(sum)
        }
    }

    private fun loadTssTst(fileName: String): Pair<List<Operations>, List<List<Operations>>> {
        val head = mutableListOf<Operations>()
        val second = mutableListOf<MutableList<Operations>>()
        loadLines(fileName).forEach { line ->
            val isFirst = line.startsWith("#")
            val str = if (isFirst) line.substring(1) else line
            val operations = OperationInterpreter.parseToOperations(str)

            if (isFirst) {
                head.add(operations)
                second.add(mutableListOf())
            } else {
                second.last().add(operations)
            }
        }

        if (head.size != second.size) error("Illegal state in the list")
        println(head.size)
        if (!(second.all { it.isNotEmpty() })) {
            error("Contains tss that does not has tst")
        }

        return head to second
    }

    private fun loadLines(fileName: String): List<String> {
        return Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8).use { reader ->
            reader.lines().filter { it.isNotBlank() }.collect(Collectors.toList())
        }
    }

    private fun countSetUpTSpinInOpening(
        lockedReachable: LockedReachable,
        anyTSpinCover: AnyTSpinCover,
        initField: Field,
        operationWithKeysList: MinoOperationWithKeysList,
        height: Int
    ): Int {
        val operationWithKeys = operationWithKeysList.operationWithKeys
        val iterable = PermutationIterable(Piece.valueList(), Piece.valueList().size)
        return iterable.count { pieces ->
            val wrapper = ReachableForCoverWrapper(lockedReachable)
            anyTSpinCover.canBuildWithHold(
                initField, operationWithKeys.stream(), pieces, height, wrapper, operationWithKeys.size
            )
        }
    }

    private fun createFigGenerator(
        minoFactory: MinoFactory,
        colorConverter: ColorConverter,
        fieldHeightBlock: Int
    ): FigGenerator {
        val frameType = FrameType.NoFrame
        val nextBoxCount = 5
        val figSetting = FigSetting(frameType, fieldHeightBlock, nextBoxCount)
        val figColors = FigColors(readProperties("default"))
        return FieldOnlyFigGenerator(figSetting, figColors, minoFactory, colorConverter)
    }

    private fun readProperties(name: String): Properties? {
        val colorThemeProperties = Properties()
        val path = String.format("solution-finder/theme/%s.properties", name)
        try {
            Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8).use { reader ->
                colorThemeProperties.load(
                    reader
                )
            }
        } catch (e: NoSuchFileException) {
            throw FinderInitializeException("Not found color theme", e)
        } catch (e: IOException) {
            throw FinderInitializeException("Occur error when read color theme", e)
        }
        return colorThemeProperties
    }

    private fun write(
        figGenerator: FigGenerator, colorConverter: ColorConverter,
        field: ColoredField, minoOperation: MinoOperation?, path: String, bag: Bag?
    ) {
        // リセット
        figGenerator.reset()

        // フィールドの更新
        if (minoOperation != null) {
            figGenerator.updateField(field, minoOperation.mino, minoOperation.x, minoOperation.y)
        } else {
            figGenerator.updateField(field, null, 0, 0)
        }

        bag?.let {
            val nextBoxCount = 5

            // ミノを置くかチェック
            if (minoOperation != null) {
                // 現在のミノの更新
                val colorType = colorConverter.parseToColorType(minoOperation.piece)
                figGenerator.updateMino(colorType, minoOperation.rotate, minoOperation.x, minoOperation.y)

                // bagの更新
                val piece: Piece = colorConverter.parseToBlock(colorType)
                it.use(piece)
            }

            // ネクストの更新
            figGenerator.updateNext(it.getNext(nextBoxCount))

            // ホールドの更新
            figGenerator.updateHold(it.hold)
        }

        // 画像の生成
        val image = figGenerator.fix()

        // 画像の出力
        ImageIO.write(image, "png", File(path))
    }
}