package main.percent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 追加する数字は 0 <= x < 2^10 であること
// 追加する数字の数は 10 であること
public class MinoIndexes {
    private static final int[] scale = new int[10];
    private static final int BIT = 10;
    private static final long MASK = (1L << BIT) - 1;

    static {
        for (int index = 0; index < 5; index++)
            scale[index] = index * BIT;
        for (int index = 5; index < 10; index++)
            scale[index] = (index - 5) * BIT;
    }

    private final long low;
    private final long high;
    private final int lowLastNumber;

    public MinoIndexes(List<Integer> numbers) {
        assert numbers.size() == 10;
        assert numbers.stream().allMatch(n -> 0 <= n && n <= MASK);
        List<Integer> ints = new ArrayList<>(numbers);
        Collections.sort(ints);
        this.low = calcLow(ints);
        this.high = calcHigh(ints);
        this.lowLastNumber = ints.get(4);
    }

    private long calcLow(List<Integer> ints) {
        long value = 0;
        for (int index = 0; index < 5; index++) {
            long v = ints.get(index);
            value += v << scale[index];
        }
        return value;
    }

    private long calcHigh(List<Integer> ints) {
        long value = 0;
        for (int index = 5; index < 10; index++) {
            long v = ints.get(index);
            value += v << scale[index];
        }
        return value;
    }

    public List<Integer> getNumbers() {
        Stream<Integer> stream = getNumbersStream();
        return stream.collect(Collectors.toList());
    }

    public Stream<Integer> getNumbersStream() {
        Stream.Builder<Integer> builder = Stream.builder();
        add(builder, low);
        add(builder, high);
        return builder.build();
    }

    private void add(Stream.Builder<Integer> builder, long value) {
        for (int index = 0; index < 4; index++) {
            builder.accept((int) (value & MASK));
            value >>= BIT;
        }
        builder.accept((int) (value & MASK));
    }

    public boolean contains(int number) {
        if (number <= lowLastNumber) {
            return contains(low, number);
        } else {
            return contains(high, number);
        }
    }

    private boolean contains(long value, long number) {
        for (int index = 0; index < 4; index++) {
            long e = value & MASK;
            if (number <= e)
                return e == number;
            value >>= BIT;
        }
        return (value & MASK) == number;
    }
}
