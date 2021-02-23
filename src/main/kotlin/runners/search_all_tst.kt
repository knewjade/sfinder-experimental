import common.parser.OperationInterpreter
import common.parser.OperationTransform
import components.MinoOperationWithKeysList
import components.SFSpin
import core.field.Field
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import entry.path.output.MyFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

// 入力した地形から、TSTの一覧を作成する。TSTに使用するミノの数は特に指定しない
fun main() {
    SearchAllTst().run()
}

class SearchAllTst {
    fun run() {
        val height = 12
        val initField = FieldFactory.createField(height)

        val operationsList = loadOperationsList("resources/tss_tst100p_false.txt", initField, height)
            .filter { it.field(initField, height).clearLine() == 1 }  // TSSのみを抽出
            .filter { it.operationWithKeys.size == 7 }
        println(operationsList.size)

        val progress = ProgressEstimator.start(operationsList.size)

        val sfSpin = SFSpin(MinoFactory(), MinoShifter(), MinoRotation.create(), height)
        val spins = operationsList
            .map {
                val field = it.field(initField, height)
                field.clearLine()
                val result = sfSpin.run(field, 3)
                progress.increment()
                it to result
            }

        val lines = spins.flatMap { pair ->
            // 1st bag
            val headLine = "#" + pair.first.let {
                val operations = OperationTransform.parseToOperations(initField, it.operationWithKeys, height)
                OperationInterpreter.parseToString(operations)
            }

            // 2nd bag
            val secondLines = run {
                val field = pair.first.field(initField, height)
                field.clearLine()

                pair.second
                    .map { OperationTransform.parseToOperations(field, it.operationWithKeys, height) }
                    .map { OperationInterpreter.parseToString(it) }
            }

            listOf(headLine) + secondLines
        }

        MyFile("search_all_tst.txt").newAsyncWriter().use { it.writeAndNewLine(lines) }
    }

    private fun loadOperationsList(
        fileName: String, initField: Field, height: Int
    ): List<MinoOperationWithKeysList> {
        val minoFactory = MinoFactory()
        return loadLines(fileName)
            .map { OperationInterpreter.parseToOperations(it) }
            .map { operations ->
                OperationTransform.parseToOperationWithKeys(initField, operations, minoFactory, height)
            }
            .map { MinoOperationWithKeysList(it) }
    }

    private fun loadLines(fileName: String): List<String> {
        return Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8).use { reader ->
            reader.lines().filter { it.isNotBlank() }.collect(Collectors.toList())
        }
    }
}