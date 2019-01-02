import common.datastore.FullOperationWithKey
import common.datastore.PieceCounter
import core.field.Field
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import searcher.pack.separable_mino.AllMinoFactory
import java.util.*
import java.util.stream.Stream

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()

    val maxHeight = 4
    val factory = AllMinoFactory(minoFactory, minoShifter, 10, maxHeight, 0L)
    val all = factory.create().toList()
    val tPieces = all.asSequence().filter { it.piece == Piece.T }.toList()
    val otherPieces = all.asSequence().filter { it.piece != Piece.T }.toList()

    val initField = FieldFactory.createField(maxHeight)

    allLine(maxHeight).forEach { lines ->
        println(lines)

        // 埋めるべきラインを決める
        val field = FieldFactory.createField(maxHeight)
        for (y in lines) {
            (0..9).forEach { field.setBlock(it, y) }
        }

        // 必ず埋める必要がある領域
        val requiredList = tPieces.asSequence()
                .filter { !field.canPut(it.mino, it.x, it.y) }
                .map {
                    val freeze = field.freeze(maxHeight)
                    freeze.remove(it.mino, it.x, it.y)
                    freeze to it
                }
                .filter {
                    val freeze = it.first.freeze(maxHeight)
                    freeze.clearLine() == 0
                }
                .toList()

        // 最小
        val minMaxLineY = lines.min()!! to lines.max()!!
        requiredList.forEach { k(initField, it.first, otherPieces, minMaxLineY, maxHeight, it.second) }
    }
}

fun k(initField: Field, initRequired: Field, otherPieces: List<FullOperationWithKey>, minMaxLineY: Pair<Int, Int>, maxHeight: Int, last: FullOperationWithKey) {
    println(FieldView.toString(initRequired))

    val (minLineY, maxLineY) = minMaxLineY

    val testField = FieldFactory.createField(maxHeight)
    testField.put(last.mino, last.x, last.y)
    testField.insertWhiteLineWithKey(last.needDeletedKey)

    // 最低限のミノを選択
    val allPieces = otherPieces.asSequence()
            .filter { !(it.y + it.mino.maxY < minLineY || maxLineY < it.y + it.mino.minY) }
            .filter { testField.canPut(it.mino, it.x, it.y) }
            .groupBy { it.piece }
            .toMap()

    // 単一ミノのPieceCounterを作成
    val pieceCounterMap = EnumMap<Piece, PieceCounter>(Piece::class.java)
    for (piece in Piece.values()) {
        pieceCounterMap[piece] = PieceCounter(Stream.of(piece))
    }

    val done: MutableList<Result> = mutableListOf()

    fun search(result: Result, required: Field) {
        val restPieces = result.restPieces
        // すべてのミノを使い果たしたとき
        if (restPieces.counter == 0L) {
            return
        }

        for (piece in restPieces.blocks) {
            val operations = allPieces[piece]

            if (operations != null) {
                for (op in operations) {
                    if (required.canPut(op.mino, op.x, op.y) || !result.field.canPut(op.mino, op.x, op.y)) {
                        continue
                    }

                    val nextRestPieces = restPieces.removeAndReturnNew(pieceCounterMap[piece])

                    val next = OperationResult(result, op, nextRestPieces)

                    val nextRequired = required.freeze(maxHeight)
                    nextRequired.remove(op.mino, op.x, op.y)

                    if (nextRequired.isPerfect) {
                        // ラインがすべて埋まったとき
                        done.add(next)
                    } else {
                        // ラインが埋まっていない
                        search(next, nextRequired)
                    }
                }
            }
        }
    }

    val restPieces = PieceCounter(Piece.values().filter { it != Piece.T })
    search(EmptyResult(initField, restPieces), initRequired)

    println(done.size)
}

fun allLine(maxHeight: Int): List<List<Int>> {
    val list = mutableListOf<List<Int>>()
    list.addAll(singleLine(maxHeight))
    list.addAll(doubleLine(maxHeight))
    list.addAll(tripleLine(maxHeight))
    return list
}

fun singleLine(maxHeight: Int): List<List<Int>> {
    return (0..(maxHeight - 1)).map { listOf(it) }
}

fun doubleLine(maxHeight: Int): List<List<Int>> {
    val list = mutableListOf<List<Int>>()
    list.addAll((0..(maxHeight - 2)).map { listOf(it, it + 1) })
    list.addAll((0..(maxHeight - 3)).map { listOf(it, it + 2) })
    return list
}

fun tripleLine(maxHeight: Int): List<List<Int>> {
    return (0..(maxHeight - 3)).map { listOf(it, it + 1, it + 2) }
}

interface Result {
    val field: Field
    val restPieces: PieceCounter

    fun toOperationStream(): Stream<FullOperationWithKey>
}

class EmptyResult(override val field: Field, override val restPieces: PieceCounter) : Result {
    override fun toOperationStream(): Stream<FullOperationWithKey> {
        return Stream.empty()
    }
}

class OperationResult(
        private val result: Result,
        private val operationWithKey: FullOperationWithKey,
        override val restPieces: PieceCounter
) : Result {
    override val field: Field

    init {
        val field = result.field
        val height = field.maxFieldHeight
        val freeze = field.freeze(height)

        val pieceField = FieldFactory.createField(operationWithKey.mino.maxY)
        pieceField.put(operationWithKey.mino, operationWithKey.x, operationWithKey.y)
        pieceField.insertWhiteLineWithKey(operationWithKey.needDeletedKey)

        freeze.merge(pieceField)

        this.field = freeze
    }

    override fun toOperationStream(): Stream<FullOperationWithKey> {
        val stream = result.toOperationStream()
        return Stream.concat(stream, Stream.of(operationWithKey))
    }
}
