package main

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import common.datastore.PieceCounter
import common.datastore.action.Action
import common.datastore.blocks.LongPieces
import common.datastore.blocks.Pieces
import common.parser.StringEnumTransform
import common.pattern.LoadedPatternGenerator
import common.tetfu.Tetfu
import common.tetfu.TetfuElement
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import common.tetfu.field.ArrayColoredField
import common.tetfu.field.ColoredField
import concurrent.LockedCandidateThreadLocal
import concurrent.LockedReachableThreadLocal
import concurrent.checker.CheckerUsingHoldThreadLocal
import concurrent.checker.invoker.CheckerCommonObj
import concurrent.checker.invoker.ConcurrentCheckerInvoker
import concurrent.checker.invoker.using_hold.ConcurrentCheckerUsingHoldInvoker
import concurrent.checker.invoker.using_hold.SingleCheckerUsingHoldInvoker
import core.action.candidate.LockedCandidate
import core.field.Field
import core.field.FieldFactory
import core.mino.Mino
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import helper.Patterns
import lib.Stopwatch
import searcher.common.validator.PerfectValidator
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

var executorService: ExecutorService? = null

data class Factories(
        val minoFactory: MinoFactory,
        val minoShifter: MinoShifter,
        val minoRotation: MinoRotation,
        val colorConverter: ColorConverter,
        val perfectValidator: PerfectValidator
)

data class State(val field: Field, val maxClearLine: Int) {
    override fun hashCode(): Int {
        return field.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return field.equals(other)
    }
}

data class Input(val cycle: String,
                 val data: String,
                 val headPieces: String,
                 val next: String,
                 val prevPercent: String) {
    val cycleNumber = cycle.toInt()
    val nextPiece = StringEnumTransform.toPiece(next)
    val prevPercentValue = prevPercent.toDouble()
    val prefixPath = "${cycle}/${headPieces}${next}/${data}"
    val allPiece = headPieces + next
}

data class Result(val allCount: Int, val details: List<Triple<State, Int, String>>)

fun main(args: Array<String>) {
    workSQS("fumen-dev", "dev-test", 0.95, false)
}

fun workSQS(bucketName: String, queryName: String, threshold: Double, multiThread: Boolean) {
    val s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val sqs = AmazonSQSClient.builder()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()

    val queueUrl = sqs.getQueueUrl(queryName).queueUrl

    try {
        run(sqs, queueUrl, s3, bucketName, threshold, multiThread)
    } finally {
        sqs.shutdown()
        executorService?.shutdown()
    }
}

private fun run(sqs: AmazonSQS, queueUrl: String, s3: AmazonS3, bucketName: String, threshold: Double, multiThread: Boolean) {
    val request = ReceiveMessageRequest(queueUrl)
    val receive = sqs.receiveMessage(request)

    val messages = receive.messages

    if (messages.isEmpty())
        throw RuntimeException("Empty messages")
    else if (messages.size != 1)
        throw RuntimeException("Unexpected messages")

    val message = messages[0]
    println(message)

    val stopwatch = Stopwatch.createStartedStopwatch()

    invokeMessage(message, s3, bucketName, threshold, sqs, queueUrl, multiThread)

    stopwatch.stop()
    println(stopwatch.toMessage(TimeUnit.MINUTES))

    sqs.deleteMessage(queueUrl, message.receiptHandle);
}

private fun invokeMessage(message: Message, s3: AmazonS3, bucketName: String, threshold: Double, sqs: AmazonSQS, queueUrl: String, multiThread: Boolean) {
    val split = message.body.split(",")
    val input = Input(split[0], split[1], split[2], split[3], split[4])
    println(input)

    if (input.prevPercentValue < threshold)
        return

    val (allCount, details) = search(input.cycleNumber, input.data, input.headPieces, input.nextPiece, multiThread) ?: return

    val output = details.map {
        "${it.third},${it.second}"
    }.joinToString(";")

    val content = "${allCount}?${output}"
    s3.putObject(bucketName, input.prefixPath, content)

    val nextSearchCount = allCount * threshold
    val requests = details.filter { nextSearchCount < it.second }
    println("send ${requests.size}*7 messages")

    requests.forEach { detail ->
        println(detail)
        val entries = Piece.values().map {
            val percent = detail.second.toDouble() / allCount
            val messageId = String.format("%s-%s-%s-%s", input.cycle, detail.third, input.allPiece, it.name)
            val body = String.format("%s,%s,%s,%s,%.3f", input.cycle, detail.third, input.allPiece, it.name, percent)
            SendMessageBatchRequestEntry(messageId, body)
        }
        sqs.sendMessageBatch(queueUrl, entries)
    }
}

fun search(cycle: Int, fieldData: String, headPieces: String, next: Piece, multiThread: Boolean): Result? {
    val factories = createFactories()
    val initState = parseToField(fieldData, factories)
    val moves = move(initState, next, factories)
    println("moves: ${moves.size}")

    val pattern = Patterns.hold(cycle)
    val headPieceCounter = parseHeadBlockCounter(headPieces)
    val searchPieces = createSearchPieces(pattern, headPieceCounter, next).toList()

    val allCount = searchPieces.size
    println("pieces: ${allCount}")

    if (allCount == 0)
        return null

    val invoker = createInvoker(factories.minoFactory, multiThread)
    val details = moves.map {
        println("searching: ${it}")
        val field = it.field
        val maxClearLine = it.maxClearLine
        val maxDepth = (maxClearLine * 10 - field.numOfAllBlocks) / 4
        val results = invoker.search(field, searchPieces, maxClearLine, maxDepth)

        val successCount = results.count { it.value!! }

        val tetfu = Tetfu(factories.minoFactory, factories.colorConverter)
        val data = tetfu.encode(listOf(TetfuElement(parseColoredField(field), maxClearLine.toString())))

        Triple(it, successCount, data)
    }

    return Result(allCount, details)
}

private fun createFactories(): Factories {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()
    val colorConverter = ColorConverter()
    val perfectValidator = PerfectValidator()
    return Factories(minoFactory, minoShifter, minoRotation, colorConverter, perfectValidator)
}

private fun parseToField(data: String, factories: Factories): State {
    val minoFactory = factories.minoFactory
    val colorConverter = factories.colorConverter
    val tetfu = Tetfu(minoFactory, colorConverter)

    val decode = tetfu.decode(data)
    val page = decode[0]

    val coloredField = page.field
    if (page.isPutMino) {
        val piece = colorConverter.parseToBlock(page.colorType)
        val mino = Mino(piece, page.rotate)
        coloredField.putMino(mino, page.x, page.y)
    }

    val height = if (page.comment != "") Integer.valueOf(page.comment) else 4
    val empty = ColorType.Empty.number
    val field = FieldFactory.createField(height)
    for (y in 0 until height)
        for (x in 0..9)
            if (coloredField.getBlockNumber(x, y) != empty)
                field.setBlock(x, y)

    return State(field, height)
}

private fun parseHeadBlockCounter(headPieces: String): PieceCounter {
    val takeIf = headPieces.takeIf { it != "" }
    return takeIf?.let { LoadedPatternGenerator(it).blockCountersStream().findFirst().get() } ?: PieceCounter.EMPTY
}

internal fun createSearchPieces(pattern: String, headPieceCounter: PieceCounter, next: Piece): Set<Pieces> {
    val generator = LoadedPatternGenerator(pattern)

    val needPieceCounter = headPieceCounter.addAndReturnNew(listOf(next))
    val numOfHead = needPieceCounter.blockStream.count() + 1

    return generator.blocksStream().parallel()
            .filter {
                val pieceCounter = PieceCounter(it.blockStream().limit(numOfHead))
                pieceCounter.containsAll(needPieceCounter)
            }
            .map {
                val pieces = it.pieces
                for (entry in needPieceCounter.enumMap.entries)
                    for (x in 0 until entry.value)
                        pieces.remove(entry.key)
                LongPieces(pieces)
            }
            .collect(Collectors.toSet())
}

private fun move(init: State, next: Piece, factories: Factories): List<State> {
    val maxClearLine = init.maxClearLine
    val minoFactory = factories.minoFactory
    val field = init.field

    val candidate = LockedCandidate(minoFactory, factories.minoShifter, factories.minoRotation, maxClearLine)
    val actions = candidate.search(field, next, maxClearLine)
    return actions.map {
        val freeze = field.freeze(maxClearLine)
        val mino = minoFactory.create(next, it.rotate)
        freeze.put(mino, it.x, it.y)
        val deleteLine = freeze.clearLine()
        State(freeze, maxClearLine - deleteLine)
    }
}

private fun createInvoker(minoFactory: MinoFactory, multiThread: Boolean, maxY: Int = 4): ConcurrentCheckerInvoker {
    val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
    val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
    val reachableThreadLocal = LockedReachableThreadLocal(maxY)
    val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
    if (multiThread)
        return SingleCheckerUsingHoldInvoker(commonObj)
    else {
        val core = Runtime.getRuntime().availableProcessors()
        if (executorService == null)
            executorService = Executors.newFixedThreadPool(core)

        return ConcurrentCheckerUsingHoldInvoker(executorService, commonObj)
    }
}

private fun parseColoredField(field: Field): ColoredField {
    val coloredField = ArrayColoredField(24)
    for (y in 0 until field.maxFieldHeight)
        for (x in 0 until 10)
            if (!field.isEmpty(x, y))
                coloredField.setColorType(ColorType.Gray, x, y)
    return coloredField
}