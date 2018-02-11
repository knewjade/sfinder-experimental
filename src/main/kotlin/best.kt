import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import common.iterable.PermutationIterable
import common.order.ForwardOrderLookUp
import common.pattern.LoadedPatternGenerator
import common.tetfu.Tetfu
import common.tetfu.common.ColorConverter
import common.tetfu.field.ColoredFieldView
import core.field.Field
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import entry.path.LockedBuildUpListUpThreadLocal
import helper.EasyTetfu
import helper.Patterns
import main.caller.ContentCaller
import main.domain.Counter
import main.domain.Result
import searcher.pack.InOutPairField
import searcher.pack.SeparableMinos
import searcher.pack.SizedBit
import searcher.pack.calculator.BasicSolutions
import searcher.pack.memento.AllPassedSolutionFilter
import searcher.pack.solutions.OnDemandBasicSolutions
import searcher.pack.task.BasicMinoPackingHelper
import searcher.pack.task.SetupPackSearcher
import util.fig.Bag
import util.fig.FigSetting
import util.fig.FrameType
import util.fig.generator.FieldOnlyFigGenerator
import util.fig.output.PngWriter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()

    // Initialize
    val minoShifter = MinoShifter()
    val sizedBit = SizedBit(3, 4)
    val taskResultHelper = BasicMinoPackingHelper()
    val solutionFilter = AllPassedSolutionFilter()
    val buildUpStreamThreadLocal = LockedBuildUpListUpThreadLocal(4)

    val file = File("output/test.html")
    file.writeText("")
    file.appendText("<!doctype html>")
    file.appendText("<html><head>")
    file.appendText("<meta charset=\"UTF-8\">")
    file.appendText("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,minimum-scale=1.0\">")
    file.appendText("<link rel=\"stylesheet\" type=\"text/css\" href=\"./css/main.css\">")
    file.appendText("</head><body>")

    (1..1).map { Counter(it) }.forEach { count ->
        println(count)
        val patterns = Patterns.hold(count.value)
        val generator = LoadedPatternGenerator(patterns)
        val lookUp = ForwardOrderLookUp(4, true)

        val set = generator.blocksStream()
                .map { it.pieces.subList(0, 5) }
                .flatMap { lookUp.parse(it) }
                .map { PieceCounter(it) }
                .collect(Collectors.toSet())

        println(set.size)

        file.appendText(String.format("<section id='count%d'>", count.value))
        file.appendText(String.format("<span>%d 回目</span>", count.value))

        set.forEach { pieceCounter ->
            val bestPair = best(pieceCounter, count)

            bestPair?.let {
                val (bestResult, allCount) = it
                val pages = Tetfu(minoFactory, colorConverter).decode(bestResult.fieldData.raw)
                val page = pages[0]

                val fieldData = ColoredFieldView.toString(page.field, 4, "").replace('0', ' ')
                val field = FieldFactory.createField(fieldData)

                // ミノリストの作成
                val notFilledField = field.freeze(4)
                notFilledField.inverse()
                val deleteKeyMask = getDeleteKeyMask(notFilledField, 4)
                val separableMinos = SeparableMinos.createSeparableMinos(minoFactory, minoShifter, sizedBit, deleteKeyMask)

                // 絶対に置かないブロック
                val inOutPairFields = InOutPairField.createInOutPairFields(sizedBit, notFilledField)

                // 絶対に置く必要があるブロック
                val basicSolutions = ArrayList<BasicSolutions>()
                val needFillFields = InOutPairField.createInnerFields(sizedBit, field)
                for (innerField in needFillFields) {
                    val solutions = OnDemandBasicSolutions(separableMinos, sizedBit, innerField.getBoard(0))
                    basicSolutions.add(solutions)
                }

                // 探索
                val searcher = SetupPackSearcher(inOutPairFields, basicSolutions, sizedBit, solutionFilter, taskResultHelper, needFillFields)
                val result = getResult(FieldFactory.createField(4), sizedBit, buildUpStreamThreadLocal, searcher, pieceCounter)

                val operations = result!!.memento.getOperationsStream(sizedBit.width).collect(Collectors.toList())
                val encodeUrl = EasyTetfu().encodeUrl(FieldFactory.createField(4), operations, 4)

                val pagesColor = Tetfu(minoFactory, colorConverter).decode(encodeUrl.removePrefix("http://fumen.zui.jp/?v115@"))
                val pageColor = pagesColor[0]

                val setting = FigSetting(FrameType.NoFrame, 4, 0)
                val name = pieceCounter.blocks.joinToString("") { it.name }
                val prefix = String.format("output/img/%s", name)
                File(prefix).parentFile.mkdirs()
                val writer = PngWriter(minoFactory, colorConverter, FieldOnlyFigGenerator(setting, minoFactory, colorConverter), Bag.EMPTY, 0, prefix, count.value - 1)
                writer.write(listOf(pageColor))

                file.appendText("<article>")
                file.appendText(String.format("<span id=\"m\">%s</span>", name))
                file.appendText(String.format("<img src='./img/%s_%03d.png'>", name, count.value))
                file.appendText(String.format("<span id=\"p\">%.3f %%</span>", bestResult.success.value * 100.0 / allCount))
                file.appendText(String.format("<span id=\"c\">(%d/%d)</span>", bestResult.success.value, allCount))
                file.appendText("</a></article>")
            }
        }

        file.appendText("</section>")
    }

    file.appendText("</body></html>")
}

fun getResult(initField: Field, sizedBit: SizedBit, buildUpStreamThreadLocal: LockedBuildUpListUpThreadLocal, searcher: SetupPackSearcher, pieceCounter: PieceCounter): searcher.pack.task.Result? {
    val results = searcher.toList()
    val result = results.find { result ->
        val collector = Collectors.toCollection(Supplier<LinkedList<MinoOperationWithKey>> { LinkedList() })
        val operationWithKeys: LinkedList<MinoOperationWithKey> = result.getMemento()
                .getSeparableMinoStream(sizedBit.width)
                .map { it.toMinoOperationWithKey() }
                .collect(collector);

        val usingPiece = PieceCounter(operationWithKeys.map { it.mino.piece })
        if (!pieceCounter.equals(usingPiece))
            return@find false

        // 地形の中で組むことができるoperationsを一つ作成
        val buildUpStream = buildUpStreamThreadLocal.get()
        val sampleOperations = buildUpStream.existsValidBuildPatternDirectly(initField, operationWithKeys)
                .findFirst()
                .orElse(emptyList())

        // 地形の中で組むことができるものがないときはスキップ
        !sampleOperations.isEmpty()
    }

    return result
}

fun best(pieceCounter: PieceCounter, counter: Counter): Pair<Result, Int>? {
//    println("-----")
//    println(pieceCounter.blocks.joinToString(""))

    val blocks = pieceCounter.blocks
    val iterable = PermutationIterable(blocks, blocks.size)
    var max = -1

    val bestResult = iterable.map {
        val file = File("input/fumens/" + counter.value + "/" + it.joinToString("") { it.name })
        if (file.isDirectory) {
            val results = file.listFiles().map {
                val content = it.readText(StandardCharsets.UTF_8)
                val caller = ContentCaller(content)
                caller.call()
            }

            // verify
            assert(results.map { it.allCount }.toSet().size == 1, { results })
            max = results[0].allCount.value

            // max in directory
            results.flatMap { it.details }.maxBy { it.success.value }
        } else {
            null
        }
    }.filterNotNull().maxBy { it.success.value }

    return bestResult?.let { it to max }
}

private fun getDeleteKeyMask(notFilledField: Field, maxHeight: Int): Long {
    val freeze = notFilledField.freeze(maxHeight)
    freeze.inverse()
    return freeze.clearLineReturnKey()
}