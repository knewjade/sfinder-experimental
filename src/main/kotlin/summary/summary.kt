package summary

import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import common.tetfu.Tetfu
import common.tetfu.TetfuPage
import common.tetfu.common.ColorConverter
import common.tetfu.field.ColoredField
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import percent.Index
import util.fig.FigSetting
import util.fig.FrameType
import util.fig.generator.FieldOnlyFigGenerator
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO


// ThA8HeC8PeAgWBAUAAAA
// ThA8HeC8PeAgWBAUAAAA

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()
    val minoShifter = MinoShifter()
    Main(minoFactory, colorConverter, minoShifter).invoke()
}

fun PieceCounter.decode(): String {
    return this.blocks.sorted().map { it.name }.joinToString("").takeIf { it.isNotBlank() } ?: "_"
}

data class Result(val mino: Int, val success: Int, val all: Int, val fieldData: String) {
    val percent = success.toDouble() / all
    val message = String.format("%.3f %% (%d/%d)", success.toDouble() / all * 100, success, all)
}

data class Element(val pieceCounter: PieceCounter, val result: Result) {
    val pieces = pieceCounter.decode()
    val fieldData = result.fieldData.replace("/", "_")
    val figureName = "${fieldData}_$pieces"
    val figurePath = String.format("output/img/$figureName.png")
}

class Main(val minoFactory: MinoFactory, val colorConverter: ColorConverter, val minoShifter: MinoShifter) {
    fun invoke() {
        val index = Index(minoFactory, minoShifter, Paths.get("input/index.csv"))

        val queue = LinkedList<Triple<Int, PieceCounter, String>>()
        queue.addFirst(Triple(1, PieceCounter(), "vhAAgWBAUAAAA"))

        while (queue.isNotEmpty()) {
            val pop = queue.removeFirst()
            println("$pop ${queue.size}")
            val (cycle, pieceCounter, fieldData) = pop

            val start = page(fieldData)
            val initField = start.field
            val maxClearLine = start.comment.toInt()

            val elements = mutableMapOf<Piece, MutableList<Element>>()
            Piece.values().forEach {
                val newPieceCounter = pieceCounter.addAndReturnNew(listOf(it))
                get(cycle, newPieceCounter, fieldData).forEach { result ->
                    val operationWithKey = index.get(result.mino)!!

                    val element = Element(newPieceCounter, result)

                    fig(element.figurePath, initField, maxClearLine, operationWithKey)

                    val elementList = elements.computeIfAbsent(operationWithKey.piece, { mutableListOf() })
                    elementList.add(element)

                    if (0.95 <= result.percent)
                        queue.addLast(Triple(cycle, newPieceCounter, result.fieldData))
                }
            }

            if (elements.isNotEmpty()) {
                val buffer = StringBuffer()
                buffer.append("<!doctype html>")
                buffer.append("<html>")
                buffer.append("<head>")
                buffer.append("<meta charset=\"UTF-8\">")
                buffer.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"../../main.css\">")
                buffer.append("<script src=\"../../main.js\"></script>")
                buffer.append("<title>$cycle/${pieceCounter.decode()}/$fieldData</title>")
                buffer.append("</head>")
                buffer.append("<body onload='init()'>")
                elements.entries.forEach { entry ->
                    val name = entry.key.name
                    buffer.append("<h2 name='$name'>$name</h2>")
                    buffer.append("<section id='$name' class='piece'>")
                    entry.value.forEach {
                        buffer.append("<article>")
                        buffer.append("<img src='../../img/${it.figureName}.png'>")
                        if (0.95 <= it.result.percent) {
                            buffer.append("<a href='../${it.pieces}/${it.fieldData.replace("/", "_")}.html'>")
                            buffer.append(it.result.message)
                            buffer.append("</a>")
                        } else {
                            buffer.append(it.result.message)
                        }
                        buffer.append("</article>")
                    }
                    buffer.append("</section>")
                }
                buffer.append("</body>")
                buffer.append("</html>")

                val dir = "output/$cycle/${pieceCounter.decode()}"
                Files.createDirectories(Paths.get(dir))
                File("$dir/${fieldData.replace("/", "_")}.html").writeText(buffer.toString(), StandardCharsets.UTF_8)
            }
        }
    }

    fun get(cycle: Int, pieceCounter: PieceCounter, fieldData: String): List<Result> {
        val minos = pieceCounter.blocks.sorted().map { it.name }.joinToString("")
        val text = try {
            File("input/fumen/$cycle/$minos/${fieldData.replace("/", "_")}").readText()
        } catch (e: FileNotFoundException) {
            "0"
        }
        return text.takeUnless { !it.contains('?') || it.endsWith("?") }?.let {
            val indexOf = it.indexOf("?")
            val allCounter = it.substring(0, indexOf).toInt()
            it.substring(indexOf + 1).split(";").map { result ->
                val split = result.split(",")
                val (mino, success, data) = split
                Result(mino.toInt(), success.toInt(), allCounter, data)
            }
        } ?: emptyList()
    }

    fun page(data: String): TetfuPage {
        val tetfu = Tetfu(minoFactory, colorConverter)
        val decode = tetfu.decode(data)
        return decode.first()!!
    }

    fun fig(path: String, initField: ColoredField, maxClearLine: Int, operationWithKey: MinoOperationWithKey) {
        val figSetting = FigSetting(FrameType.NoFrame, maxClearLine, 0);
        val generator = FieldOnlyFigGenerator(figSetting, minoFactory, colorConverter)
        generator.reset()

        generator.updateField(initField, null, operationWithKey.x, operationWithKey.y)
        generator.updateMino(colorConverter.parseToColorType(operationWithKey.piece), operationWithKey.rotate, operationWithKey.x, operationWithKey.y)

        // 画像の生成
        val image = generator.fix()

        // 画像の出力
        ImageIO.write(image, "png", File(path))
    }
}

private fun tag(tag: String, func: () -> String): String {
    val str = func()
    return "<$tag>$str</$tag>"
}