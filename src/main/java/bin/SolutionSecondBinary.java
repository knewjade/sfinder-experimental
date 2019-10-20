package bin;

import common.datastore.MinoOperationWithKey;

import java.util.List;
import java.util.function.BiFunction;

// Support multi threading
public class SolutionSecondBinary {
    private final short[] steps;
    private final byte[] solutions;
    private final int max;
    private final Movement movement;

    public SolutionSecondBinary(short[] steps, byte[] solutions, Movement movement) {
        this.movement = movement;
        assert steps.length * 11 == solutions.length;
        this.steps = steps;
        this.solutions = solutions;
        this.max = steps.length;
    }

    public SolutionSecondBinary(int max, Movement movement) {
        this.steps = new short[max];
        this.solutions = new byte[max * 11];
        this.max = max;
        this.movement = movement;
    }

    public byte[] getSolutions() {
        return this.solutions;
    }

    public int max() {
        return max;
    }

    public boolean satisfies(int index, short newValue, BiFunction<Short, Short, Boolean> satisfy) {
        return satisfy.apply(steps[index], newValue);
    }

    public synchronized void putIfSatisfy(
            int index, short newValue, List<MinoOperationWithKey> operations, boolean[] holds,
            BiFunction<Short, Short, Boolean> satisfy
    ) {
        assert operations.size() == 10;
        assert holds.length == 10;

        if (satisfy.apply(steps[index], newValue)) {
            steps[index] = newValue;
            solutions[index] = 1;
            int start = index + 1;
            for (int i = 0; i < 10; i++) {
                MinoOperationWithKey operation = operations.get(i);
                boolean hold = holds[i];
                Step step = movement.harddrop(operation.getPiece(), operation.getRotate(), operation.getX());
                solutions[start + i] = Movements.possibleForByte(step.movement(), step.rotateCount(), hold);
            }
        }
    }
}
