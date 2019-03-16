package bin;

public class SolutionBooleanBinary {
    private final boolean[] booleans;
    private final int max;

    public SolutionBooleanBinary(int max) {
        this.booleans = new boolean[max];
        this.max = max;
    }

    public synchronized void put(int index, boolean value) {
        assert 0 <= index && index < max;
        booleans[index] = value;
    }

    public boolean[] get() {
        return booleans;
    }
}
