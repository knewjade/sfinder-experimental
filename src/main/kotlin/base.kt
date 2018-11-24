import common.datastore.MinimalOperationWithKey
import common.datastore.MinoOperationWithKey
import common.datastore.PieceCounter
import common.parser.OperationTransform
import commons.Commons
import core.field.Field
import core.field.FieldFactory
import core.field.FieldView
import core.mino.Mino
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.Rotate
import entry.setup.FieldOperationWithKey
import lib.BooleanWalker
import searcher.pack.separable_mino.AllMinoFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import java.util.stream.Stream

fun main(args: Array<String>) {
    val runner = Runner()
//    runner.test()
    runner.test2()
}

class Runner(private val height: Int = 12) {
    private val minoFactory = MinoFactory()
    private val minoRotation = MinoRotation()
    private val minoShifter = MinoShifter()
    private val checker = Checker(minoFactory, minoRotation, height)

    fun test() {
        // Tをすべての場所に置いたあとまわす
        val candidates = Rotate.values().flatMap { rotate ->
            // 現在のミノ
            val currentMino = minoFactory.create(Piece.T, rotate)

            // 回転後にy軸でもっとも大きく移動する量
            val patterns = minoRotation.getLeftPatternsFrom(currentMino)
            val maxPatternY = patterns.map { it[1] }.max()!!

            (-currentMino.minY until height - currentMino.maxY - maxPatternY).flatMap { py ->
                (-currentMino.minX until 10 - currentMino.maxX).flatMap { px ->
                    parseLeftCandidates(currentMino, px, py)
                }
            } + (-currentMino.minY until height - currentMino.maxY - maxPatternY).flatMap { py ->
                (-currentMino.minX until 10 - currentMino.maxX).flatMap { px ->
                    parseRightCandidates(currentMino, px, py)
                }
            }
        }

        val colorize = Colorize(height)
        candidates.forEach {
            if (it.requiredBlock.isPerfect) {
                return@forEach
            }
            println(it.current)
            println(it.next)
            println(FieldView.toString(it.requiredBlock))

            val notAllowBlock = it.effectBlock.freeze(height)
            notAllowBlock.reduce(it.requiredBlock)
            notAllowBlock.put(it.current.mino, it.current.x, it.current.y)
            notAllowBlock.put(it.next.mino, it.next.x, it.next.y)

            println("*")
            println(FieldView.toString(notAllowBlock))

            val stream = colorize.run(it.requiredBlock, notAllowBlock)
            val k = stream.count()
            println(k)
            println("##")
            return
        }
    }

    fun test2() {
        val requiredBlock = FieldFactory.createField("" +
                "__X_______" +
                "__X_______" +
                "__________" +
                "__X_______"
        )
        val notAllowBlock = FieldFactory.createField("" +
                "_X________" +
                "XXX_______" +
                "_X________"
        )

        val colorize = Colorize2(height)
        val list = colorize.run(requiredBlock)
        println(list.size)

        list
                .filter { notAllowBlock.canMerge(it.toField(height)) }
                .forEach { println(FieldView.toString(it.toField(height))) }
    }

    private fun parseLeftCandidates(currentMino: Mino, px: Int, py: Int): List<Candidate> {
        val patterns = minoRotation.getLeftPatternsFrom(currentMino)

        // effectBlock // 回転に影響を与えるブロック // 回転前のミノは取り除く
        val (effectBlock, maps) = checker.rotateLeft(currentMino, px, py)
        val nextRotate = currentMino.rotate.leftRotate

        return maps
                .filterKeys { 0 <= it }
                .flatMap { entry ->
                    val index = entry.key
                    val fields = entry.value
                    fields.mapNotNull { requiredBlock ->
                        val pattern = patterns[index]

                        val piece = currentMino.piece
                        val nextMino = minoFactory.create(piece, nextRotate)
                        val current = MinimalOperationWithKey(currentMino, px, py, 0L)
                        val next = MinimalOperationWithKey(nextMino, px + pattern[0], py + pattern[1], 0L)
                        parseCandidate(current, next, effectBlock, requiredBlock)
                    }
                }
    }

    private fun parseRightCandidates(currentMino: Mino, px: Int, py: Int): List<Candidate> {
        val patterns = minoRotation.getLeftPatternsFrom(currentMino)

        val (effectBlock, maps) = checker.rotateRight(currentMino, px, py)
        val nextRotate = currentMino.rotate.rightRotate

        return maps
                .filterKeys { 0 <= it }
                .flatMap { entry ->
                    val index = entry.key
                    val fields = entry.value
                    fields.mapNotNull { needBlock ->
                        val pattern = patterns[index]

                        val piece = currentMino.piece
                        val nextMino = minoFactory.create(piece, nextRotate)
                        val current = MinimalOperationWithKey(currentMino, px, py, 0L)
                        val next = MinimalOperationWithKey(nextMino, px + pattern[0], py + pattern[1], 0L)
                        parseCandidate(current, next, effectBlock, needBlock)
                    }
                }
    }

    private fun parseCandidate(
            current: MinimalOperationWithKey,
            next: MinimalOperationWithKey,
            effectBlock: Field,
            requiredBlock: Field
    ): Candidate? {
        // すべてのブロック  // effectBlock + T回転前のブロック
        val allBlock = effectBlock.freeze(height)
        allBlock.put(current.mino, current.x, current.y)

        // effectBlock外のTの隅
        val noEffectField = FieldFactory.createField(height)
        val ones = listOf(-1, 1)
        ones.forEach { dy ->
            ones.forEach { dx ->
                val x = next.x + dx
                val y = next.y + dy
                if (x in 0..9 && y in 0..height) {
                    noEffectField.setBlock(x, y)
                }
            }
        }
        noEffectField.reduce(allBlock)

        // requiredBlock + Tスピン用のブロック
        val require = requiredBlock.freeze(height)
        require.merge(noEffectField)

        if (Commons.isTSpin(require, next.x, next.y)) {
            val field = FieldFactory.createField(height)
            (next.y + next.mino.minY..next.y + next.mino.maxY).forEach { field.fillLine(it) }
            field.reduce(allBlock)
            field.merge(requiredBlock)
            field.put(next.mino, next.x, next.y)

            val clearedLine = field.freeze(height).clearLine()
            if (0 < clearedLine) {
                return Candidate(current, next, effectBlock, requiredBlock)
            }
        }

        return null
    }
}

class Checker(
        private val minoFactory: MinoFactory,
        private val minoRotation: MinoRotation,
        private val height: Int
) {
    fun rotateLeft(current: Mino, x: Int, y: Int): Pair<Field, Map<Int, List<Field>>> {
        // 回転後の移動量
        val patterns: Map<Position, Int> = minoRotation.getLeftPatternsFrom(current)
                .mapIndexed { index, pattern -> Position(pattern[0], pattern[1]) to index }
                .toMap()

        // 左回転後のミノ
        val leftMino = minoFactory.create(current.piece, current.rotate.leftRotate)

        // 回転に影響を与えるブロック // 回転前のミノは取り除く
        val field = FieldFactory.createField(height)
        for (pattern in patterns.keys) {
            // 回転前のミノを取り除く // 回転前にミノを置けるようにする
            field.remove(current, x, y)

            // 回転後のブロックを埋める
            for (position in leftMino.positions) {
                val dx = x + pattern.x + position[0]
                val dy = y + pattern.y + position[1]
                if (dx in 0 until 10 && dy in 0 until height) {
                    field.setBlock(dx, dy)
                }
            }
        }

        // 影響のあるブロックを座標に変換する
        val blocks = (0 until 10)
                .flatMap { py ->
                    (0 until field.maxFieldHeight).map { px -> Position(px, py) }
                }
                .filter { !field.isEmpty(it.x, it.y) }

        // 回転先とフィールドを紐付ける
        return field to BooleanWalker.walk(blocks.size)
                .map { booleans ->
                    val blockField = FieldFactory.createField(height)
                    booleans.zip(blocks)
                            .filter { it.first }
                            .map { it.second }
                            .forEach { blockField.setBlock(it.x, it.y) }
                    blockField
                }
                .filter { it.canPut(current, x, y) }
                .collect(Collectors.groupingBy {
                    val pattern = minoRotation.getKicksWithLeftRotation(it, current, leftMino, x, y)
                    if (pattern == null) -1 else patterns[Position(pattern[0], pattern[1])]
                })
    }

    fun rotateRight(current: Mino, x: Int, y: Int): Pair<Field, Map<Int, List<Field>>> {
        // 回転後の移動量
        val patterns: Map<Position, Int> = minoRotation.getRightPatternsFrom(current)
                .mapIndexed { index, pattern -> Position(pattern[0], pattern[1]) to index }
                .toMap()

        // 右回転後のミノ
        val rightMino = minoFactory.create(current.piece, current.rotate.rightRotate)

        // 回転に影響を与えるブロック
        val field = FieldFactory.createField(height)
        for (pattern in patterns.keys) {
            // 回転前のミノを取り除く // 回転前にミノを置けるようにする
            field.remove(current, x, y)

            // 回転後のブロックを埋める
            for (position in rightMino.positions) {
                val dx = x + pattern.x + position[0]
                val dy = y + pattern.y + position[1]
                if (dx in 0 until 10 && dy in 0 until height) {
                    field.setBlock(dx, dy)
                }
            }
        }

        // 影響のあるブロックを座標に変換する
        val blocks = (0 until 10)
                .flatMap { py ->
                    (0 until field.maxFieldHeight).map { px -> Position(px, py) }
                }
                .filter { !field.isEmpty(it.x, it.y) }

        // 回転先とフィールドを紐付ける
        return field to BooleanWalker.walk(blocks.size)
                .map { booleans ->
                    val blockField = FieldFactory.createField(height)
                    booleans.zip(blocks)
                            .filter { it.first }
                            .map { it.second }
                            .forEach { blockField.setBlock(it.x, it.y) }
                    blockField
                }
                .filter { it.canPut(current, x, y) }
                .collect(Collectors.groupingBy {
                    val pattern = minoRotation.getKicksWithRightRotation(it, current, rightMino, x, y)
                    if (pattern == null) -1 else patterns[Position(pattern[0], pattern[1])]
                })
    }
}

data class Position(val x: Int, val y: Int)

class Candidate(
        val current: MinimalOperationWithKey,
        val next: MinimalOperationWithKey,
        val effectBlock: Field,
        val requiredBlock: Field
)

class Colorize(private val height: Int = 12) {
    private val minoFactory = MinoFactory()
    private val minoShifter = MinoShifter()
    private val allOperations: Array<FieldOperationWithKey>

    private val allPieceCounter = PieceCounter(Piece.valueList())
    private val eachPieceCounter: EnumMap<Piece, PieceCounter>

    init {
        allOperations = AllMinoFactory(minoFactory, minoShifter, 10, height, Long.MAX_VALUE).create()
                .map { FieldOperationWithKey(it) }
                .toTypedArray()
        eachPieceCounter = EnumMap(Piece.valueList().map { it to PieceCounter(listOf(it)) }.toMap())
    }

    fun run(requiredBlock: Field, notAllowBlock: Field): Stream<Result2> {
        return run(requiredBlock, notAllowBlock, PieceCounter.EMPTY, EmptyResult2(height), 0)
    }

    private fun run(requiredBlock: Field, notAllowBlock: Field, pieceCounter: PieceCounter, result: Result2, startIndex: Int): Stream<Result2> {
        // すべてのブロックを置ききったとき
        if (requiredBlock.isPerfect) {
            return Stream.of(result)
        }

        // すべてのPieceを使い切ったとき
        if (pieceCounter.containsAll(allPieceCounter)) {
            return Stream.empty()
        }

        var results = Stream.empty<Result2>()

        for (index in startIndex until allOperations.size) {
            val operationWithKey = allOperations[index]
            val currentPieceCounter = eachPieceCounter[operationWithKey.piece]

            // そのPieceが使用されているときはスキップ
            if (pieceCounter.containsAll(currentPieceCounter)) {
                continue
            }

            // 必要なブロックを消費する
            // 置けないブロックと重ならない
            val field = operationWithKey.field
            if (!requiredBlock.canMerge(field) && notAllowBlock.canMerge(field)) {
                val newPieceCounter = pieceCounter.addAndReturnNew(currentPieceCounter)

                val freeze = requiredBlock.freeze(height)
                freeze.reduce(field)

                val k = notAllowBlock.freeze(height)
                k.merge(field)

                val nextResult = RecursiveResult2(result, operationWithKey)

                val stream = run(freeze, k, newPieceCounter, nextResult, index + 1)
                results = Stream.concat(results, stream)
            }
        }

        return results
    }
}

class Colorize2(private val height: Int = 12) {
    private val minoFactory = MinoFactory()
    private val minoShifter = MinoShifter()
    private val allOperations: List<FieldOperationWithKey>

    private val maps = ConcurrentHashMap<Field, List<Result2>>()

    private val eachPieceCounter: EnumMap<Piece, PieceCounter>

    init {
        val pivotField = FieldFactory.createField(height)
        (0 until height).forEach { pivotField.setBlock(4, it) }

        allOperations = AllMinoFactory(minoFactory, minoShifter, 10, height, 0).create()
                .map { FieldOperationWithKey(it) }
                .filter { !it.field.canMerge(pivotField) }
                .toList()
        eachPieceCounter = EnumMap(Piece.valueList().map { it to PieceCounter(listOf(it)) }.toMap())
        maps[FieldFactory.createField(height)] = Collections.singletonList(EmptyResult2(height))
    }

    fun run(field: Field): List<SlideResult2> {
        var minX: Int? = null
        for (x in 0 until 10) {
            val exists = (0 until height).any { !field.isEmpty(x, it) }
            if (exists) {
                minX = x
                break
            }
        }

        if (minX == null) {
            throw IllegalStateException("no block")
        }

        val freeze = field.freeze(height)
        freeze.slideRight(9 - minX)
        freeze.slideLeft(5)

        println(FieldView.toString(freeze))

        return innerRunner(freeze).map { SlideResult2(it, minX - 4) }
    }

    private fun innerRunner(requiredBlock: Field): List<Result2> {
        val cache = maps[requiredBlock]
        if (cache != null) {
            return cache
        }

        val results = allOperations
                .parallelStream()
                .flatMap { operation ->
                    val operationField = operation.field
                    if (requiredBlock.canMerge(operationField)) {
                        return@flatMap Stream.empty<Result2>()
                    }

                    val freeze = requiredBlock.freeze(height)
                    freeze.reduce(operationField)

                    val currentPieceCounter = eachPieceCounter[operation.piece]!!

                    innerRunner(freeze)
                            .filter { !it.pieceCounter.containsAll(currentPieceCounter) }
                            .filter { it.field.canMerge(operationField) }
                            .map { RecursiveResult2(it, operation) }.stream()
                }
                .collect(Collectors.toList())

        maps[requiredBlock] = results

        return results
    }
}

interface Result2 {
    val field: Field
    val size: Int
    val pieceCounter: PieceCounter
    val operations: Stream<FieldOperationWithKey>
}

class EmptyResult2(private val height: Int) : Result2 {
    override val field: Field
        get() {
            return FieldFactory.createField(height)
        }

    override val size = 0
    override val pieceCounter = PieceCounter.EMPTY

    override val operations: Stream<FieldOperationWithKey>
        get() {
            return Stream.empty<FieldOperationWithKey>()
        }
}

class RecursiveResult2(
        private val result: Result2,
        private val operationWithKey: FieldOperationWithKey
) : Result2 {
    override val field: Field
        get() {
            val prevField = result.field
            val operation = operationWithKey.operation
            val mino = operation.mino
            prevField.put(mino, operation.x, operation.y)
            return prevField
        }

    override val size: Int
        get() {
            return result.size + 1
        }

    override val pieceCounter: PieceCounter
        get() {
            val pieceCounter = result.pieceCounter
            return pieceCounter.addAndReturnNew(Stream.of(operationWithKey.piece))
        }

    override val operations: Stream<FieldOperationWithKey>
        get() {
            return Stream.concat(result.operations, Stream.of(operationWithKey))
        }
}

class SlideResult2(
        private val result: Result2,
        private val slideX: Int
) {
    val operations: Stream<MinoOperationWithKey>
        get () {
            return result.operations.map { SlideMinoOperationWithKey(it.operation, slideX) }
        }

    fun toField(height: Int): Field {
        val operations = this.operations.collect(Collectors.toList())
        return OperationTransform.parseToField(operations, height)
    }
}

class SlideMinoOperationWithKey(private val operation: MinoOperationWithKey, private val slideX: Int) : MinoOperationWithKey {
    override fun getRotate(): Rotate {
        return operation.rotate
    }

    override fun getNeedDeletedKey(): Long {
        return operation.needDeletedKey
    }

    override fun getUsingKey(): Long {
        return operation.usingKey
    }

    override fun getMino(): Mino {
        return operation.mino
    }

    override fun getPiece(): Piece {
        return operation.piece
    }

    override fun getX(): Int {
        return operation.x + slideX
    }

    override fun getY(): Int {
        return operation.y
    }
}