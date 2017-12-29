package percent

import common.buildup.BuildUpStream
import common.datastore.FullOperationWithKey
import common.datastore.MinimalOperationWithKey
import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import common.datastore.blocks.LongPieces
import common.datastore.blocks.Pieces
import common.parser.StringEnumTransform
import core.action.reachable.LockedReachable
import core.action.reachable.Reachable
import core.field.Field
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.Rotate
import helper.KeyParser
import helper.Patterns
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val height = 4

    val minoFactory = MinoFactory()
    val usingPieces = listOf(Piece.L)
    val minoShifter = MinoShifter()

    val path = Paths.get("output/index.csv")
    val index = Index(minoFactory, minoShifter, path)

    val allSolutionsPath = Paths.get("output/indexed_solutions_10x4_SRS.csv")
    val solutionLoader = SolutionLoader(allSolutionsPath, index, setOf())

    val searchingPieces = SearchingPieces(Patterns.hold(1), PieceCounter(usingPieces))

    val successCalculator = Success(solutionLoader, index, searchingPieces, height)

    println("ready")

    val minos = listOf(
            MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Right), 2, 1, 0L)
    )

    val minoRotation = MinoRotation()
    val reachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)
    val success = successCalculator.success(minos, reachable)
    println(success)
}

class Success(private val solutionLoader: SolutionLoader, val index: Index, val searchingPieces: SearchingPieces, val height: Int) {
    val allCount = searchingPieces.allCount

    fun success(current: MinimalOperationWithKey, reachable: Reachable): Int {
        return success(listOf(current), reachable)
    }

    fun success(minos: List<MinimalOperationWithKey>, reachable: Reachable): Int {
        val field = minoToField(minos)
//        println(FieldView.toString(field))

        val solutionsMap = solutionLoader
                .load(minos)
                .map { it.key to Solutions(it.value, field, reachable, height) }
                .toMap()
        println("solutions: ${solutionsMap.size}")

        val maxDepth = 10L - solutionLoader.requires.size - minos.size
        val failedCount = failedCount(solutionsMap, maxDepth, searchingPieces.piecesMap)
        val allCount = searchingPieces.allCount

        return allCount - failedCount
    }

    private fun failedCount(solutionsMap: Map<PieceCounter, Solutions>, maxDepth: Long, searchingPieces: Map<LongPieces, List<Pieces>>): Int {
        var counter = 0
        val checker = Checker(solutionsMap, maxDepth.toInt())
        searchingPieces.entries.forEach { entry ->
            if (checker.checks1(entry.key))
                return@forEach

            entry.value.forEach {
                if (!checker.checks2(it))
                    counter += 1
            }
        }
        return counter
    }

    private fun minoToField(minos: List<MinimalOperationWithKey>): Field {
        val field = FieldFactory.createField(height)
        (solutionLoader.requires + minos).forEach {
            val minoField = FieldFactory.createField(height)
            minoField.put(it.mino, it.x, it.y)
            minoField.insertWhiteLineWithKey(it.needDeletedKey)
            field.merge(minoField)
        }
        return field
    }
}

class Solutions(solutions: List<Solution>, initField: Field, reachable: Reachable, height: Int = 4) {
    val tree: SuccessTreeHead by lazy {
        val eHead = SuccessTreeHead()
        solutions.stream()
                .forEach {
                    BuildUpStream(reachable, height).existsValidBuildPattern(initField, it.keys)
                            .forEach { eHead.register(it.stream().map { it.piece }) }
                }
        eHead
    }
}

data class Solution(val keys: List<MinimalOperationWithKey>)

class Index(val minoFactory: MinoFactory, val minoShifter: MinoShifter, val path: Path) {
    private val toKeyMap: Map<Int, MinimalOperationWithKey>
    private val toIndexMap: Map<MinimalOperationWithKey, Int>

    init {
        val toPiece: String.() -> Piece = { StringEnumTransform.toPiece(this) }
        val toRotate: String.() -> Rotate = { StringEnumTransform.toRotate(this) }
        val toKey: String.() -> Long = { KeyParser.parseToLong(this) }

        this.toKeyMap = Files.lines(path)
                .map { it.split(",") }
                .collect(Collectors.toMap({ it[0].toInt() }, { it: List<String> ->
                    val mino = minoFactory.create(it[1].toPiece(), it[2].toRotate())

                    val x = it[3].toInt()
                    val lowerY = it[4].toInt()
//                val usingKey = it[5].toKey()
                    val deleteKey = it[6].toKey()
                    MinimalOperationWithKey(mino, x, lowerY - mino.minY, deleteKey)
                }))

        this.toIndexMap = toKeyMap.map {
            it.value to it.key
        }.toMap()
    }

    fun get(key: MinimalOperationWithKey): Int? {
        fun whenNull(): Int? {
            minoShifter.enumerateSameOtherActions(key.piece, key.rotate, key.x, key.y).forEach {
                val newKey: MinoOperationWithKey = MinimalOperationWithKey(minoFactory.create(key.piece, it.rotate), it.x, it.y, key.needDeletedKey)
                return toIndexMap[newKey] ?: return@forEach
            }
            throw Error("Not found: $key")
        }
        return toIndexMap[key] ?: whenNull()
    }

    fun get(index: Int): MinimalOperationWithKey? {
        return toKeyMap[index]
    }
}

class Checker(private val solutionsMap: Map<PieceCounter, Solutions>, val maxDepth: Int) {
    fun checks1(pieces: Pieces): Boolean {
        val subList = PieceCounter(pieces.pieces.subList(0, maxDepth))
        solutionsMap[subList]?.let {
            if (it.tree.checksWithHold(pieces))
                return true
        }

        return false
    }

    fun checks2(pieces: Pieces): Boolean {
        (maxDepth - 1 downTo 0).forEach { lastHoldDepth ->
            val p = pieces.pieces
            p.removeAt(lastHoldDepth)
            val subList2 = PieceCounter(p)

            solutionsMap[subList2]?.let {
                if (it.tree.checksMixHold(pieces, lastHoldDepth)) {
//                    println("  found: $lastHoldDepth ${pieces.pieces}")
                    return true
                }
            }
        }

        return false
    }
}
