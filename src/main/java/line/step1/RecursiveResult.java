package line.step1;

import common.datastore.PieceCounter;
import core.field.Field;
import core.neighbor.OriginalPiece;

import java.util.stream.Stream;

public class RecursiveResult implements Result {
    private final Result prev;
    private final int lineY;
    private final Field left;
    private final Field field;
    private final int clearedLine;
    private final PieceCounter pieceCounter;
    private final OriginalPiece piece;

    RecursiveResult(Result prev, OriginalPiece piece, int lineY) {
        this.prev = prev;
        this.lineY = lineY;

        Field freezeLeft = prev.getLeft().freeze();
        freezeLeft.remove(piece);

        Field freezeField = prev.getField().freeze();
        freezeField.put(piece);

        PieceCounter pieceCounter = prev.getPieceCounter();

        this.left = freezeLeft;
        this.field = freezeField;
        this.clearedLine = freezeField.freeze().clearLine();
        this.piece = piece;
        this.pieceCounter = pieceCounter.removeAndReturnNew(new PieceCounter(Stream.of(piece.getPiece())));
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
        Stream<OriginalPiece> stream = prev.pieceStream();
        return Stream.concat(stream, Stream.of(piece));
    }

    @Override
    public PieceCounter getPieceCounter() {
        return pieceCounter;
    }
}
