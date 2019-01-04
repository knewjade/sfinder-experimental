package line.commons;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import core.field.Field;
import core.mino.MinoFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmptyCandidate implements Candidate {
    public static EmptyCandidate create(MinoFactory minoFactory, int maxHeight, List<Operation> operationList) {
        Field field = LineCommons.toField(minoFactory, operationList, maxHeight);
        return new EmptyCandidate(operationList, field);
    }

    private final List<Operation> operationList;
    private final Field field;
    private final HashSet<Integer> keys;

    public EmptyCandidate(List<Operation> operationList, Field field) {
        this.operationList = operationList;
        this.field = field;
        this.keys = new HashSet<>();
    }

    @Override
    public Operations toOperations() {
        return new Operations(operationList);
    }

    @Override
    public boolean canPut(KeyOriginalPiece keyOriginalPiece) {
        return field.canPut(keyOriginalPiece.getOriginalPiece());
    }

    @Override
    public List<Operation> getOperationList() {
        return operationList;
    }

    @Override
    public List<Operation> newOperationList() {
        return new ArrayList<>(operationList);
    }

    @Override
    public Field newField() {
        return field.freeze();
    }

    @Override
    public Set<Integer> newKeys() {
        return new HashSet<>(keys);
    }

    @Override
    public Set<Integer> getKeys() {
        return keys;
    }

    @Override
    public PieceCounter newPieceCounter() {
        return new PieceCounter(operationList.stream().map(Operation::getPiece));
    }
}
