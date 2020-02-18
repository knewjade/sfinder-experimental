package utils.frame;

public class Frames {
    public static byte impossible() {
        return 0;
    }

    public static int compare(byte o1, byte o2) {
        if ((o1 & o2 & (byte) 0b10000000) != 0) {
            assert (o1 & (byte) 0b10000000) != 0 : o1;
            assert (o2 & (byte) 0b10000000) != 0 : o2;

            // どちらも解あり
            return comparePossible(Short.toUnsignedInt(o1), Short.toUnsignedInt(o2));
        }

        // 片方が解なし or 両方とも解なし
        // 値が小さい方（符号ビットが立っている方）が良い数字となる
        return Short.compare(o1, o2);
    }

    private static int comparePossible(int o1, int o2) {
        int count1 = o1 & 0b01111111;
        int count2 = o2 & 0b01111111;

        return Integer.compare(count1, count2);
    }

    public static byte possible(int stepCount, int holdCount) {
        int count = stepCount + holdCount;
        assert count < 128;
        return (byte) (0b10000000 | count);
    }

    public static boolean isPossible(byte value) {
        return (value & 0b10000000) != 0;
    }

    public static int getFrameCount(byte value) {
        if (!isPossible(value)) {
            return -1;
        }
        return value & 0b01111111;
    }
}
