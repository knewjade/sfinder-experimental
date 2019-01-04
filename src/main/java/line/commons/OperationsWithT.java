package line.commons;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import core.field.Field;
import core.mino.MinoFactory;
import core.mino.Piece;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OperationsWithT {
    private final List<Operation> operationList;
    private final Operation operationT;
    private final Field field;
    private final Field fieldWithoutT;

    public OperationsWithT(List<Operation> operationList, MinoFactory minoFactory, int maxHeight) {
        this.operationList = operationList;

        // Tミノを取得
        Optional<Operation> optional = operationList.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst();

        if (!optional.isPresent()) {
            throw new IllegalStateException("T does not exist");
        }

        this.operationT = optional.get();

        // Tミノを除いたOperations
        List<Operation> operationListWithoutT = operationList.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());

        // すべてのミノの地形
        this.field = LineCommons.toField(minoFactory, operationList, maxHeight);

        // Tミノを除いた地形
        this.fieldWithoutT = LineCommons.toField(minoFactory, operationListWithoutT, maxHeight);
    }

    public Operations newOperations() {
        return new Operations(operationList);
    }

    public Field newFieldWithoutT() {
        return fieldWithoutT.freeze();
    }

    public Operation getT() {
        return operationT;
    }

    public boolean isTSpin() {
        return LineCommons.isTSpin(fieldWithoutT, operationT.getX(), operationT.getY());
    }

    public Field newField() {
        return field.freeze();
    }

    public List<Operation> getOperationList() {
        return operationList;
    }

    public PieceCounter newPieceCounter() {
        return new PieceCounter(operationList.stream().map(Operation::getPiece));
    }
}
