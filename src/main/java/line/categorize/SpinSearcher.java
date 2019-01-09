package line.categorize;

import common.datastore.Operation;
import core.action.reachable.LockedReachable;
import core.field.Field;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.srs.Rotate;
import line.commons.rotate.Direction;
import line.commons.rotate.MinoRotationDetail;
import line.commons.rotate.SpinResult;
import line.commons.spin.Spin;
import line.commons.spin.SpinsCommons;

import java.util.ArrayList;
import java.util.List;

class SpinSearcher {
    private static final int MAX_HEIGHT = 24;

    private final MinoFactory minoFactory;
    private final MinoRotationDetail minoRotationDetail;

    SpinSearcher(MinoFactory minoFactory, MinoRotationDetail minoRotationDetail) {
        this.minoFactory = minoFactory;
        this.minoRotationDetail = minoRotationDetail;
    }

    List<Spin> getSpins(Field fieldWithoutT, Operation operation, LockedReachable lockedReachable) {
        assert operation.getPiece() == Piece.T;

        int x = operation.getX();
        int y = operation.getY();
        Rotate rotate = operation.getRotate();

        assert SpinsCommons.isTSpin(fieldWithoutT, x, y);

        List<Spin> allSpins = new ArrayList<>();

        Mino after = minoFactory.create(Piece.T, rotate);

        // 左回転
        {
            Mino before = minoFactory.create(Piece.T, after.getRotate().getRightRotate());
            int[][] patterns = minoRotationDetail.getLeftPatternsFrom(before);

            Direction direction = Direction.Left;

            List<Spin> spins = get(lockedReachable, fieldWithoutT, operation, after, before, patterns, direction, MAX_HEIGHT);
            allSpins.addAll(spins);
        }

        // 右回転
        {
            Mino before = minoFactory.create(Piece.T, after.getRotate().getLeftRotate());
            int[][] patterns = minoRotationDetail.getRightPatternsFrom(before);

            Direction direction = Direction.Right;

            List<Spin> spins = get(lockedReachable, fieldWithoutT, operation, after, before, patterns, direction, MAX_HEIGHT);
            allSpins.addAll(spins);
        }

        return allSpins;
    }

    private List<Spin> get(LockedReachable lockedReachable, Field fieldWithoutT, Operation operation, Mino after, Mino before, int[][] patterns, Direction direction, int maxHeight) {
        List<Spin> spins = new ArrayList<>();

        for (int[] pattern : patterns) {
            // 開店前の位置に移動
            int beforeX = operation.getX() - pattern[0];
            int beforeY = operation.getY() - pattern[1];

            if (beforeX + before.getMinX() < 0 || 10 <= beforeX + before.getMaxX()) {
                continue;
            }

            if (beforeY + before.getMinY() < 0) {
                continue;
            }

            if (!fieldWithoutT.canPut(before, beforeX, beforeY)) {
                continue;
            }

            SpinResult spinResult = minoRotationDetail.getKicks(fieldWithoutT, direction, before, after, beforeX, beforeY);

            if (spinResult == SpinResult.NONE) {
                continue;
            }

            // 回転後に元の場所に戻る
            if (spinResult.getToX() != operation.getX() || spinResult.getToY() != operation.getY()) {
                continue;
            }

            // 回転前の位置に移動できる
            if (!lockedReachable.checks(fieldWithoutT, before, beforeX, beforeY, maxHeight)) {
                continue;
            }

            Spin spin = SpinsCommons.getSpins(fieldWithoutT, spinResult);
            spins.add(spin);
        }

        return spins;
    }
}
