package bin;

import java.util.Comparator;

public class MovementComparator implements Comparator<Short> {
    public boolean shouldUpdate(Short oldValue, Short newValue) {
        return 0 < compare(oldValue, newValue);
    }

    @Override
    public int compare(Short o1, Short o2) {
        if ((o1 & o2 & (short) 0b10000000_00000000) != 0) {
            // どちらも解あり
            return comparePossible(o1, o2);
        }

        // 片方が解なし or 両方とも解なし
        // 値が小さい方（符号ビットが立っている方）が良い数字となる
        return Short.compare(o1, o2);
    }

    private int comparePossible(int o1, int o2) {
        assert (o1 & (short) 0b10000000_00000000) != 0 : o1;
        assert (o2 & (short) 0b10000000_00000000) != 0 : o2;

        int move1 = (o1 & 0b00111111_00000000) >>> 8;
        int rotate1 = ((o1 & 0b11110000) >>> 4);
        int hold1 = o1 & 0b1111;
        int sum1 = move1 + rotate1 + hold1;

        int move2 = (o2 & 0b00111111_00000000) >>> 8;
        int rotate2 = ((o2 & 0b11110000) >>> 4);
        int hold2 = o2 & 0b1111;
        int sum2 = move2 + rotate2 + hold2;

        // 合計値の比較
        int compareSum = Integer.compare(sum1, sum2);
        if (compareSum != 0) {
            return compareSum;
        }

        // moveが小さい方が良い
        int compareMove = Integer.compare(move1, move2);
        if (compareMove != 0) {
            return compareMove;
        }

        // holdが小さい方が良い
        return Integer.compare(hold1, hold2);
    }
}
