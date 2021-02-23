import common.datastore.Operations
import components.LockedCandidateMoverThreadLocal
import components.Mover
import concurrent.LockedCandidateThreadLocal
import core.field.Field
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation

class TSpinFinderThreadLocal(
    private val requiredClearLines: Int,
    private val minoFactory: MinoFactory,
    private val minoShifter: MinoShifter,
    private val minoRotation: MinoRotation,
    private val moverThreadLocal: ThreadLocal<Mover>,
    private val height: Int
) : ThreadLocal<TSpinFinder>() {
    override fun initialValue(): TSpinFinder {
        val mover = moverThreadLocal.get()
        return TSpinFinder(requiredClearLines, minoFactory, minoShifter, minoRotation, mover, height)
    }

    companion object {
        fun create(requiredClearLines: Int, height: Int): TSpinFinderThreadLocal {
            val minoFactory = MinoFactory()
            val minoShifter = MinoShifter()
            val minoRotation = MinoRotation.create()

            val lockedCandidateThreadLocal = LockedCandidateThreadLocal(height)
            val moverThreadLocal = LockedCandidateMoverThreadLocal(minoFactory, lockedCandidateThreadLocal, height)

            return TSpinFinderThreadLocal(
                requiredClearLines, minoFactory, minoShifter, minoRotation, moverThreadLocal, height
            )
        }
    }
}

class TSpinFinder(
    requiredClearLines: Int,
    minoFactory: MinoFactory,
    minoShifter: MinoShifter,
    minoRotation: MinoRotation,
    private val mover: Mover,
    height: Int
) {
    private val possibleValidator = TSpinPossibleValidator(requiredClearLines, height)
    private val justValidator = TSpinJustValidator(requiredClearLines, minoFactory, minoShifter, minoRotation, height)

    // 与えられたツモに対して、ホールドなしでTスピンできる手順を列挙する
    // Tスピン以外でライン消去が発生する手順は取り除く
    // ホールドなしなので、ツモはすべて消費される
    // @param piecesWithoutT Tミノの前のツモ順。このツモをすべて使い切ったあとに、Tミノを受け取れる
    // @returns Tスピンの手順のリスト。Tミノの手順は含まれないため、手順の数はpiecesWithoutT.sizeと同じになる
    fun searchWithoutHold(initField: Field, piecesWithoutT: List<Piece>): List<Operations> {
        assert(piecesWithoutT.isNotEmpty())

        val firstPiece = piecesWithoutT[0]
        var operations = mover.moveWithoutClearNoHold(initField, firstPiece, null)

        (1 until piecesWithoutT.size).forEach { index ->
            val piece = piecesWithoutT[index]

            operations = operations
                .flatMap { op ->
                    val field = op.field(initField)
                    mover.moveWithoutClearNoHold(field, piece, op)
                }
                .filter { op ->
                    val field = op.field(initField)
                    possibleValidator.validate(field, 6 - index)
                }
                .distinctBy { op ->
                    op.field(initField)
                }
        }

        return operations
            .filter { op ->
                val field = op.field(initField)
                justValidator.validate(field)
            }
            .map { op ->
                Operations(op.operations())
            }
    }
}


