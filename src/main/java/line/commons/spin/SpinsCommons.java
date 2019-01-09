package line.commons.spin;

import core.field.Field;
import core.srs.Rotate;
import line.commons.rotate.Direction;
import line.commons.rotate.SpinResult;

import java.util.stream.Stream;

public class SpinsCommons {
    public static boolean isTSpin(Field field, int x, int y) {
        return 3L <= Stream.of(
                isBlock(field, x - 1, y - 1),
                isBlock(field, x - 1, y + 1),
                isBlock(field, x + 1, y - 1),
                isBlock(field, x + 1, y + 1)
        ).filter(Boolean::booleanValue).count();
    }

    private static boolean isBlock(Field field, int x, int y) {
        if (x < 0 || 10 <= x || y < 0) {
            return true;
        }
        return !field.isEmpty(x, y);
    }

    public static Spin getSpins(Field before, SpinResult spinResult) {
        Rotate toRotate = spinResult.getToRotate();
        int toX = spinResult.getToX();
        int toY = spinResult.getToY();

        boolean filledTFront = isFilledTFront(before, toRotate, toX, toY);
        TSpins spin = getTSpin(spinResult, toRotate, filledTFront);

        Direction direction = spinResult.getDirection();
        TSpinNames name = getTSpinName(spinResult, toRotate, filledTFront, direction);

        return new Spin(spin, name, spinResult.getClearedLine());
    }

    private static TSpins getTSpin(SpinResult spinResult, Rotate toRotate, boolean filledTFront) {
        if (!filledTFront) {
            // 正面側に2つブロックがない
            // Mini判定の可能性がある
            if (isHorizontal(toRotate) || spinResult.getTestPatternIndex() != 4) {
                // 接着時にTが横向き or 回転テストパターンが最後のケースではない
                return TSpins.Mini;
            }

            // TSTの形のみ、Regularとなる
        }

        return TSpins.Regular;
    }

    private static TSpinNames getTSpinName(SpinResult spinResult, Rotate toRotate, boolean filledTFront, Direction direction) {
        if ((direction == Direction.Left && toRotate == Rotate.Right) || (direction == Direction.Right && toRotate == Rotate.Left)) {
            // 裏返した状態から回転させたとき
            switch (spinResult.getTestPatternIndex()) {
                case 3: {
                    if (filledTFront) {
                        // 正面側に2つブロックがある
                        return TSpinNames.Iso;
                    }
                    return TSpinNames.Neo;
                }
                case 4: {
                    return TSpinNames.Fin;
                }
            }
        }

        return TSpinNames.NoName;
    }

    private static boolean isFilledTFront(Field field, Rotate rotate, int x, int y) {
        switch (rotate) {
            case Spawn: {
                return isBlock(field, x - 1, y + 1) && isBlock(field, x + 1, y + 1);
            }
            case Reverse: {
                return isBlock(field, x - 1, y - 1) && isBlock(field, x + 1, y - 1);
            }
            case Left: {
                return isBlock(field, x - 1, y - 1) && isBlock(field, x - 1, y + 1);
            }
            case Right: {
                return isBlock(field, x + 1, y - 1) && isBlock(field, x + 1, y + 1);
            }
        }
        throw new IllegalStateException();
    }

    private static boolean isHorizontal(Rotate rotate) {
        return rotate == Rotate.Spawn || rotate == Rotate.Reverse;
    }
}
