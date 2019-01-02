package line.step3;

import common.datastore.Operation;
import common.datastore.PieceCounter;
import core.mino.MinoFactory;
import line.commons.LineCommons;

import java.util.List;

public class EmptySlideOperations implements SlideOperations {
    private final List<? extends Operation> operationList;
    private final int minY;

    // 最も低いブロックがy=4になるように調整
    EmptySlideOperations(MinoFactory minoFactory, List<? extends Operation> operations) {
        this.operationList = operations;
        this.minY = LineCommons.getMinY(minoFactory, operationList);
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
        return null;
    }
}
