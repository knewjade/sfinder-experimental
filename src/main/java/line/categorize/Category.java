package line.categorize;

import common.datastore.Operation;
import common.datastore.Operations;
import core.mino.MinoFactory;
import line.commons.LineCommons;
import line.commons.OperationsWithT;

import java.util.ArrayList;
import java.util.List;

class Category {
    private final Operations operations;
    private final Operation operation;
    private final int clearedLine;
    private final int maxY;

    static Category create(Operations operations, MinoFactory minoFactory, int maxHeight) {
        List<Operation> operationList = new ArrayList<>(operations.getOperations());
        OperationsWithT operationsWithT = new OperationsWithT(operationList, minoFactory, maxHeight);
        int maxY = LineCommons.getMaxY(operationsWithT.newField());
        int clearedLine = operationsWithT.newField().clearLine();
        return new Category(operations, operationsWithT.getT(), clearedLine, maxY);
    }

    private Category(Operations operations, Operation operation, int clearedLine, int maxY) {
        this.operations = operations;
        this.operation = operation;
        this.clearedLine = clearedLine;
        this.maxY = maxY;
    }

    public Operations getOperations() {
        return operations;
    }

    public Operation getOperation() {
        return operation;
    }

    int getUsingFieldHeight() {
        return maxY + 1;
    }

    public Operation getT() {
        return operation;
    }

    public int getClearedLine() {
        return clearedLine;
    }
}
