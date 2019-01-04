package line.commons;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import core.field.Field;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecursiveCandidate implements Candidate {
    private final List<Operation> operationList;
    private final Field field;
    private final Set<Integer> keys;
    private final KeyOriginalPiece keyOriginalPiece;

    public RecursiveCandidate(Candidate prev, KeyOriginalPiece keyOriginalPiece) {
        List<Operation> operationList = prev.newOperationList();
        operationList.add(keyOriginalPiece);
        this.operationList = operationList;

        Field field = prev.newField();
        field.put(keyOriginalPiece.getOriginalPiece());
        this.field = field;

        Set<Integer> keys = prev.newKeys();
        keys.add(keyOriginalPiece.getIndex());
        this.keys = keys;

        this.keyOriginalPiece = keyOriginalPiece;
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

    public KeyOriginalPiece getKeyOriginalPiece() {
        return keyOriginalPiece;
    }
}
