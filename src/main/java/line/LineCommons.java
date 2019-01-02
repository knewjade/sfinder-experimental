package line;

import common.datastore.MinimalOperationWithKey;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;

import java.util.List;
import java.util.stream.Collectors;

class LineCommons {
    static Field toField(MinoFactory minoFactory, List<? extends Operation> operationList, int maxHeight) {
        Field field = FieldFactory.createField(maxHeight);
        for (Operation operation : operationList) {
            Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
            field.put(mino, operation.getX(), operation.getY());
        }
        return field;
    }

    static List<MinoOperationWithKey> toOperationWithKeys(MinoFactory minoFactory, List<? extends Operation> operations) {
        return toOperationWithKeys(minoFactory, operations, 0);
    }

    static List<MinoOperationWithKey> toOperationWithKeys(MinoFactory minoFactory, List<? extends Operation> operations, int slideY) {
        return operations.stream()
                .map(operation -> {
                    Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
                    int operationX = operation.getX();
                    int operationY = operation.getY() - slideY;
                    return new MinimalOperationWithKey(mino, operationX, operationY, 0L);
                })
                .collect(Collectors.toList());
    }
}
