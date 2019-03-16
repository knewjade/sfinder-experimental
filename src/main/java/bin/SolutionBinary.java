package bin;

// Support multi threading
public class SolutionBinary {
    private final byte[] buffer;
    private final int max;

    public SolutionBinary(byte[] buffer) {
        this.buffer = buffer;
        this.max = buffer.length;
    }

    public SolutionBinary(int max) {
        this.buffer = new byte[max];
        this.max = max;
    }

    public synchronized void put(int index, byte value) {
        assert 0 <= index && index < max;
        buffer[index] = value;
    }

    public synchronized void or(int index, byte value) {
        assert 0 <= index && index < max;
        buffer[index] |= value;
    }

    public byte[] get() {
        return buffer;
    }

    public byte at(int index) {
        return buffer[index];
    }
}
