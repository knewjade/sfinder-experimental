import common.parser.OperationInterpreter
import common.parser.OperationTransform
import common.tetfu.Tetfu
import common.tetfu.TetfuElement
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation.create()
    val colorConverter = ColorConverter()

    val maxHeight = 4
    val initField = FieldFactory.createField(maxHeight)


    val filePath1 = "output/tspin4"
    println("all: " + Files.lines(Paths.get(filePath1)).count())
    val fields1 = Files.lines(Paths.get(filePath1))
            .map { OperationInterpreter.parseToOperations(it) }
            .map {
                val operationWithKeys = OperationTransform.parseToOperationWithKeys(initField, it, minoFactory, maxHeight)
                OperationTransform.parseToBlockField(operationWithKeys, minoFactory, maxHeight)
            }
            .collect(Collectors.toSet())

    val filePath2 = "output/version2"
    println("all: " + Files.lines(Paths.get(filePath2)).count())
    val fields2 = Files.lines(Paths.get(filePath2))
            .map { OperationInterpreter.parseToOperations(it) }
            .map {
                val operationWithKeys = OperationTransform.parseToOperationWithKeys(initField, it, minoFactory, maxHeight)
                OperationTransform.parseToBlockField(operationWithKeys, minoFactory, maxHeight)
            }
            .collect(Collectors.toSet())

    println(fields1.size)
    println(fields2.size)
    println("intersect = ${(fields1.intersect(fields2)).size}")
    println("subtract1-2 = ${(fields1.subtract(fields2)).size}")
    println("subtract2-1 = ${(fields2.subtract(fields1)).size}")

    val initColoredField = ArrayColoredField(24)
    (0..(maxHeight - 1)).forEach { y ->
        (0..9).forEach { x ->
            if (!initField.isEmpty(x, y)) {
                initColoredField.setColorType(ColorType.Gray, x, y)
            }
        }
    }

    fields1.subtract(fields2).forEach { blockField ->
        val coloredField = initColoredField.freeze(initColoredField.maxHeight)
        (0..(blockField.height - 1)).forEach { y ->
            (0..9).forEach { x ->
                val currentPiece = blockField.getBlock(x, y)
                currentPiece?.let { coloredField.setColorType(colorConverter.parseToColorType(it), x, y) }
            }
        }
        val element = TetfuElement(coloredField, "")
        val tetfu = Tetfu(minoFactory, colorConverter)
        val fumen = tetfu.encode(Collections.singletonList(element))
        println("http://fumen.zui.jp/?v115@${fumen}")
    }
}
