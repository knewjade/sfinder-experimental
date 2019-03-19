package bin;

import java.util.function.BiFunction;

// Support multi threading
public class SolutionShortBinary {
    private final short[] buffer;
    private final int max;

    public SolutionShortBinary(short[] buffer) {
        this.buffer = buffer;
        this.max = buffer.length;
    }

    public SolutionShortBinary(int max) {
        this.buffer = new short[max];
        this.max = max;
    }

    public short[] get() {
        return buffer;
    }

    public short at(int index) {
        return buffer[index];
    }

    public int max() {
        return max;
    }

    public synchronized void putIfSatisfy(int index, short newValue, BiFunction<Short, Short, Boolean> satisfy) {
        if (satisfy.apply(buffer[index], newValue)) {
            buffer[index] = newValue;
        }
    }
}
