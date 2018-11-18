import common.buildup.BuildUpStream
import common.datastore.Operations
import common.parser.OperationInterpreter
import common.parser.OperationTransform
import common.tetfu.Tetfu
import common.tetfu.TetfuElement
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import concurrent.LockedReachableThreadLocal
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import core.srs.Rotate
import entry.path.output.MyFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()
    val colorConverter = ColorConverter()

    val maxHeight = 4
    val initField = FieldFactory.createField(maxHeight)

    val initColoredField = ArrayColoredField(24)
    (0..(maxHeight - 1)).forEach { y ->
        (0..9).forEach { x ->
            if (!initField.isEmpty(x, y)) {
                initColoredField.setColorType(ColorType.Gray, x, y)
            }
        }
    }

    val counter = AtomicInteger()
    val reachableThreadLocal = LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight)
    val filePath = "output/tspin4"
    println("all: " + Files.lines(Paths.get(filePath)).count())
    val elements = Files.lines(Paths.get(filePath))
            .collect(Collectors.toList())
            .map { OperationInterpreter.parseToOperations(it) }
            .filter {
                //                val i = counter.incrementAndGet()
//                if (i % 100 == 0) println(i)
                val operations = it.operations.subList(0, it.operations.size - 1)
                val operationWithKeys = OperationTransform.parseToOperationWithKeys(initField, Operations(operations), minoFactory, maxHeight)
                val field = FieldFactory.createField(maxHeight)
                operationWithKeys.forEach { op ->
                    val pieceField = FieldFactory.createField(maxHeight)
                    pieceField.put(op.mino, op.x, op.y)
                    pieceField.insertWhiteLineWithKey(op.needDeletedKey)
                    field.merge(pieceField)
                }
                field.clearLine()
                val last = it.operations[it.operations.size - 1]
                when (last.rotate) {
                    Rotate.Spawn -> {
                        0 <= last.y - 1 && field.isEmpty(last.x, last.y - 1)
                    }
                    Rotate.Left -> {
                        10 < last.x + 1 && field.isEmpty(last.x + 1, last.y)
                    }
                    Rotate.Reverse -> {
                        field.isEmpty(last.x, last.y + 1)
                    }
                    Rotate.Right -> {
                        0 <= last.x - 1 && field.isEmpty(last.x - 1, last.y)
                    }
                    else -> {
                        throw Error()
                    }
                }
            }
            .map {
                val i = counter.incrementAndGet()
                if (i % 100 == 0) println(i)
                val operations = it.operations.subList(0, it.operations.size - 1)
                val reachable = reachableThreadLocal.get()
                val operationWithKeys = OperationTransform.parseToOperationWithKeys(initField, Operations(operations), minoFactory, maxHeight)
                val count = BuildUpStream(reachable, maxHeight)
                        .existsValidBuildPattern(initField, operationWithKeys)
                        .count()
                it to count
            }
            .sortedByDescending { it.second }
            .stream()
//            .filter { 390 <= it.second }
//            .limit(100)
            .map { (first, second) ->
                val operationWithKeys = OperationTransform.parseToOperationWithKeys(initField, first, minoFactory, maxHeight)

                val blockField = OperationTransform.parseToBlockField(operationWithKeys, minoFactory, maxHeight)
                val coloredField = initColoredField.freeze(initColoredField.maxHeight)
                (0..(blockField.height - 1)).forEach { y ->
                    (0..9).forEach { x ->
                        val currentPiece = blockField.getBlock(x, y)
                        currentPiece?.let { coloredField.setColorType(colorConverter.parseToColorType(it), x, y) }
                    }
                }

                second to TetfuElement(coloredField,"" + second)
            }
            .collect(Collectors.toList())

    println(elements.size)

//    val tetfu = Tetfu(minoFactory, colorConverter)
//    val encode = tetfu.encode(elements.map { it.second })
//    val fumen = "v115@${encode}"

    MyFile("output/url.html").newBufferedWriter().use {
        it.write("<html><body><ol>")
        for ((first, second) in elements) {
            val tetfu = Tetfu(minoFactory, colorConverter)
            val encode = tetfu.encode(listOf(second))
            it.write("<li><a href='http://fumen.zui.jp/?v115@${encode}'>[${first}] ${encode}</a></li>")
        }
        it.write("</ol></body></html>")
    }
}
