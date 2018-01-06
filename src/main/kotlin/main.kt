import common.datastore.PieceCounter
import common.parser.StringEnumTransform
import main.caller.ContentCaller
import main.domain.*
import percent.Index
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


fun main(args: Array<String>) {
    val factories = createFactories()
    val index = Index(factories.minoFactory, factories.minoShifter, Paths.get("input/index.csv"))

    Files.walk(Paths.get("input/fumen"))
            .map { it.toFile() }
            .filter { it.isFile }
            .forEach {
                val content = it.readText()
                val caller = ContentCaller(content)
                val results = caller.call()

                if (results.details.isNotEmpty()) {
                    val fieldData = FieldData(it.name)
                    val minos = it.parentFile.name
                    val cycle = Cycle(it.parentFile.parentFile.name.toInt())
                    val pieceCounter = minos.toCharArray().map { StringEnumTransform.toPiece(it) }.let { PieceCounter(it) }
                    val using = results.details.map { index.get(it.mino.index)!! }.map { it.piece }

                    val mino = using[0]
                    if (using.any { it != mino })
                        Error("illegal: $using")

                    val newPieceCount = pieceCounter.removeAndReturnNew(PieceCounter(mutableListOf(mino)))
                    val newMino = newPieceCount.blocks.sorted().joinToString("") + mino.name

                    println("$fieldData $newMino $cycle")

                    val file = File("output/fumen/${cycle.number}/$newMino/${fieldData.representation}")
                    file.parentFile.mkdirs()
                    file.writeText(content)
                }
            }
}