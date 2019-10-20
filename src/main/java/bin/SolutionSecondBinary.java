package bin;

import common.datastore.MinoOperationWithKey;

import java.util.List;
import java.util.function.BiFunction;

// Support multi threading
public class SolutionSecondBinary {
    private final short[] steps;
    private final byte[] solutions1;
    private final byte[] solutions2;
    private final int max1;
    private final Movement movement;

    public SolutionSecondBinary(int max, Movement movement) {
        this.steps = new short[max];
        this.movement = movement;

        long size = max * 11L;

        System.out.println(max);

        if ((long) Integer.MAX_VALUE < size) {
            this.max1 = max / 2;

            int size1 = this.max1 * 11;
            int size2 = (int) (size - size1);

            this.solutions1 = new byte[size1];
            this.solutions2 = new byte[size2];
        } else {
            this.max1 = max;

            this.solutions1 = new byte[(int) size];
            this.solutions2 = new byte[0];
        }

        if ((long) this.solutions1.length + (long) this.solutions2.length != max * 11L) {
            throw new IllegalArgumentException();
        }
    }

    public byte[] getSolutions1() {
        return this.solutions1;
    }

    public byte[] getSolutions2() {
        return this.solutions2;
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

            int baseIndex;
            byte[] solutions;
            if (index < max1) {
                solutions = this.solutions1;
                baseIndex = index * 11;
            } else {
                solutions = this.solutions2;
                baseIndex = (index - max1) * 11;
            }

            solutions[baseIndex] = 1;
            int start = baseIndex + 1;
            for (int i = 0; i < 10; i++) {
                MinoOperationWithKey operation = operations.get(i);
                boolean hold = holds[i];
                Step step = movement.harddrop(operation.getPiece(), operation.getRotate(), operation.getX());
                solutions[start + i] = Movements.possibleForByte(step.movement(), step.rotateCount(), hold);
            }
        }
    }
}
