package tsd_opener

import common.parser.OperationTransform
import common.parser.OperationWithKeyInterpreter
import common.tetfu.DirectTetfuPage
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import core.mino.MinoFactory
import core.srs.Rotate
import util.fig.Bag
import util.fig.FigSetting
import util.fig.FrameType
import util.fig.generator.FieldOnlyFigGenerator
import util.fig.output.FigWriter
import util.fig.output.PngWriter
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()

    val fieldHeight = 6

    Files.readAllLines(Paths.get("figs/tsd"))
            .map { line ->
                val o = line.split(":")
                assert(o.size == 2)
                val (index, operations) = o
                Integer.parseInt(index) to OperationWithKeyInterpreter.parseToList(operations, minoFactory)
            }
            .forEach { (index, operations) ->
                val outputFile = String.format("figs/%03d", index)

                val blockField = OperationTransform.parseToBlockField(operations, minoFactory, fieldHeight)
                val initColoredField = ArrayColoredField(24)
                val coloredField = initColoredField.freeze(initColoredField.maxHeight)
                (0..(blockField.height - 1)).forEach { y ->
                    (0..9).forEach { x ->
                        val currentPiece = blockField.getBlock(x, y)
                        currentPiece?.let { coloredField.setColorType(colorConverter.parseToColorType(it), x, y) }
                    }
                }

                val usingTetfuPages = listOf(
                        DirectTetfuPage(ColorType.Empty, 0, 0, Rotate.Spawn, "", coloredField, false, false)
                )
                val figWriter = createFigWriter(minoFactory, colorConverter, fieldHeight, outputFile)
                figWriter.write(usingTetfuPages)
            }
}

fun createFigWriter(minoFactory: MinoFactory, colorConverter: ColorConverter, height: Int, path: String): FigWriter {
    // generatorの準備
    val figSetting = FigSetting(FrameType.NoFrame, height, 0)
    val figGenerator = FieldOnlyFigGenerator(figSetting, minoFactory, colorConverter)

    // Bagの作成
    val bag = Bag(listOf(), null)

    return PngWriter(minoFactory, colorConverter, figGenerator, bag, 0, path, 0)
}
