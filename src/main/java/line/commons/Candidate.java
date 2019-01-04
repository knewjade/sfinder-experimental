package line.commons;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import core.field.Field;

import java.util.List;
import java.util.Set;

public interface Candidate {
    Operations toOperations();

    boolean canPut(KeyOriginalPiece keyOriginalPiece);

    List<Operation> newOperationList();

    Field newField();

    Set<Integer> newKeys();

    Set<Integer> getKeys();

    PieceCounter newPieceCounter();

    List<Operation> getOperationList();
}
