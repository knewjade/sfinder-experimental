package main

import common.datastore.Operations
import common.datastore.Result
import common.datastore.SimpleOperation
import common.datastore.action.Action
import common.datastore.order.Order
import common.parser.OperationInterpreter
import common.pattern.LoadedPatternGenerator
import core.action.candidate.LockedCandidate
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import entry.path.output.MyFile
import searcher.PutterNoHold
import searcher.TSpinSearchValidator
import searcher.checkmate.CheckmateDataPool
import searcher.core.FullSearcherCore
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.stream.Collectors

fun main(args: Array<String>) {
    run("I")
}

fun run(args: Array<String>) {
    val headPiece = args[0]
    println("headPiece=$headPiece")

    run(headPiece)
}

fun run(headPiece: String) {
    val pattern = "$headPiece[^${headPiece}T]!"
    println("pattern=$pattern")

    val generator = LoadedPatternGenerator(pattern)
    val allPieces = generator.blocksStream().collect(Collectors.toList())
    println("allPieces=${allPieces.size}")

    val thread = 4
    println("thread=$thread")

    val executorService = Executors.newFixedThreadPool(thread)

    val maxHeight = 15
    allPieces
            .mapIndexed { index, piecesObj ->
                val pieces = piecesObj.pieces
                val name = pieces.joinToString("") { it.name }

                val callable = Callable<List<Result>> {
                    println("$index: $pieces")

                    val runner = Runner(maxHeight)
                    runner.run(pieces)
                }
                name to executorService.submit(callable)
            }
            .forEach {
                val name = it.first
                val results = it.second.get()

                println(name)

                MyFile("output/$name").newAsyncWriter().use { writer ->
                    results.forEach { result ->
                        val history = result.order.history
                        val action = result.lastAction
                        val lastPiece = result.lastPiece

                        val operations = history.operationStream.collect(Collectors.toList())
                        operations.add(SimpleOperation(lastPiece, action.rotate, action.x, action.y))

                        val line = OperationInterpreter.parseToString(Operations(operations))
                        writer.writeAndNewLine(line)
                    }
                }
            }

    executorService.shutdown()
}

class Runner(private val maxHeight: Int) {
    private val minoFactory = MinoFactory()
    private val minoShifter = MinoShifter()
    private val minoRotation = MinoRotation()

    fun run(pieces: List<Piece>): List<Result> {
        val validator = TSpinSearchValidator(minoFactory, minoShifter, minoRotation, maxHeight, pieces.size)

        val dataPool = CheckmateDataPool()
        val searcherCore = FullSearcherCore<Action, Order>(minoFactory, validator, dataPool)
        val putter = PutterNoHold(dataPool, searcherCore)

        val candidate = LockedCandidate(minoFactory, minoShifter, minoRotation, maxHeight)
        putter.first(FieldFactory.createField(maxHeight), pieces, candidate, maxHeight, pieces.size)

        return putter.results
    }
}