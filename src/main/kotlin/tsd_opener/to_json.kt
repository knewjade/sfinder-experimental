package tsd_opener

import BuildUp2
import common.parser.OperationWithKeyInterpreter
import common.pattern.LoadedPatternGenerator
import concurrent.HarddropReachableThreadLocal
import concurrent.LockedReachableThreadLocal
import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val isHold = true
    val isHarddrop = true

    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation.create()

    val fieldHeight = 6

    val lockedReachableThreadLocal = LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, fieldHeight)
    val harddropReachableThreadLocal = HarddropReachableThreadLocal(fieldHeight)

    val operationsPairList = Files.readAllLines(Paths.get("figs/tsd"))
            .map { line ->
                val o = line.split(":")
                assert(o.size == 2)
                val (index, operations) = o
                Integer.parseInt(index) to OperationWithKeyInterpreter.parseToList(operations, minoFactory)
            }

    val generator = LoadedPatternGenerator("*!")

    val patterns = generator.blocksStream().collect(Collectors.toList())

    PrintWriter(BufferedWriter(FileWriter("figs/data.json"))).use { writer ->
        writer.println("{")
        patterns.forEach { pieces ->
            val indexes = mutableListOf<Int>()
            operationsPairList.forEach { (index, operations) ->
                val field = FieldFactory.createField(fieldHeight)
                val lockedReachable = lockedReachableThreadLocal.get()
                val harddropReachable = if (isHarddrop) harddropReachableThreadLocal.get() else lockedReachableThreadLocal.get()
                val canBuild = BuildUp2.existsValidByOrderWithHold(
                        lockedReachable, isHold, field, operations.stream(), pieces.pieces, fieldHeight, harddropReachable, operations.size
                )
                if (canBuild) {
                    indexes.add(index)
                }
            }

            val names = pieces.pieces.joinToString("") { it.name }
            val values = indexes.joinToString(",") { it.toString() }

            writer.println("\"$names\": [$values],")
        }
        writer.println("}")
    }
}