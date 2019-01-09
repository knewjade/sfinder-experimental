package line.commons.rotate;

import core.field.Field;
import core.mino.Mino;
import core.srs.MinoRotation;

public class MinoRotationDetail {
    private static final int FIELD_WIDTH = 10;

    private final MinoRotation minoRotation;

    public MinoRotationDetail(MinoRotation minoRotation) {
        this.minoRotation = minoRotation;
    }

    public SpinResult getKicks(Field field, Direction direction, Mino before, Mino after, int x, int y) {
        int[][] offsets = getOffsets(before, direction);
        return getKicks(field, direction, after, x, y, offsets);
    }

    private int[][] getOffsets(Mino before, Direction direction) {
        switch (direction) {
            case Left:
                return minoRotation.getLeftPatternsFrom(before);
            case Right:
                return minoRotation.getRightPatternsFrom(before);
        }
        throw new IllegalStateException();
    }

    private SpinResult getKicks(Field field, Direction direction, Mino after, int x, int y, int[][] offsets) {
        int minX = -after.getMinX();
        int maxX = FIELD_WIDTH - after.getMaxX();
        int minY = -after.getMinY();
        for (int index = 0, offsetsLength = offsets.length; index < offsetsLength; index++) {
            int[] offset = offsets[index];
            int toX = x + offset[0];
            int toY = y + offset[1];
            if (minX <= toX && toX < maxX && minY <= toY && field.canPut(after, toX, toY)) {
                Field freeze = field.freeze();
                freeze.put(after, toX, toY);
                int clearLine = freeze.clearLine();
                return new SuccessSpinResult(direction, after, toX, toY, clearLine, index);
            }
        }
        return SpinResult.NONE;
    }

    public int[][] getRightPatternsFrom(Mino current) {
        return minoRotation.getRightPatternsFrom(current);
    }

    public int[][] getLeftPatternsFrom(Mino current) {
        return minoRotation.getLeftPatternsFrom(current);
    }
}
