package helper;

import core.field.KeyOperators;

public class KeyParser {
    public static String parseToString(long deleteKey, int height) {
        assert height == 4;
        int intValue = 0;
        for (int index = 0; index < 4; index++) {
            long mask = 1L << (index * 10);
            if ((deleteKey & mask) != 0L)
                intValue += (1 << index);
        }
        return padding(Integer.toBinaryString(intValue), height);
    }

    private static String padding(String s, int maxLength) {
        int length = maxLength - s.length();
        StringBuilder empty = new StringBuilder();
        for (int count = 0; count < length; count++)
            empty.append("0");
        return empty + s;
    }

    public static long parseToLong(String str) {
        long deleteKey = 0L;
        int height = str.length();
        for (int index = 0; index < height; index++) {
            char c = str.charAt(index);
            if (c == '1')
                deleteKey |= KeyOperators.getDeleteBitKey(height - index - 1);
        }
        return deleteKey;
    }
}
