package summary

import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import common.tetfu.Tetfu
import common.tetfu.TetfuPage
import common.tetfu.field.ColoredField
import core.mino.Piece
import main.caller.ContentCaller
import main.domain.*
import main.percent.Index
import util.fig.FigSetting
import util.fig.FrameType
import util.fig.generator.FieldOnlyFigGenerator
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO


// ThA8HeC8PeAgWBAUAAAA
// ThA8HeC8PeAgWBAUAAAA

fun main(args: Array<String>) {
    val factories = createFactories()
    val index = Index(factories.minoFactory, factories.minoShifter, Paths.get("input/index.csv"))
    Main(factories, index).invoke(Cycle(8))
}

class Main(val factories: Factories, val index: Index) {
    val queue: LinkedList<Task> = LinkedList<Task>().apply {
        addFirst(Task(PieceCounter(), FieldData("vhAAgWBAUAAAA")))
    }

    fun invoke(cycle: Cycle) {
        File("output/img").mkdirs()

        while (queue.isNotEmpty()) {
            val task = queue.removeFirst()
            println("$task / ${queue.size}")

            val elements = onePage(cycle, task)

            elements.takeIf { it.isNotEmpty() }?.let {
                val minos = task.minos.takeIf { it.isNotBlank() } ?: "first"
                val path = "${cycle.number}/$minos/v115${task.fieldData.raw}";

                val buffer = StringBuffer()
                buffer.append("<!doctype html>")
                buffer.append("<html>")
                buffer.append("<head>")
                buffer.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,minimum-scale=1.0\">")
                buffer.append("<meta charset=\"UTF-8\">")
                buffer.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"../../main.css\">")
                buffer.append("<script src=\"../../main.js\"></script>")
                buffer.append("<title>$path</title>")
                buffer.append("</head>")
                buffer.append("<body onload='init()'>")

                val section = section(task, it)
                buffer.append(section)

                buffer.append("</body>")
                buffer.append("</html>")

                val file = File("output/$path.html")
                Files.createDirectories(file.parentFile.toPath())
                file.writeText(buffer.toString(), StandardCharsets.UTF_8)
            }
        }
    }

    private fun onePage(cycle: Cycle, task: Task): Map<Piece, List<Element>> {
        val start = page(task.fieldData)
        val initField = start.field
        val maxClearLine = start.comment.toInt()

        val fieldFigFile = File("output/img/${task.figFilePath}")
        fieldFigFile.takeUnless { it.exists() }?.let {
            fig(it, initField, maxClearLine, null)
        }

        val elements = mutableMapOf<Piece, MutableList<Element>>()
        Piece.values().forEach {
            val results = loadResults(cycle, task, it)
            val newPieceCounter = task.pieceCounter.addAndReturnNew(listOf(it))
            results.details.forEach { result ->
                val minoIndex = result.mino
                val fieldData = result.fieldData

                val operationWithKey = index.get(minoIndex)!!

                val figFilePath = String.format("%s_%s.png", minoIndex.index, fieldData.representation)
                val figFile = File("output/img/$figFilePath")
                figFile.takeUnless { it.exists() }?.let {
                    fig(it, initField, maxClearLine, operationWithKey)
                }

                val htmlFilePath = "${newPieceCounter.minos()}/v115${fieldData.representation}.html"

                val element = Element(it, result, figFilePath, htmlFilePath, results.allCount)

                val piece = operationWithKey.piece
                val elementList = elements.computeIfAbsent(piece, { mutableListOf() })
                elementList.add(element)

                if (0.95 <= element.successPercent) {
                    val newTask = Task(newPieceCounter, fieldData)
                    queue.add(newTask)
                }
            }
        }

        return elements
    }

    private fun page(data: FieldData): TetfuPage {
        val tetfu = Tetfu(factories.minoFactory, factories.colorConverter)
        val decode = tetfu.decode(data.raw)
        return decode.first()!!
    }

    private fun loadResults(cycle: Cycle, task: Task, current: Piece): Results {
        val path = String.format("input/fumens/%d/%s/%s", cycle.number, task.minos + current.name, task.fieldData.representation)
        val content = File(path).readText()

        val caller = ContentCaller(content)
        return caller.call()
    }

    private fun fig(file: File, initField: ColoredField, maxClearLine: Int, operationWithKey: MinoOperationWithKey?) {
        val minoFactory = factories.minoFactory
        val colorConverter = factories.colorConverter

        val figSetting = FigSetting(FrameType.NoFrame, maxClearLine, 0);
        val generator = FieldOnlyFigGenerator(figSetting, minoFactory, colorConverter)
        generator.reset()

        generator.updateField(initField, null, 0, 0)

        operationWithKey?.let { it ->
            val colorType = colorConverter.parseToColorType(it.piece)
            generator.updateMino(colorType, it.rotate, it.x, it.y)
        }

        // 画像の生成
        val image = generator.fix()

        // 画像の出力
        ImageIO.write(image, "png", file)
    }

    private fun section(task: Task, map: Map<Piece, List<Element>>): String {
        val buffer = StringBuffer()

        buffer.append("<img id='current' src='../../img/${task.figFilePath}'>")

        map.entries.forEach { entry ->
            val name = entry.key.name
            val elements = entry.value

            buffer.append("<h2 name='$name'>$name</h2>")
            buffer.append("<section id='$name' class='piece' style='display: none;'>")

            elements.sortedBy { it.successPercent }.asReversed()
                    .forEach { element ->
                        buffer.append("<article>")
                        buffer.append("<img src='../../img/${element.figFilePath}'>")

                        val message1 = String.format("%.3f %%", element.successPercent * 100)
                        val message2 = String.format("(%d/%d)", element.result.success.value, element.allCount.value)

                        if (0.95 <= element.successPercent) {
                            buffer.append("<a href='../${element.htmlFilePath}'>")
                            buffer.append("<span id=\"p\">$message1</span>")
                            buffer.append("<span id=\"c\">$message2</span>")
                            buffer.append("</a>")
                        } else {
                            buffer.append("<span id=\"p\">$message1</span>")
                            buffer.append("<span id=\"c\">$message2</span>")
                        }

                        buffer.append("</article>")
                    }

            buffer.append("</section>")
        }

        return buffer.toString()
    }
}

fun PieceCounter.minos(): String {
    return blocks.sorted().map { it.name }.joinToString("")
}


data class Task(val pieceCounter: PieceCounter, val fieldData: FieldData) {
    val minos = pieceCounter.minos()
    val figFilePath = String.format("none_%s.png", fieldData.representation)
}

data class Element(
        val current: Piece,
        val result: Result,
        val figFilePath: String,
        val htmlFilePath: String,
        val allCount: Counter
) {
    val successPercent = result.success.value.toDouble() / allCount.value
}