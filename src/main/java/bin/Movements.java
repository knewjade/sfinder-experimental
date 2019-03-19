package bin;

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

    public static boolean isRangeIn(int move, int rotate, int hold) {
        assert 0 <= move && 0 <= rotate && 0 <= hold;
        return move < 64 && rotate < 16 && hold < 16;
    }

    public static boolean isPossible(short value) {
        return value != 0;
    }
}
