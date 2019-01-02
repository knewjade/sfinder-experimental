package line.step3;

import common.datastore.Operation;
import common.datastore.PieceCounter;

import java.util.List;

interface SlideOperations {
    int SLIDE_Y = 4;

    List<Operation> getGroundSlideOperationList();

    List<Operation> getAirSlideOperationList();

    PieceCounter getPieceCounter();

    KeyOriginalPiece getKeyOriginalPiece();
}
