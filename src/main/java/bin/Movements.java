package bin;

import common.datastore.MinoOperationWithKey;

import java.util.List;

public class Movements {
    public static short impossible() {
        return 0;
    }

    public static short possible(int move, int rotate, int hold) {
        assert isRangeIn(move, rotate, hold);
        return (short) (0b10000000_00000000 |
                move << 8 |
                rotate << 4 |
                hold);
    }

    public static short possible(short moveAndRotate, int hold) {
        assert isPossible(moveAndRotate);
        assert (moveAndRotate & 0b1111) == 0;
        assert isRangeIn(0, 0, hold);
        return (short) (moveAndRotate | hold);
    }

    public static boolean isRangeIn(int move, int rotate, int hold) {
        assert 0 <= move && 0 <= rotate && 0 <= hold;
        return move < 64 && rotate < 16 && hold < 16;
    }

    public static boolean isPossible(short value) {
        return value != 0;
    }

    // operationsの順番にmovementは影響を受けない
    // holdは0と仮定する (ある固定されたミノ順下で最も小さい値を見つけるため、ホールドの回数は定数として扱っても問題ない)
    public static short calcMinStep(Movement movement, List<? extends MinoOperationWithKey> operations) {
        assert operations.size() == 10;
        int moveCount = 0;
        int rotateCount = 0;

        for (MinoOperationWithKey operation : operations) {
            assert operation.getNeedDeletedKey() == 0L;
            Step step = movement.harddrop(operation.getPiece(), operation.getRotate(), operation.getX());
            moveCount += step.movement();
            rotateCount += step.rotateCount();
        }

        int holdCount = 0;
        if (Movements.isRangeIn(moveCount, rotateCount, holdCount)) {
            return Movements.possible(moveCount, rotateCount, holdCount);
        }

        throw new IllegalStateException();
    }
}
