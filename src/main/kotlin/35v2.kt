import common.datastore.Operations
import common.datastore.PieceCounter
import common.datastore.action.Action
import common.iterable.CombinationIterable
import common.parser.OperationInterpreter
import concurrent.RotateReachableThreadLocal
import core.action.candidate.LockedCandidate
import core.field.FieldFactory
import core.field.FieldView
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.mino.Piece
import core.srs.MinoRotation
import searcher.checker.CheckerNoHold
import searcher.common.validator.PerfectValidator
import searcher.spins.FirstPreSpinRunner
import searcher.spins.FullSpinRunner
import searcher.spins.SecondPreSpinRunner
import java.util.*

class CheckerThreadLocal(val minoFactory: MinoFactory, val validator: PerfectValidator) : ThreadLocal<CheckerNoHold<Action>>() {
    override fun get(): CheckerNoHold<Action> {
        return CheckerNoHold(minoFactory, validator)
    }
}

fun main(args: Array<String>) {
    val fieldHeight = 6
    val initField = FieldFactory.createField("" +
            "__________" +
            "__________" +
            "__________" +
            "__________" +
            "__________" +
            "", fieldHeight)
    val s1 = Arrays.asList(
            Piece.S, Piece.I, Piece.S, Piece.Z, Piece.O, Piece.T
    )
    val s2 = Arrays.asList(
            Piece.I, Piece.L, Piece.J, Piece.S, Piece.Z
    )
    val pieceCounter3 = PieceCounter(listOf(Piece.S, Piece.Z, Piece.T))

    val pieceCounterSx2 = PieceCounter(listOf(Piece.S, Piece.S))

    val lockObj = Object()

    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation.create()

    val rotateReachableThreadLocal = RotateReachableThreadLocal(minoFactory, minoShifter, minoRotation, 6)

    // Initialize
    val lockedCandidate = LockedCandidate(minoFactory, minoShifter, minoRotation, 4)

    val validator = PerfectValidator()

    val checkerThreadLocal = CheckerThreadLocal(minoFactory, validator)

    (1..3).forEach { selected ->
        CombinationIterable(s2, selected).forEach { combination ->
            println(combination)

            val pieceCounter1 =  PieceCounter(s1 + combination)
            val pieceCounter2 =  PieceCounter(s2 + listOf(Piece.T)).removeAndReturnNew(PieceCounter(combination))

            val firstPreSpinRunner1 = FirstPreSpinRunner(minoFactory, minoShifter, rotateReachableThreadLocal, 0, 2, 6, fieldHeight)
            val secondPreSpinRunner1 = SecondPreSpinRunner(firstPreSpinRunner1, initField, pieceCounter1, 1)
            val runner1 = FullSpinRunner()
            runner1.search(secondPreSpinRunner1, 1).parallel()
                    .forEach loop1@{ candidate1 ->
                        val restCounter = pieceCounter1.removeAndReturnNew(PieceCounter(candidate1.result.operationStream().map { it.piece }))
                        if (restCounter.containsAll(pieceCounterSx2)) {
                            // S=0 solution
                            return@loop1
                        }

                        val nextCounter = restCounter.addAndReturnNew(pieceCounter2)

                        val freeze = candidate1.result.allMergedField.freeze()
                        val clearLine1 = freeze.clearLine()

                        if (clearLine1 != 1) {
                            return@loop1
                        }

                        if (candidate1.operationT.x <= 0 || 9 <= candidate1.operationT.x) {
                            return@loop1
                        }

                        val nextLine = 6 - clearLine1

                        if (!validator.validate(freeze, nextLine)) {
                            return@loop1
                        }

                        val firstPreSpinRunner2 = FirstPreSpinRunner(minoFactory, minoShifter, rotateReachableThreadLocal, 0, 2, nextLine, fieldHeight)
                        val secondPreSpinRunner2 = SecondPreSpinRunner(firstPreSpinRunner2, freeze, nextCounter, 1)
                        val runner2 = FullSpinRunner()
                        runner2.search(secondPreSpinRunner2, 1)
                                .forEach loop2@{ candidate2 ->
                                    val restCounter2 = nextCounter.removeAndReturnNew(PieceCounter(candidate2.result.operationStream().map { it.piece }))

                                    val nextCounter2 = restCounter2.addAndReturnNew(pieceCounter3)

                                    val freeze2 = candidate2.result.allMergedField.freeze()
                                    val clearLine2 = freeze2.clearLine()
                                    val nextLine2 = nextLine - clearLine2

                                    if (!validator.validate(freeze2, nextLine2)) {
                                        return@loop2
                                    }

                                    if (clearLine2 != 1) {
                                        return@loop2
                                    }

                                    if (candidate1.operationT.x <= 0 || 9 <= candidate1.operationT.x) {
                                        return@loop2
                                    }

                                    val firstPreSpinRunner3 = FirstPreSpinRunner(minoFactory, minoShifter, rotateReachableThreadLocal, 0, 3, nextLine2, fieldHeight)
                                    val secondPreSpinRunner3 = SecondPreSpinRunner(firstPreSpinRunner3, freeze2, nextCounter2, 1)
                                    val runner3 = FullSpinRunner()
                                    runner3.search(secondPreSpinRunner3, 2)
                                            .forEach loop3@{ candidate3 ->
                                                val restCounter3 = nextCounter2.removeAndReturnNew(PieceCounter(candidate3.result.operationStream().map { it.piece }))
                                                val restPieces = restCounter3.blocks

                                                if (restPieces.size == 1) {
                                                    if (restPieces[0] != Piece.S && restPieces[0] != Piece.Z) {
                                                        return@loop3
                                                    }
                                                } else if (restPieces.size == 2) {
                                                    if (!(
                                                                    restPieces[0] == Piece.S && restPieces[1] == Piece.Z
                                                                            || restPieces[0] == Piece.Z && restPieces[1] == Piece.S

                                                                    )) {
                                                        return@loop3
                                                    }
                                                } else {
                                                    return@loop3
                                                }

                                                val freeze3 = candidate3.result.allMergedField.freeze()
                                                val clearLine3 = freeze3.clearLine()

                                                if (clearLine3 != 2) {
                                                    return@loop3
                                                }

                                                if (candidate1.operationT.x <= 0 || 9 <= candidate1.operationT.x) {
                                                    return@loop3
                                                }

                                                if (!validator.validate(freeze3, 2)) {
                                                    return@loop3
                                                }

                                                val checker = checkerThreadLocal.get()
                                                val isSucceed = checker.check(freeze3, restPieces, lockedCandidate, 2, restPieces.size)

                                                if (isSucceed) {
                                                    synchronized(lockObj) {
                                                        println("===")
                                                        println(FieldView.toString(freeze3))
                                                        println(OperationInterpreter.parseToString(Operations(candidate1.result.operationStream())))
                                                        println(OperationInterpreter.parseToString(Operations(candidate2.result.operationStream())))
                                                        println(OperationInterpreter.parseToString(Operations(candidate3.result.operationStream())))
                                                    }
                                                }
                                            }
                                }
                    }
        }
    }

//    val results = runner.search(secondPreSpinRunner, 1).parallel().collect<List<Candidate>, Any>(Collectors.toList())

}