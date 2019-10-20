package bin;

// Support multi threading
public class SolutionIntBinary {
    private final int[] buffer;
    private final int max;

    public SolutionIntBinary(int[] buffer) {
        this.buffer = buffer;
        this.max = buffer.length;
    }

    public SolutionIntBinary(int max) {
        this.buffer = new int[max];
        this.max = max;
    }

    public int[] get() {
        return buffer;
    }

    public int at(int index) {
        return buffer[index];
    }

    public int max() {
        return max;
    }
}
