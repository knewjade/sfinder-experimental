package tsd_opener

import common.datastore.PieceCounter
import common.parser.OperationWithKeyInterpreter
import concurrent.RotateReachableThreadLocal
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import searcher.spins.FirstPreSpinRunner
import searcher.spins.FullSpinRunner
import searcher.spins.SecondPreSpinRunner
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation.create()

    val fieldHeight = 8
    val initField = FieldFactory.createField("" +
            "__________" +
            "__________" +
            "__________" +
            "__________" +
            "__________" +
            "", fieldHeight
    )
    val pieceCounter = PieceCounter(Piece.valueList())

    val rotateReachableThreadLocal = RotateReachableThreadLocal(minoFactory, minoShifter, minoRotation, fieldHeight)
    val firstPreSpinRunner = FirstPreSpinRunner(
            minoFactory, minoShifter, rotateReachableThreadLocal, 0, 4, 6, fieldHeight
    )
    val secondPreSpinRunner = SecondPreSpinRunner(firstPreSpinRunner, initField, pieceCounter, Integer.MAX_VALUE)
    val runner = FullSpinRunner()
    val results = runner.search(secondPreSpinRunner, 2).parallel().collect(Collectors.toList())

    val operationsList = results
            .sortedBy {
                val op = it.operationT
                val score = fieldHeight * 10 * ((op.rotate.number + 2) % 4) + 10 * op.y + op.x
                if (initField.isOnGround(op.mino, op.x, op.y)) {
                    score
                } else {
                    score + 1000
                }
            }
            .map { it.result.operationStream().collect(Collectors.toList()) }

    PrintWriter(BufferedWriter(FileWriter("figs/tsd"))).use {
        operationsList.forEachIndexed { index, operations ->
            val line = OperationWithKeyInterpreter.parseToString(operations)
            it.println("$index:$line")
        }
    }
}
