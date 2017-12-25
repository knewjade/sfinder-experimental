package percent

import common.buildup.BuildUpStream
import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import common.datastore.action.Action
import common.datastore.blocks.LongPieces
import common.datastore.blocks.Pieces
import common.parser.StringEnumTransform
import common.pattern.LoadedPatternGenerator
import core.action.candidate.Candidate
import core.action.candidate.LockedCandidate
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
import searcher.checker.CheckerUsingHold
import searcher.common.validator.PerfectValidator
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val height = 4
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()
    val reachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)

    val index = Index(minoFactory)
    val minos = listOf(MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Right), 2, 1, 0L))
    val nums = minos.map { index.get(it) }.toSet()

    val field = FieldFactory.createField(4)
    minos.forEach {
        val f = FieldFactory.createField(4)
        f.put(it.mino, it.x, it.y)
        f.insertWhiteLineWithKey(it.needDeletedKey)
        field.merge(f)
    }
    println(FieldView.toString(field))

    println("# Load")
    val loaded = Files.lines(Paths.get("output/indexed_solutions_10x4_SRS.csv"))
            .map { line ->
                line.split(",").map { it.toInt() }
            }
            .filter { it.containsAll(nums) }
            .map {
                val map: List<MinimalOperationWithKey> = it.filter { !nums.contains(it) }
                        .map { index.get(it)!! }
                Solution(map)
            }
            .collect(Collectors.groupingBy({ it: Solution -> PieceCounter(it.keys.stream().map { it.piece }) }))
            .map { it.key to Solutions(it.value!!, field, reachable, height) }
            .toMap()

    println("# Piece")
    val maxDepth = 10L - minos.size
    val drawn = LoadedPatternGenerator("[^L]!,*p4").blocksStream()
            .collect(Collectors.groupingBy({ it: Pieces -> LongPieces(it.blockStream().limit(maxDepth).map { it }) }))
    println(drawn.size)

//    val usingHold = CheckerUsingHold<Action>(minoFactory, PerfectValidator())
//    val candidate: Candidate<Action> = LockedCandidate(minoFactory, minoShifter, minoRotation, height)


    println("# Check")
    var counter = 0
    val checker = Checker(loaded, maxDepth.toInt())
    drawn.entries.forEach { entry ->
        // [S, J, Z, T, O, I, S, I, Z, T]
//        if (!entry.key.pieces.toString().equals("[S, J, Z, T, O, I, S, I, Z]"))
//            return@forEach

        if (checker.checks1(entry.key))
            return@forEach

//        println("on hold: ${entry.key}")
        entry.value.forEach {
            if (!checker.checks2(it)) {
//                println("  no perfect: $it")
//                val check = usingHold.check(field, it, candidate, height, maxDepth.toInt())
//                if (check)
//                    println("  wrong: $it")
                counter += 1
            }
        }
    }

    val size = drawn.values.sumBy { it.size }
    println(100.0 * (size - counter) / size)
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

class Index(minoFactory: MinoFactory) {
    private val toKeyMap: Map<Int, MinimalOperationWithKey>
    private val toIndexMap: Map<MinimalOperationWithKey, Int>

    init {
        val toPiece: String.() -> Piece = { StringEnumTransform.toPiece(this) }
        val toRotate: String.() -> Rotate = { StringEnumTransform.toRotate(this) }
        val toKey: String.() -> Long = { KeyParser.parseToLong(this) }

        this.toKeyMap = Files.lines(Paths.get("output/index.csv"))
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
        return toIndexMap[key]
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