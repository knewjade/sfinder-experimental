package main.first;

import java.util.function.BiFunction;

// Support multi threading
class SolutionFirstBinary {
    private final short[] steps;
    private final int[] solutions;
    private final int max;

    public SolutionFirstBinary(short[] steps, int[] solutions) {
        assert steps.length == solutions.length;
        this.steps = steps;
        this.solutions = solutions;
        this.max = steps.length;
    }

    public SolutionFirstBinary(int max) {
        this.steps = new short[max];
        this.solutions = new int[max];
        this.max = max;
    }

    public short[] getSteps() {
        return this.steps;
    }

    public int[] getSolutions() {
        return this.solutions;
    }

    public synchronized void putIfSatisfy(
            int index, short newValue, int newSolutionIndex, BiFunction<Short, Short, Boolean> satisfy
    ) {
        if (satisfy.apply(steps[index], newValue)) {
            steps[index] = newValue;
            solutions[index] = newSolutionIndex;
        }
    }
}
