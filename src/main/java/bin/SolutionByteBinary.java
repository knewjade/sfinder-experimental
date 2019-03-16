package bin;

public class SolutionByteBinary {
    private final byte[] bytes;
    private final int max;

    public SolutionByteBinary(int max) {
        this.bytes = new byte[max];
        this.max = max;
    }

    public synchronized void put(int index, byte value) {
        assert 0 <= index && index < max;
        bytes[index] = value;
    }

    public byte[] get() {
        return bytes;
    }
}
