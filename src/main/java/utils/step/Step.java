package utils.step;

import common.datastore.MinoOperationWithKey;
import utils.Movement;

import java.util.List;

public interface Step {
    static Step create(Movement movement) {
        return new MaxMoveRotateStep(movement);
//        return new PlusMoveRotateStep(movement);
    }

    short calcMinSteps(List<? extends MinoOperationWithKey> operations);
}
