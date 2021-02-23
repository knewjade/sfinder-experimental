import common.SpinChecker
import common.datastore.SimpleOperation
import core.action.candidate.RotateCandidate
import core.action.reachable.LockedReachable
import core.field.Field
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.MinoRotationDetail
import searcher.spins.spin.TSpins

class TSpinPossibleValidator(
    private val requiredClearLines: Int,
    private val height: Int
) {
    // `unusedPiece` 分のミノを使って積み込み、そのあとTスピンできる可能性があるなら `true` を返却する
    // @param unusedPiece まだ置いていないミノの数。TスピンをするTミノもカウントに含む
    fun validate(field: Field, unusedPiece: Int): Boolean {
        val spinHeight = requiredClearLines
        (0..height - spinHeight).forEach { lowerY ->
            val freeze = field.freeze()
            if (0 < lowerY) {
                freeze.slideDown(lowerY)
            }
            val least = if (freeze.isPerfect) {
                (height * 10 + 3) / 4  // ceil
            } else {
                leastPieces(freeze, spinHeight)
            }

            if (least <= unusedPiece) {
                return true
            }
        }

        return false
    }

    private fun leastPieces(field: Field, spinHeight: Int): Int {
        var counter = 0
        var sum = spinHeight - field.getBlockCountBelowOnX(0, spinHeight)
        for (x in 1 until 10) {
            val emptyCountInColumn = spinHeight - field.getBlockCountBelowOnX(x, spinHeight)
            if (field.isWallBetweenLeft(x, spinHeight)) {
                counter += (sum + 3) / 4  // ceil
                sum = emptyCountInColumn
            } else {
                sum += emptyCountInColumn
            }
        }
        counter += (sum + 3) / 4  // ceil
        return counter
    }
}

class TSpinJustValidator(
    private val requiredClearLines: Int,
    private val minoFactory: MinoFactory,
    minoShifter: MinoShifter,
    minoRotation: MinoRotation,
    private val height: Int
) {
    private val rotateCandidate = RotateCandidate(minoFactory, minoShifter, minoRotation, height)
    private val minoRotationDetail = MinoRotationDetail(minoFactory, minoRotation)
    private val lockedReachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)
    private val checker = SpinChecker(minoFactory, minoRotationDetail, lockedReachable)

    // 入力された地形が、Tミノが与えられたときにTスピンできる地形であるとき `true` を返す
    fun validate(field: Field): Boolean {
        val piece = Piece.T
        val rotateCandidate = rotateCandidate
        val search = rotateCandidate.search(field.freeze(), piece, height)
        if (search.isEmpty()) {
            return false
        }

        return search.any {
            val freeze = field.freeze()
            val mino = minoFactory.create(piece, it.rotate)
            freeze.put(mino, it.x, it.y)
            val clearLine = freeze.clearLine()

            val spinOptional = checker.check(field, SimpleOperation(piece, it.rotate, it.x, it.y), height, clearLine)
            if (spinOptional.isPresent) {
                val spin = spinOptional.get()
                spin.spin == TSpins.Regular && requiredClearLines <= spin.clearedLine
            } else {
                false
            }
        }
    }
}