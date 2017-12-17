package main

import common.datastore.action.Action
import common.pattern.LoadedPatternGenerator
import common.tetfu.Tetfu
import common.tetfu.TetfuPage
import common.tetfu.common.ColorConverter
import common.tetfu.common.ColorType
import concurrent.LockedCandidateThreadLocal
import concurrent.LockedReachableThreadLocal
import concurrent.checker.CheckerUsingHoldThreadLocal
import concurrent.checker.invoker.CheckerCommonObj
import concurrent.checker.invoker.using_hold.SingleCheckerUsingHoldInvoker
import core.field.Field
import core.field.FieldFactory
import core.mino.Mino
import core.mino.MinoFactory
import java.util.stream.Collectors

fun main(args: Array<String>) {
//    val data = "v115@9gF8DeF8DeF8DeF8NeAgH"
//    val pattern = "*p5"
//    run(data, pattern)
//    run(args[0], args[1])

//    s3.putObject("fumen", "")

//    print("${successCount},${allCount}")
}

private fun run(data: String, pattern: String): Triple<Int, Int, Field> {
    val maxY = 4
    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()

    val tetfu = Tetfu(minoFactory, colorConverter)
    val removePrefixData = Tetfu.removePrefixData(Tetfu.removeDomainData(data))
    val decode = tetfu.decode(removePrefixData)
    val field = parseToField(decode[0], colorConverter, maxY)

    val invoker = createInvoker(minoFactory, maxY)
    val generator = LoadedPatternGenerator(pattern)
    val pieces = generator.blocksStream().collect(Collectors.toList())
    val maxDepth = (maxY * 10 - field.numOfAllBlocks) / 4
    val resultPairs = invoker.search(field, pieces, maxY, maxDepth)

    val allCount = resultPairs.size
    val successCount = resultPairs.count { it.value!! }

    return Triple(allCount, successCount, field)
}

private fun parseToField(page: TetfuPage, colorConverter: ColorConverter, height: Int): Field {
    val coloredField = page.field
    if (page.isPutMino) {
        val mino = Mino(colorConverter.parseToBlock(page.colorType), page.rotate)
        coloredField.putMino(mino, page.x, page.y)
    }

    val empty = ColorType.Empty.number
    val field = FieldFactory.createField(height)
    for (y in 0 until height)
        for (x in 0..9)
            if (coloredField.getBlockNumber(x, y) != empty)
                field.setBlock(x, y)

    return field
}

private fun singleCheckerUsingHoldInvoker() {
    val array = listOf(1, -4, 3, 2, 1)
    val max = array.indexOf(array.maxBy { it * it })
    println(max)

    println("Hello Kotlin!!")
//    val a = 3  // final

    infix fun Int.sum(other: Int): Int = this + other
    println(3 sum 1)  // 4

    println(Char.MAX_HIGH_SURROGATE)

    for (ch in 'a'..'c')
        print(ch)
    // -> abc

    k()
    3.sum(1)
}

fun k(): Int {
//    return 2 sum 3
    return 3
}

private fun createInvoker(minoFactory: MinoFactory, maxY: Int): SingleCheckerUsingHoldInvoker {
    val candidateThreadLocal = LockedCandidateThreadLocal(maxY)
    val checkerThreadLocal = CheckerUsingHoldThreadLocal<Action>()
    val reachableThreadLocal = LockedReachableThreadLocal(maxY)
    val commonObj = CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal)
    return SingleCheckerUsingHoldInvoker(commonObj)
}