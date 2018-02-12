import best.Obj
import best.colorize
import best.getBest
import best.getPieceCounters
import common.tetfu.Tetfu
import main.domain.Cycle
import util.fig.Bag
import util.fig.FigSetting
import util.fig.FrameType
import util.fig.generator.FieldOnlyFigGenerator
import util.fig.output.PngWriter
import webpage.html
import java.io.File

fun main(args: Array<String>) {
    val obj = Obj()
    val maxCycle = 8

    val html = html {
        head {
            meta {
                charset("UTF-8")
                viewport()
            }
            link {
                css("./css/main.css")
            }
        }
        body {
            nav {
                ul {
                    (1..maxCycle).forEach { count ->
                        a(href = "#count$count") { +"$count 回目" }
                    }
                }
            }
            (1..maxCycle).forEach { count ->
                section(attr = mapOf("id" to "count$count")) {
                    span { +"$count 回目" }

                    val articleData = calcArticleData(obj, count)
                    articleData.sortedBy { -it.percent }.forEach { data ->
                        article {
                            span(attr = mapOf("id" to "m")) { +data.mino }
                            img(src = "./img/${data.image}.png")
                            if (0 <= data.allCount) {
                                span(attr = mapOf("id" to "p")) { +"%.3f %%".format(data.percent) }
                                span(attr = mapOf("id" to "c")) { +"(%d/%d)".format(data.success, data.allCount) }
                            } else {
                                span(attr = mapOf("id" to "p")) { +"---" }
                                span(attr = mapOf("id" to "c")) { +"(---)" }
                            }
                        }
                    }
                }
            }
        }
    }

//    println(html.generate())
    File("output/index.html").writeText(html.generate("", ""))
}

private fun calcArticleData(obj: Obj, count: Int): List<ArticleData> {
    val cycle = Cycle(count)
    println(cycle)

    val pieceCounters = getPieceCounters(cycle)
    println(pieceCounters.size)

    val bests = pieceCounters.map { it to getBest(it, cycle) }.toMap()

    return bests.map {
        val pieceCounter = it.key
        val mino = pieceCounter.blocks.joinToString("") { it.name }

        val pair = it.value ?: return@map ArticleData(mino, "nothing", 0, -1)

        val result = pair.first

        val field = colorize(pieceCounter, result.fieldData, obj)

        generatePng(cycle, mino, field, obj)

        ArticleData(mino, "%s_%03d".format(mino, cycle.number), result.success.value, pair.second)
    }.toList()
}

fun generatePng(cycle: Cycle, mino: String, field: String, obj: Obj) {
    val prefix = String.format("output/img/%s", mino)
    File(prefix).parentFile.mkdirs()
    val minoFactory = obj.minoFactory
    val colorConverter = obj.colorConverter
    val setting = FigSetting(FrameType.NoFrame, obj.height, 0)

    val fieldOnlyFigGenerator = FieldOnlyFigGenerator(setting, minoFactory, colorConverter)
    val writer = PngWriter(minoFactory, colorConverter, fieldOnlyFigGenerator, Bag.EMPTY, 0, prefix, cycle.number - 1)

    val pages = Tetfu(minoFactory, colorConverter).decode(field)
    val page = pages[0]
    writer.write(listOf(page))
}

data class ArticleData(
        val mino: String,
        val image: String,
        val success: Int,
        val allCount: Int
) {
    val percent: Double = success * 100.0 / allCount
}