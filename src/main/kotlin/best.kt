import common.tetfu.common.ColorConverter
import core.mino.MinoFactory
import core.mino.MinoShifter
import entry.path.LockedBuildUpListUpThreadLocal
import searcher.pack.SizedBit
import searcher.pack.memento.AllPassedSolutionFilter
import searcher.pack.task.BasicMinoPackingHelper
import webpage.html
import java.io.File

data class Obj(val height: Int = 4) {
    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()

    val minoShifter = MinoShifter()
    val sizedBit = SizedBit(3, height)
    val taskResultHelper = BasicMinoPackingHelper()
    val solutionFilter = AllPassedSolutionFilter()
    val buildUpStreamThreadLocal = LockedBuildUpListUpThreadLocal(height)
}

fun main(args: Array<String>) {
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
                    (1..8).forEach { count ->
                        a(href = "#count$count") {  +"$count 回目" }
                    }
                }
            }
            (1..8).forEach { count ->
                section(attr = mapOf("id" to "count$count")) {
                    span { +"$count 回目" }

                    val articleData = calcArticleData()
                    articleData.sortedBy { -it.percent }.forEach { data ->
                        article {
                            span(attr = mapOf("id" to "m")) { +data.name }
                            img(src = "./img/${data.image}.png")
                            span(attr = mapOf("id" to "p")) { +"%.3f %%".format(data.percent) }
                            span(attr = mapOf("id" to "c")) { +"(%d/%d)".format(data.success, data.allCount) }
                        }
                    }
                }
            }
        }
    }

    println(html.generate())
    File("output/index.html").writeText(html.generate("", ""))
}

private fun calcArticleData(): List<ArticleData> {
    return listOf(ArticleData())
}

data class ArticleData(val name: String = "hello") {
    val image: String = "hello"
    val success: Int = 1
    val allCount: Int = 10
    val percent: Double = success * 100.0 / allCount
}