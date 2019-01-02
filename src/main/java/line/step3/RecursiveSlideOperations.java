package line.step3;

import common.datastore.Operation;
import common.datastore.PieceCounter;
import core.mino.MinoFactory;
import line.commons.LineCommons;

import java.util.ArrayList;
import java.util.List;

public class RecursiveSlideOperations implements SlideOperations {
    private final List<? extends Operation> operationList;
    private final KeyOriginalPiece keyOriginalPiece;
    private final int minY;

    // 最も低いブロックがy=4になるように調整
    RecursiveSlideOperations(MinoFactory minoFactory, List<? extends Operation> operationList, KeyOriginalPiece keyOriginalPiece) {
        List<Operation> operations = new ArrayList<>(operationList);
        operations.add(keyOriginalPiece.getOriginalPiece());
        this.operationList = operations;

        this.keyOriginalPiece = keyOriginalPiece;
        this.minY = LineCommons.getMinY(minoFactory, operations);
    }

    @Override
    public List<Operation> getGroundSlideOperationList() {
        return LineCommons.slideOperations(operationList, -minY);
    }

    @Override
    public List<Operation> getAirSlideOperationList() {
        return LineCommons.slideOperations(operationList, SLIDE_Y - minY);
    }

    @Override
    public PieceCounter getPieceCounter() {
        return new PieceCounter(operationList.stream().map(Operation::getPiece));
    }

    @Override
    public KeyOriginalPiece getKeyOriginalPiece() {
        return keyOriginalPiece;
    }
}
