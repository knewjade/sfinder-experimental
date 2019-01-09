package line.commons.rotate;

import core.mino.Mino;
import core.srs.Rotate;

public class SuccessSpinResult implements SpinResult {
    private final Direction direction;
    private final Mino after;
    private final int x;
    private final int y;
    private final int clearedLine;
    private final int testPatternIndex;

    SuccessSpinResult(Direction direction, Mino after, int x, int y, int clearedLine, int testPatternIndex) {
        this.direction = direction;
        this.after = after;
        this.x = x;
        this.y = y;
        this.clearedLine = clearedLine;
        this.testPatternIndex = testPatternIndex;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public int getToX() {
        return x;
    }

    @Override
    public int getToY() {
        return y;
    }

    @Override
    public Rotate getToRotate() {
        return after.getRotate();
    }

    @Override
    public int getTestPatternIndex() {
        return testPatternIndex;
    }

    @Override
    public int getClearedLine() {
        return clearedLine;
    }
}
