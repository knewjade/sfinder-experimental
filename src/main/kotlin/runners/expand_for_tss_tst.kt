import common.datastore.Operations
import common.datastore.PieceCounter
import common.parser.OperationInterpreter
import common.parser.OperationTransform
import components.LockedReachableExpanderThreadLocal
import components.MinoOperationWithKeysList
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.Piece
import entry.path.output.MyFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

// TSS->TST一覧のTSTを7ミノに拡張する
fun main() {
    ExpandForTssTst().run()
}

class ExpandForTssTst {
    fun run() {
        val height = 12
        val initField = FieldFactory.createField(height)

        val headSecond = loadTssTst("resources/search_all_tst.txt")
        val head = headSecond.first
        val second = headSecond.second

        val minoFactory = MinoFactory()
        val expanderThreadLocal = LockedReachableExpanderThreadLocal.create(height)
        val allPieceCounter = PieceCounter(Piece.valueList())

        val lines = head.zip(second).flatMap { pair ->
            // 1st bag
            val headLine = "#" + pair.first.let {
                OperationInterpreter.parseToString(it)
            }

            val first = MinoOperationWithKeysList(
                OperationTransform.parseToOperationWithKeys(initField, pair.first, minoFactory, height)
            )

            // 2nd bag
            val secondLines = run {
                val field = first.field(initField, height)
                field.clearLine()

                pair.second
                    .map { OperationTransform.parseToOperationWithKeys(field, it, minoFactory, height) }
                    .map { MinoOperationWithKeysList(it) }
                    .flatMap { operationsWithKeysList ->
                        val expander = expanderThreadLocal.get()
                        val unusedPieces = allPieceCounter.removeAndReturnNew(operationsWithKeysList.usedPieceCounter())
                        expander.moveWithTSpin(field, operationsWithKeysList, unusedPieces)
                    }
                    .map {
                        OperationTransform.parseToOperations(field, it.operationWithKeys, height)
                    }
                    .map { OperationInterpreter.parseToString(it) }
            }

            listOf(headLine) + secondLines
        }

        MyFile("tst_after_tss_expanded.txt").newAsyncWriter().use { it.writeAndNewLine(lines) }
    }

    private fun loadTssTst(fileName: String): Pair<List<Operations>, List<List<Operations>>> {
        val head = mutableListOf<Operations>()
        val second = mutableListOf<MutableList<Operations>>()
        loadLines(fileName).forEach { line ->
            val isFirst = line.startsWith("#")
            val str = if (isFirst) line.substring(1) else line
            val operations = OperationInterpreter.parseToOperations(str)

            if (isFirst) {
                head.add(operations)
                second.add(mutableListOf())
            } else {
                second.last().add(operations)
            }
        }

        if (head.size != second.size) error("Illegal state in the list")
        println(head.size)
        if (!(second.all { it.isNotEmpty() })) {
            error("Contains tss that does not has tst")
        }

        return head to second
    }

    private fun loadLines(fileName: String): List<String> {
        return Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8).use { reader ->
            reader.lines().filter { it.isNotBlank() }.collect(Collectors.toList())
        }
    }
}