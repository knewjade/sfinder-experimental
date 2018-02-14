package best

import common.datastore.MinoOperationWithKey
import common.datastore.Operations
import common.datastore.PieceCounter
import common.datastore.action.Action
import common.iterable.PermutationIterable
import common.order.ForwardOrderLookUp
import common.parser.OperationTransform
import common.pattern.LoadedPatternGenerator
import common.tetfu.Tetfu
import common.tetfu.TetfuPage
import common.tetfu.common.ColorConverter
import common.tetfu.field.ColoredFieldView
import core.action.candidate.LockedCandidate
import core.field.Field
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import entry.path.LockedBuildUpListUpThreadLocal
import helper.EasyTetfu
import helper.Patterns
import main.caller.ContentCaller
import main.domain.Cycle
import main.domain.FieldData
import main.domain.Result
import searcher.PutterNoHold
import searcher.common.validator.PerfectValidator
import searcher.pack.InOutPairField
import searcher.pack.SeparableMinos
import searcher.pack.SizedBit
import searcher.pack.calculator.BasicSolutions
import searcher.pack.memento.AllPassedSolutionFilter
import searcher.pack.solutions.OnDemandBasicSolutions
import searcher.pack.task.BasicMinoPackingHelper
import searcher.pack.task.SetupPackSearcher
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors


data class Obj(val height: Int = 4) {
    val minoFactory = MinoFactory()
    val colorConverter = ColorConverter()

    val minoShifter = MinoShifter()
    val sizedBit = SizedBit(3, height)
    val taskResultHelper = BasicMinoPackingHelper()
    val solutionFilter = AllPassedSolutionFilter()
    val buildUpStreamThreadLocal = LockedBuildUpListUpThreadLocal(height)

    val minoRotation = MinoRotation()
    val perfectValidator = PerfectValidator()
}

fun getPieceCounters(cycle: Cycle): Set<PieceCounter> {
    val patterns = Patterns.hold(cycle.number)
    val generator = LoadedPatternGenerator(patterns)
    val lookUp = ForwardOrderLookUp(4, true)

    val set = generator.blocksStream()
            .map { it.pieces.subList(0, 5) }
            .flatMap { lookUp.parse(it) }
            .map { PieceCounter(it) }
            .collect(Collectors.toSet())

    return set
}

fun getBest(pieceCounter: PieceCounter, cycle: Cycle): Pair<Result, Int>? {
    val blocks = pieceCounter.blocks
    val iterable = PermutationIterable(blocks, blocks.size)
    var max = -1

    val bestResult = iterable.map {
        val file = File("input/fumens/" + cycle.number + "/" + it.joinToString("") { it.name })
        if (file.isDirectory) {
            val results = file.listFiles().map {
                val content = it.readText(StandardCharsets.UTF_8)
                val caller = ContentCaller(content)
                caller.call()
            }

            // verify
            assert(results.map { it.allCount }.toSet().size == 1, { results })
            max = results[0].allCount.value

            // max in directory
            results.flatMap { it.details }.maxBy { it.success.value }
        } else {
            null
        }
    }.filterNotNull().maxBy { it.success.value }

    return bestResult?.let { it to max }
}

fun colorize(pieceCounter: PieceCounter, monoFieldData: FieldData, obj: Obj): String {
    val minoFactory = obj.minoFactory
    val colorConverter = obj.colorConverter
    val minoShifter = obj.minoShifter
    val sizedBit = obj.sizedBit
    val solutionFilter = obj.solutionFilter
    val taskResultHelper = obj.taskResultHelper

    val pages = Tetfu(minoFactory, colorConverter).decode(monoFieldData.raw)
    val page = pages[0]

    val field = getField(page, pieceCounter, obj)

    // ミノリストの作成
    val notFilledField = field.freeze(4)
    notFilledField.inverse()
    val deleteKeyMask = getDeleteKeyMask(notFilledField, 4)
    val separableMinos = SeparableMinos.createSeparableMinos(minoFactory, minoShifter, sizedBit, deleteKeyMask)

    // 絶対に置かないブロック
    val inOutPairFields = InOutPairField.createInOutPairFields(sizedBit, notFilledField)

    // 絶対に置く必要があるブロック
    val basicSolutions = ArrayList<BasicSolutions>()
    val needFillFields = InOutPairField.createInnerFields(sizedBit, field)
    for (innerField in needFillFields) {
        val solutions = OnDemandBasicSolutions(separableMinos, sizedBit, innerField.getBoard(0))
        basicSolutions.add(solutions)
    }

    // 探索
    val searcher = SetupPackSearcher(inOutPairFields, basicSolutions, sizedBit, solutionFilter, taskResultHelper, needFillFields)
    val result = getResult(FieldFactory.createField(4), searcher, pieceCounter, obj)

    val operations = result!!.memento.getOperationsStream(sizedBit.width).collect(Collectors.toList())
    val encodeUrl = EasyTetfu().encodeUrl(FieldFactory.createField(4), operations, 4)

    return encodeUrl.removePrefix("http://fumen.zui.jp/?v115@")
}

private fun getField(page: TetfuPage, pieceCounter: PieceCounter, obj: Obj): Field {
    val fieldData = ColoredFieldView.toString(page.field, obj.height, "").replace('0', ' ')
    val field = FieldFactory.createField(fieldData)
    if (page.comment == "4") {
        return field
    } else {
        return restoreField(obj, pieceCounter, field)
    }
}

private fun restoreField(obj: Obj, pieceCounter: PieceCounter, field: Field): Field {
    val putter = PutterNoHold<Action>(obj.minoFactory, obj.perfectValidator)
    val lockedCandidate = LockedCandidate(obj.minoFactory, obj.minoShifter, obj.minoRotation, obj.height)

    for (blocks in PermutationIterable(pieceCounter.blocks, pieceCounter.blocks.size)) {
        val first = putter.first(FieldFactory.createField(obj.height), blocks, lockedCandidate, obj.height, 10)

        first.filter { field == it.field }
                .forEach {
                    val operations = Operations(it.history.operationStream)
                    val initField = FieldFactory.createField(4)
                    val operationWithKeys = OperationTransform.parseToOperationWithKeys(initField, operations, obj.minoFactory, obj.height)

                    val newField = FieldFactory.createField(obj.height)
                    for (operationWithKey in operationWithKeys) {
                        val fieldForMino = FieldFactory.createField(obj.height)
                        fieldForMino.put(operationWithKey.mino, operationWithKey.x, operationWithKey.y)
                        fieldForMino.insertWhiteLineWithKey(operationWithKey.needDeletedKey)

                        newField.merge(fieldForMino)
                    }

                    return newField
                }
    }

    error("No reachable: ${pieceCounter} " + FieldView.toString(field))
}

private fun getDeleteKeyMask(notFilledField: Field, maxHeight: Int): Long {
    val freeze = notFilledField.freeze(maxHeight)
    freeze.inverse()
    return freeze.clearLineReturnKey()
}

fun getResult(initField: Field, searcher: SetupPackSearcher, pieceCounter: PieceCounter, obj: Obj): searcher.pack.task.Result? {
    val sizedBit = obj.sizedBit
    val buildUpStreamThreadLocal = obj.buildUpStreamThreadLocal

    val results = searcher.toList()
    val result = results.find { result ->
        val collector = Collectors.toCollection(Supplier<LinkedList<MinoOperationWithKey>> { LinkedList() })
        val operationWithKeys: LinkedList<MinoOperationWithKey> = result.getMemento()
                .getSeparableMinoStream(sizedBit.width)
                .map { it.toMinoOperationWithKey() }
                .collect(collector);

        val usingPiece = PieceCounter(operationWithKeys.map { it.mino.piece })
        if (!pieceCounter.equals(usingPiece))
            return@find false

        // 地形の中で組むことができるoperationsを一つ作成
        val buildUpStream = buildUpStreamThreadLocal.get()
        val sampleOperations = buildUpStream.existsValidBuildPatternDirectly(initField, operationWithKeys)
                .findFirst()
                .orElse(emptyList())

        // 地形の中で組むことができるものがないときはスキップ
        !sampleOperations.isEmpty()
    }

    if (result == null) {
        println(FieldView.toString(initField))
        println(pieceCounter)
    }

    return result
}