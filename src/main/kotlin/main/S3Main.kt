package main

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import common.datastore.PieceCounter
import common.datastore.action.Action
import common.datastore.blocks.LongPieces
import common.datastore.blocks.Pieces
import common.iterable.PermutationIterable
import common.pattern.LoadedPatternGenerator
import concurrent.LockedCandidateThreadLocal
import concurrent.LockedReachableThreadLocal
import concurrent.checker.CheckerUsingHoldThreadLocal
import concurrent.checker.invoker.CheckerCommonObj
import concurrent.checker.invoker.using_hold.ConcurrentCheckerUsingHoldInvoker
import core.action.candidate.LockedCandidate
import core.field.Field
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import helper.Patterns
import lib.MyIterables
import lib.Stopwatch
import searcher.PutterNoHold
import searcher.common.validator.PerfectValidator
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

fun main(args: Array<String>) {
    run(1, listOf(Piece.I, Piece.J, Piece.L))
/*

    val s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val bucketName = "fumen-dev"
    val bucket = main.S3Bucket(s3, bucketName)

//    val notNullFilePath = filePath?.let { it } ?: "default"
//    println(notNullFilePath)

    val filePath: String? = "dummy"  // 存在しないファイルのパス
    fun String.exists(): Boolean = false  // ファイルのチェックを実装
    val notNullFilePath = filePath?.takeIf { it.exists() } ?: "default"
    println(notNullFilePath)  // -> default

    val key = "test-7"
    val s3Object = try {
        bucket.getObject(key)
    } catch (e: Exception) {
        println("not found")

        Thread() {
            println("..wait")
            Thread.sleep(5000L)
            val putObject = s3.putObject(bucketName, key, "body")
            println("..created")
        }.start()

        println("waiter")
        bucket.waitUntilObjectExists(key, { bucket.getObject(key) })
//        println("ok")
//        bucket.getObject(key)
    }
    println(s3Object)
    */
}

fun run(cycle: Int, headPieces: List<Piece>) {
    val maxClearLine = 4

    val pieces = getAllPieces(cycle, headPieces)
    val targets = getAllTargets(headPieces, maxClearLine)

    val minoFactory = MinoFactory()
    val candidateThreadLocal = LockedCandidateThreadLocal(maxClearLine)
    val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
    val reachableThreadLocal = LockedReachableThreadLocal(maxClearLine)
    val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
    val core = Runtime.getRuntime().availableProcessors()
    val executorService = Executors.newFixedThreadPool(core)
    val invoker = ConcurrentCheckerUsingHoldInvoker(executorService, commonObj)

    val maxDepth = 10 - headPieces.size
    val stopwatch = Stopwatch.createStoppedStopwatch()
    for (target in targets.toList().subList(0, 10)) {
        stopwatch.start()

        val field = target.field
        val targetInitMaxClearLine = target.maxClearLine
        val resultPairs = invoker.search(field, pieces, targetInitMaxClearLine, maxDepth)

        val successCount = resultPairs.parallelStream()
                .filter { it.value }
                .count()

        println(FieldView.toString(field, targetInitMaxClearLine))
        println(100.0 * successCount / resultPairs.size)
        println(successCount)
        println(resultPairs.size)
        
        stopwatch.stop()
        println(stopwatch.toMessage(TimeUnit.MILLISECONDS))
    }
    println(targets.size)
    executorService.shutdown()
}

private fun getAllPieces(cycle: Int, headPieces: List<Piece>): List<Pieces> {
    val pattern = Patterns.hold(cycle)
    val generator = LoadedPatternGenerator(pattern)

    val numOfPiece = headPieces.size.toLong()
    val headPieceCounter = PieceCounter(headPieces)

    return generator.blocksStream().parallel()
            .filter {
                val pieceCounter = PieceCounter(it.blockStream().limit(numOfPiece))
                pieceCounter == headPieceCounter
            }
            .map({ LongPieces(it.blockStream().skip(numOfPiece)) })
            .collect(Collectors.toSet())
            .toList()
}

private fun getAllTargets(headPieces: List<Piece>, maxClearLine: Int): Set<Target> {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()
    val perfectValidator = PerfectValidator()
    val initField = FieldFactory.createField(maxClearLine)
    val maxDepth = 10

    val fields = mutableSetOf<Target>()
    val heads = MyIterables.toList(PermutationIterable(headPieces, headPieces.size))
    heads.forEach {
        val putter = PutterNoHold<Action>(minoFactory, perfectValidator)
        val candidate = LockedCandidate(minoFactory, minoShifter, minoRotation, maxClearLine)
        val orders = putter.first(initField, it, candidate, maxClearLine, maxDepth)
        orders.mapTo(fields) { Target(it.field, it.maxClearLine) }
    }
    return fields
}

data class Target(val field: Field, val maxClearLine: Int) {
    override fun hashCode(): Int {
        return field.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return field.equals(other)
    }
}

class S3Bucket(private val s3: AmazonS3, private val bucketName: String) : AmazonS3 by s3 {
    fun <T> waitUntilObjectExists(key: String, callback: () -> T, retry: Int = 10, sleeptime: Long = 1000L): T {
        for (i in 1..retry) {
            if (s3.doesObjectExist(bucketName, key))
                return callback()
            Thread.sleep(sleeptime)
            print("wait")
        }
        throw IllegalStateException("Doesn't exist object")
    }

    fun getObject(key: String): S3Object {
        return s3.getObject(bucketName, key)
    }
}