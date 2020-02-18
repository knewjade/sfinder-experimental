package utils.step;

public class Steps {
    public static short impossible() {
        return 0;
    }

    public static boolean isRangeIn(int frameCount, int holdCount) {
        assert 0 <= frameCount && 0 <= holdCount;
        // frameCount < 2^11 && holdCount < 2^4;
        return frameCount < 2048 && holdCount < 16;
    }

    public static short possible(int frameCount, int holdCount) {
        assert isRangeIn(frameCount, holdCount);
        return (short) (0b10000000_00000000 |
                frameCount << 4 |
                holdCount);
    }

    public static boolean isPossible(short value) {
        return (value & 0b10000000_00000000) != 0;
    }

    public static int compare(short o1, short o2) {
        if ((o1 & o2 & (short) 0b10000000_00000000) != 0) {
            assert (o1 & (short) 0b10000000_00000000) != 0 : o1;
            assert (o2 & (short) 0b10000000_00000000) != 0 : o2;

            // どちらも解あり
            return comparePossible(Short.toUnsignedInt(o1), Short.toUnsignedInt(o2));
        }

        // 片方が解なし or 両方とも解なし
        // 値が小さい方（符号ビットが立っている方）が良い数字となる
        return Short.compare(o1, o2);
    }

    private static int comparePossible(int o1, int o2) {
        int frame1 = (o1 & 0b01111111_11110000) >>> 4;
        int hold1 = o1 & 0b1111;
        int sum1 = frame1 + hold1;

        int frame2 = (o2 & 0b01111111_11110000) >>> 4;
        int hold2 = o2 & 0b1111;
        int sum2 = frame2 + hold2;

        // 合計値の比較
        int compareSum = Integer.compare(sum1, sum2);
        if (compareSum != 0) {
            return compareSum;
        }

        // frameが小さい方が良い
        return Integer.compare(frame1, frame2);

        // 合計値が同じでframeが同じとき、holdが異なることはない
    }

    public static int getStepCount(short value) {
        if (!isPossible(value)) {
            return -1;
        }
        return  (value & 0b01111111_11110000) >>> 4;
    }
}
