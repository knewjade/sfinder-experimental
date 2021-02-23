import common.SpinChecker
import core.action.reachable.LockedReachable
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import core.srs.MinoRotationDetail

fun createLockedReachableSpinChecker(height: Int): SpinChecker {
    val minoFactory = MinoFactory()
    val minoShifter = MinoShifter()
    val minoRotation = MinoRotation.create()
    val lockedReachable = LockedReachable(minoFactory, minoShifter, minoRotation, height)
    val minoRotationDetail = MinoRotationDetail(minoFactory, minoRotation)
    return SpinChecker(minoFactory, minoRotationDetail, lockedReachable)
}