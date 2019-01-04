package line.step1;

import common.datastore.PieceCounter;
import core.field.Field;
import core.neighbor.OriginalPiece;

import java.util.stream.Stream;

public class EmptyResult implements Result {
    private final Field left;
    private final Field field;
    private final int clearedLine;
    private final PieceCounter pieceCounter;
    private final int lineY;

    EmptyResult(Field left, Field field, PieceCounter pieceCounter, int lineY) {
        this.lineY = lineY;
        assert left.getBoardCount() == 1;
        this.left = left;
        this.field = field;
        this.clearedLine = field.freeze().clearLine();
        this.pieceCounter = pieceCounter;
    }

    @Override
    public boolean isSolution(int maxClearedLine) {
        return left.isPerfect() && clearedLine == maxClearedLine;
    }

    @Override
    public boolean isCandidate(int maxClearedLine) {
        return pieceCounter.getCounter() != 0L && clearedLine < maxClearedLine;
    }

    @Override
    public long getMinBoard() {
        long board = left.getBoard(0);
        long min = board & (-board);
        return min >> lineY * 10;
    }

    @Override
    public Result next(OriginalPiece piece) {
        return new RecursiveResult(this, piece, lineY);
    }

    @Override
    public boolean canPut(OriginalPiece piece) {
        return field.canPut(piece);
    }

    @Override
    public Field getLeft() {
        return left;
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public Stream<OriginalPiece> pieceStream() {
        return Stream.empty();
    }

    @Override
    public PieceCounter getPieceCounter() {
        return pieceCounter;
    }
}
