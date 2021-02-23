package components

import common.SpinChecker
import common.datastore.*
import common.iterable.PermutationIterable
import common.parser.OperationTransform
import concurrent.LockedReachableThreadLocal
import core.action.reachable.LockedReachable
import core.action.reachable.Reachable
import core.field.Field
import core.field.FieldFactory
import core.field.KeyOperators
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.MinoRotationDetail
import java.lang.Long.bitCount
import java.util.*

class LockedReachableExpanderThreadLocal(
    private val minoFactory: MinoFactory,
    private val minoShifter: MinoShifter,
    private val minoRotation: MinoRotation,
    private val lockedReachableThreadLocal: LockedReachableThreadLocal,
    private val height: Int
) : ThreadLocal<Expander>() {
    override fun initialValue(): Expander {
        val lockReachable = lockedReachableThreadLocal.get()
        return Expander(minoFactory, minoShifter, minoRotation, lockReachable, height)
    }

    companion object {
        fun create(height: Int): LockedReachableExpanderThreadLocal {
            val minoFactory = MinoFactory()
            val minoShifter = MinoShifter()
            val minoRotation = MinoRotation.create()
            val lockedReachableThreadLocal = LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, height)
            return LockedReachableExpanderThreadLocal(
                minoFactory, minoShifter, minoRotation, lockedReachableThreadLocal, height
            )
        }
    }
}

class Expander(
    minoFactory: MinoFactory,
    minoShifter: MinoShifter,
    minoRotation: MinoRotation,
    reachable: Reachable,
    private val height: Int
) {
    private val mover = MoverForExpand(minoFactory, minoShifter, height)
    private val minoRotationDetail = MinoRotationDetail(minoFactory, minoRotation)
    private val lockedReachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)
    private val checker = SpinChecker(minoFactory, minoRotationDetail, lockedReachable)
    private val builder = TSpinBuilder(reachable, height, checker)
    private val indexer: Indexer = Indexer(minoFactory, height)

    // Tスピンを維持したまま、未使用のミノを置いた手順に拡張する
    fun moveWithTSpin(
        initField: Field, operations: MinoOperationWithKeysList, unusedPieces: PieceCounter
    ): List<MinoOperationWithKeysList> {
        val field = operations.field(initField, height)
        val filledLine = field.filledLine
        val clearLine = bitCount(filledLine)

        // 使っていないミノのすべての組み合わせ
        val moveBitSets = moveDistinctBitSet(field, unusedPieces.blocks, filledLine)

        return moveBitSets
            .map { bitSet ->
                val copied = operations.toMutableList()
                bitSet.stream()
                    .mapToObj {
                        val operation = indexer.toMinoOperationWithKey(it)
                        val minoField = operation.createMinoField(height)
                        if (minoField.usingKey and filledLine == 0L) {
                            // Tスピンのライン消去と重ならない
                            operation
                        } else {
                            OperationTransform.toFullOperationWithKey(
                                operation.mino, operation.x, operation.y, filledLine, height
                            )
                        }
                    }
                    .forEach { copied.add(it) }
                copied
            }
            .map { MinoOperationWithKeysList(it) }
            .filter {
                // 拡張後のクリアラインが増えていないことを確認
                val freeze = it.field(initField, height)
                filledLine == freeze.filledLine
            }
            .filter { expanded ->
                val expandedNoT = MinoOperationWithKeysList(expanded.operationWithKeys.filter { it.piece != Piece.T })
                val freeze = expandedNoT.field(initField, height)
                val requiredClearLines = clearLine - bitCount(freeze.filledLine)
                assert(clearLine in 1..3)
                builder.canBuild(initField, expanded, requiredClearLines)
            }
    }

    // ミノを配置する。Reachableであるかは問わない (あとでチェックする)
    private fun moveDistinctBitSet(initField: Field, pieces: List<Piece>, deletedKey: Long): HashSet<BitSet> {
        val set = HashSet<BitSet>()
        PermutationIterable(pieces, pieces.size).forEach {
            set.addAll(moveBitSet(initField, it, deletedKey))
        }
        return set
    }

    private fun moveBitSet(initField: Field, pieces: List<Piece>, deletedKey: Long): List<BitSet> {
        var buffer = listOf(initField.freeze() to BitSet(indexer.maxIndex))
        pieces.forEach { piece ->
            buffer = moveBitSet(piece, buffer, deletedKey)
        }
        return buffer.map { it.second }
    }

    private fun moveBitSet(
        piece: Piece, buffer: List<kotlin.Pair<Field, BitSet>>, deletedKey: Long
    ): List<kotlin.Pair<Field, BitSet>> {
        return buffer.flatMap { prev ->
            val field = prev.first
            val bitSet = prev.second
            mover.move(field, piece, deletedKey)
                .map {
                    val freeze = field.freeze()
                    freeze.merge(it.createMinoField(height))

                    val copy = bitSet.copy()
                    copy.set(indexer.toIndex(piece, it.rotate, it.x, it.y))

                    freeze to copy
                }
        }
    }
}

fun BitSet.copy(): BitSet {
    val copied = BitSet(this.size())
    copied.or(this)
    return copied
}

class MoverForExpand(
    private val minoFactory: MinoFactory,
    private val minoShifter: MinoShifter,
    private val height: Int
) {
    // @param deletedKey 揃っていることを許容するラインを指定。このラインをまたぐときはミノを分離させる
    fun move(field: Field, piece: Piece, deletedKey: Long): List<MinoOperationWithKey> {
        assert(field.filledLine and deletedKey == deletedKey)

        val freeze = field.freeze()
        freeze.deleteLineWithKey(deletedKey)

        val mask = FieldFactory.createField(height)
        mask.insertBlackLineWithKey(deletedKey)

        val clearedLine = bitCount(deletedKey)

        val operations = mutableListOf<MinoOperationWithKey>()

        minoShifter.getUniqueRotates(piece).forEach { rotate ->
            val mino = minoFactory.create(piece, rotate)
            (-mino.minY until height - mino.maxY).forEach { y ->
                (-mino.minX until 10 - mino.maxX).forEach loop@{ x ->
                    if (field.canPut(mino, x, y)) {
                        // フィールドに置ける
                        if (!field.isOnGround(mino, x, y)) {
                            return@loop
                        }

                        val operation = MinimalOperationWithKey(mino, x, y, 0L)
                        operations.add(operation)
                    } else {
                        // ライン消去分も含めても高さ制限を超えない
                        if (height <= y + mino.maxY + clearedLine) {
                            return@loop
                        }

                        // Tスピンラインと重なっているか
                        if (mask.canPut(mino, x, y)) {
                            return@loop
                        }

                        // Tスピンライン消去後の地形で置くことが
                        if (freeze.canPut(mino, x, y) && freeze.isOnGround(mino, x, y)) {
                            val operation = OperationTransform.toFullOperationWithKey(
                                mino, x, y, deletedKey, height
                            )
                            operations.add(operation)
                        }
                    }
                }
            }
        }

        return operations
    }
}

class TSpinBuilder(
    private val reachable: Reachable,
    private val height: Int,
    private val checker: SpinChecker
) {
    // Tスピンしながら、組み立てられる手順が存在するか確認
    fun canBuild(field: Field, operationWithKeys: MinoOperationWithKeysList, requiredClearLines: Int): Boolean {
        return canBuild(field, LinkedList(operationWithKeys.operationWithKeys), requiredClearLines)
    }

    private fun canBuild(
        fieldOrigin: Field, operationWithKeys: LinkedList<MinoOperationWithKey>, requiredClearLines: Int
    ): Boolean {
        operationWithKeys.sortWith { o1, o2 ->
            val compare = o1.y.compareTo(o2.y)
            if (compare != 0) {
                compare
            } else {
                o1.needDeletedKey.compareTo(o2.needDeletedKey)
            }
        }
        return existsValidBuildPatternRecursiveWithTSpin(
            fieldOrigin.freeze(height), operationWithKeys, requiredClearLines
        )
    }

    private fun existsValidBuildPatternRecursiveWithTSpin(
        field: Field, operationWithKeys: LinkedList<MinoOperationWithKey>, requiredClearLines: Int
    ): Boolean {
        val deleteKey = field.clearLineReturnKey()
        for (index in operationWithKeys.indices) {
            val key = operationWithKeys.removeAt(index)
            val needDeletedKey = key.needDeletedKey
            if (deleteKey and needDeletedKey != needDeletedKey) {
                // 必要な列が消えていない
                operationWithKeys.add(index, key)
                continue
            }

            // すでに下のラインが消えているときは、その分スライドさせる
            val originalY = key.y
            val deletedLines = bitCount(KeyOperators.getMaskForKeyBelowY(originalY) and deleteKey)
            val mino = key.mino
            val x = key.x
            val y = originalY - deletedLines
            if (field.isOnGround(mino, x, y)
                && field.canPut(mino, x, y)
                && reachable.checks(field, mino, x, y, height - mino.minY)
            ) {
                var valid = true
                if (mino.piece == Piece.T) {
                    val freeze = field.freeze()
                    freeze.put(mino, x, y)
                    val clearLine = freeze.clearLine()
                    val operation: Operation = SimpleOperation(mino.piece, mino.rotate, x, y)
                    val spin = checker.check(field, operation, height, clearLine)
                    valid = spin.isPresent && requiredClearLines <= spin.get().clearedLine
                }

                if (valid) {
                    if (operationWithKeys.isEmpty()) {
                        return true
                    }

                    val nextField = field.freeze(height)
                    nextField.put(mino, x, y)
                    nextField.insertBlackLineWithKey(deleteKey)
                    val exists = existsValidBuildPatternRecursiveWithTSpin(
                        nextField, operationWithKeys, requiredClearLines
                    )

                    if (exists) {
                        return true
                    }
                }
            }
            operationWithKeys.add(index, key)
        }
        field.insertBlackLineWithKey(deleteKey)
        return false
    }
}