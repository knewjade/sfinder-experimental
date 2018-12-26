package main

import common.datastore.Operations
import common.datastore.SimpleOperation
import common.datastore.action.Action
import common.datastore.order.Order
import common.parser.OperationInterpreter
import common.pattern.LoadedPatternGenerator
import core.action.candidate.LockedCandidate
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import entry.path.output.MyFile
import searcher.PutterReuseNoHold
import searcher.TSpinSearchValidator
import searcher.checkmate.CheckmateDataPool
import searcher.core.FullSearcherCore
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

    val maxHeight = 15
    val maxPieceNum = 6
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation()

    val candidate = LockedCandidate(minoFactory, minoShifter, minoRotation, maxHeight)

    val validator = TSpinSearchValidator(minoFactory, minoShifter, minoRotation, maxHeight, maxPieceNum)

    val dataPool = CheckmateDataPool()
    val searcherCore = FullSearcherCore<Action, Order>(minoFactory, validator, dataPool)
    val putter = PutterReuseNoHold(dataPool, searcherCore)

    allPieces
            .map { it.pieces }
            .sortedWith(Comparator { o1, o2 ->
                o1.forEachIndexed { index, piece1 ->
                    val piece2 = o2[index]
                    if (piece1 != piece2) {
                        return@Comparator piece1.number - piece2.number
                    }
                }
                0
            })
            .forEach { pieces ->
                val name = pieces.joinToString("") { it.name }
                println(name)

                putter.first(FieldFactory.createField(maxHeight), pieces, candidate, maxHeight, pieces.size)

                val results = putter.results
                println("results=${results.size}")
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
}
