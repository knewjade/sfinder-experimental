package line.commons.rotate;

import core.srs.Rotate;

public interface SpinResult {
    SpinResult NONE = new NoneSpinResult();

    Direction getDirection();

    int getToX();

    int getToY();

    Rotate getToRotate();

    int getTestPatternIndex();

    int getClearedLine();
}
