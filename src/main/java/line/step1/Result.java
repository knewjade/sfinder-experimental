package line.step1;

import common.datastore.PieceCounter;
import core.field.Field;
import core.neighbor.OriginalPiece;

import java.util.stream.Stream;

public interface Result {
    boolean isSolution();

    boolean isCandidate();

    long getMinBoard();

    Result next(OriginalPiece piece);

    boolean canPut(OriginalPiece piece);

    Field getLeft();

    Field getField();

    Stream<OriginalPiece> pieceStream();

    PieceCounter getPieceCounter();
}
