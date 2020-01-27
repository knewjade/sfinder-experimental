package tsd_opener

import common.parser.OperationWithKeyInterpreter
import common.tetfu.Tetfu
import common.tetfu.TetfuElement
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import common.tetfu.field.ColoredField
import core.field.FieldFactory
import core.mino.MinoFactory
import entry.path.output.OneFumenParser
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

// number -> fumen

fun main(args: Array<String>) {
    fun mirroredColorType(type: ColorType): ColorType {
        return when (type) {
            ColorType.I, ColorType.O, ColorType.T -> type
            ColorType.J -> ColorType.L
            ColorType.L -> ColorType.J
            ColorType.S -> ColorType.Z
            ColorType.Z -> ColorType.S
            else -> throw IllegalArgumentException("${type}")
        }
    }

    fun mirror(field: ColoredField): ColoredField {
        val height = field.maxHeight

        val mirrored = ArrayColoredField(height)
        for (y in 0..height - 1) {
            for (x in 0..9) {
                val type = field.getColorType(x, y)
                if (ColorType.isMinoBlock(type)) {
                    mirrored.setColorType(mirroredColorType(type), 9 - x, y)
                }
            }
        }

        return mirrored
    }

    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()

    val height = 10

    val operationsPairList = Files.readAllLines(Paths.get("figs/tsd"))
            .map { line ->
                val o = line.split(":")
                assert(o.size == 2)
                val (index, operations) = o
                Integer.parseInt(index) to OperationWithKeyInterpreter.parseToList(operations, minoFactory)
            }

    val dataToIndex = HashMap<String, Int>()
    operationsPairList.forEach {
        val parser = OneFumenParser(minoFactory, colorConverter)
        val data = parser.parse(it.second, FieldFactory.createField(height), height, "")
        println(data)
        dataToIndex[data] = it.first
    }
    println("---")

    PrintWriter(BufferedWriter(FileWriter("figs/fumen.json"))).use { writer ->
        writer.println("[")
        operationsPairList.forEach {
            val parser = OneFumenParser(minoFactory, colorConverter)
            val data = parser.parse(it.second, FieldFactory.createField(height), height, "")

            val coloredField = parser.parseToColoredField(it.second, FieldFactory.createField(height), height)
            val mirrored = mirror(coloredField)

            val mirroredData = Tetfu(minoFactory, colorConverter).encode(listOf(TetfuElement.createFieldOnly(mirrored)))
            println(mirroredData)
            val mirroredIndex = dataToIndex[mirroredData]
            writer.println("[\"$data\",$mirroredIndex],")
        }
        writer.println("]")
    }
}