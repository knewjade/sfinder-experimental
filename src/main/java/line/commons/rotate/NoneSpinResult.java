package line.commons.rotate;

import core.srs.Rotate;

public class NoneSpinResult implements SpinResult {
    @Override
    public Direction getDirection() {
        throw new IllegalStateException();
    }

    @Override
    public int getToX() {
        throw new IllegalStateException();
    }

    @Override
    public int getToY() {
        throw new IllegalStateException();
    }

    @Override
    public Rotate getToRotate() {
        throw new IllegalStateException();
    }

    @Override
    public int getTestPatternIndex() {
        throw new IllegalStateException();
    }

    @Override
    public int getClearedLine() {
        throw new IllegalStateException();
    }
}