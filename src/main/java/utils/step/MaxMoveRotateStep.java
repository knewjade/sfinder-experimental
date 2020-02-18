package utils.step;

import common.datastore.MinoOperationWithKey;
import utils.Movement;

import java.util.List;

public class MaxMoveRotateStep implements Step {
    private final Movement movement;

    public MaxMoveRotateStep(Movement movement) {
        this.movement = movement;
    }

    @Override
    public short calcMinSteps(List<? extends MinoOperationWithKey> operations) {
        assert operations.size() == 10;

        int frameCount = 0;
        for (MinoOperationWithKey operation : operations) {
            assert operation.getNeedDeletedKey() == 0L;
            utils.Step step = movement.harddrop(operation.getPiece(), operation.getRotate(), operation.getX());

            int move = step.movement();
            int rotate = step.rotateCount();

            frameCount += Math.max(move, rotate);
        }

        int holdCount = 0;
        if (Steps.isRangeIn(frameCount, holdCount)) {
            return Steps.possible(frameCount, holdCount);
        }

        throw new IllegalStateException();
    }
}
