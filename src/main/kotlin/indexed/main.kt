package indexed

import percent.SuccessTreeHead
import common.buildup.BuildUpStream
import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import common.parser.StringEnumTransform
import common.pattern.LoadedPatternGenerator
import concurrent.LockedReachableThreadLocal
import core.field.SmallField
import core.mino.MinoFactory
import core.mino.Piece
import core.srs.Rotate
import helper.KeyParser
import lib.Stopwatch
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors.toMap

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()

    val toPiece: String.() -> Piece = { StringEnumTransform.toPiece(this) }
    val toRotate: String.() -> Rotate = { StringEnumTransform.toRotate(this) }
    val toKey: String.() -> Long = { KeyParser.parseToLong(this) }

    val index: Map<Int, MinimalOperationWithKey> = Files.lines(Paths.get("output/index.csv"))
            .map { it.split(",") }
            .collect(toMap({ it[0].toInt() }, { it: List<String> ->
                val mino = minoFactory.create(it[1].toPiece(), it[2].toRotate())

                val x = it[3].toInt()
                val lowerY = it[4].toInt()
//                val usingKey = it[5].toKey()
                val deleteKey = it[6].toKey()
                MinimalOperationWithKey(mino, x, lowerY - mino.minY, deleteKey)
            }))

    val counter = PieceCounter(Piece.valueList() + listOf(Piece.I, Piece.O, Piece.S))

    val counter2 = AtomicInteger()
//    val solutions: Map<PieceCounter, List<T>> = Files.lines(Paths.get("output/indexed_solutions_10x4_SRS.csv"))
    val solutions: Map<PieceCounter, List<Solution>> = Files.lines(Paths.get("output/test.csv"))
            .map { line ->
                val indexes = line.split(",").map { it.toInt() }
                val map: List<MinimalOperationWithKey> = indexes.map { index[it]!! }
                Solution(map)
            }
            .peek {
                val v = counter2.incrementAndGet()
                if (v % 1000000 == 0) println(v)
            }
            .filter { PieceCounter(it.keys.stream().map { it.piece }).equals(counter) }
            .collect(toMap(
                    { it: Solution -> PieceCounter(it.keys.stream().map { it.piece }) },
                    { it -> mutableListOf(it) },
                    { t, u -> t.addAll(u); t })
            )

//    BufferedWriter(OutputStreamWriter(FileOutputStream("output/test.csv"), StandardCharsets.UTF_8)).use { writer ->
//        Files.lines(Paths.get("output/indexed_solutions_10x4_SRS.csv"))
//                .peek {
//                    val v = counter2.incrementAndGet()
//                    if (v % 1000000 == 0) println(v)
//                }
//                .filter { line ->
//                    val indexes = line.split(",").map { it.toInt() }
//                    val map: List<MinimalOperationWithKey> = indexes.map { index[it]!! }
//                    PieceCounter(map.stream().map { it.piece }).equals(counter)
//                }
//                .forEach { writer.write(it); writer.newLine() }
//        writer.flush()
//    }
//
    println(index.count())
    println(solutions.count())
    println(solutions.values.sumBy { it.size })

    val first = solutions[solutions.keys.first()]!!
    println(first)

    val second = listOf(
            MinimalOperationWithKey(minoFactory.create(Piece.O, Rotate.Spawn), 3, 0, 0L),
            MinimalOperationWithKey(minoFactory.create(Piece.S, Rotate.Right), 0, 1, 0L),
            MinimalOperationWithKey(minoFactory.create(Piece.L, Rotate.Reverse), 6, 1, 0L),
            MinimalOperationWithKey(minoFactory.create(Piece.Z, Rotate.Spawn), 4, 2, 0L)
//            MinimalOperationWithKey(minoFactory.create(Piece.Z, Rotate.Right), 8, 1, 0L)
    )

    mg(first)
//    mg(minoFactory, T(second))
}

private fun mg(solutions: List<Solution>) {
    val stopwatch = Stopwatch.createStartedStopwatch()

    val height = 4
    val reachable = LockedReachableThreadLocal(height)
    val initField = SmallField()
    val eHead = SuccessTreeHead()
//    val a = LongPieces(listOf(Piece.S, Piece.I, Piece.O, Piece.T, Piece.I, Piece.L, Piece.J, Piece.S, Piece.Z, Piece.O))
    solutions.stream()
            .forEach {
                BuildUpStream(reachable.get(), height).existsValidBuildPattern(initField, it.keys)
                        .forEach { eHead.register(it.stream().map { it.piece }) }
            }

    stopwatch.stop()
    println(stopwatch.toMessage(TimeUnit.SECONDS))
    println("ready")
    LoadedPatternGenerator("[IOS]!,*p7").blocksStream()
            .forEach {
                if (eHead.checksWithHold(it))
                    return@forEach

                (10 downTo 0).forEach {  }
                println("NG" + it.pieces)
            }
}

private fun mg(minoFactory: MinoFactory, first: Solution) {
//    val height = 4
//    val reachable = LockedReachable(minoFactory, MinoShifter(), MinoRotation(), height)


//    buildUp.existsValidBuildPattern(SmallField(), first.keys)
//            .forEach { println(eHead.checksWithHold(LongPieces(it.stream().map { it.piece }))) }


//    println("149,169,170,202,304,416,505,560,671,712")

//    val allField = FieldFactory.createField("" +
//            "XXXXXXXXXX" +
//            "XXXXXXXXXX" +
//            "XXXXXXXXXX" +
//            "XXXXXXXXXX"
//    )
//    for (key in first.keys) {
//        val field = FieldFactory.createField(height)
//        field.put(key.mino, key.x, key.y)
//        field.insertWhiteLineWithKey(key.needDeletedKey)
//
//        val freeze = allField.freeze(4)
//        freeze.reduce(field)
//
//
//    }
}

data class Solution(val keys: List<MinimalOperationWithKey>) {
}