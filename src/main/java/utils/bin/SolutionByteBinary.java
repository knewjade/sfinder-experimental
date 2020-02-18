package utils.bin;

import java.util.function.BiFunction;

// Support multi threading
public class SolutionByteBinary {
    private final byte[] buffer;
    private final int max;

    public SolutionByteBinary(byte[] buffer) {
        this.buffer = buffer;
        this.max = buffer.length;
    }

    public SolutionByteBinary(int max) {
        this.buffer = new byte[max];
        this.max = max;
    }

    public byte[] get() {
        return buffer;
    }

    public byte at(int index) {
        return buffer[index];
    }

    public int max() {
        return max;
    }

    public synchronized void putIfSatisfy(int index, byte newValue, BiFunction<Byte, Byte, Boolean> satisfy) {
        if (satisfy.apply(buffer[index], newValue)) {
            buffer[index] = newValue;
        }
    }
}
