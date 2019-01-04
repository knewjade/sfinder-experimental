package line.commons;

import common.datastore.Operation;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.srs.Rotate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KeyOriginalPiece implements Operation, Comparable<KeyOriginalPiece> {
    private final OriginalPiece originalPiece;
    private final int index;

    public KeyOriginalPiece(OriginalPiece originalPiece, int index) {
        this.originalPiece = originalPiece;
        this.index = index;
    }

    @Override
    public Piece getPiece() {
        return originalPiece.getPiece();
    }

    @Override
    public Rotate getRotate() {
        return originalPiece.getRotate();
    }

    @Override
    public int getX() {
        return originalPiece.getX();
    }

    @Override
    public int getY() {
        return originalPiece.getY();
    }

    public int getIndex() {
        return index;
    }

    public OriginalPiece getOriginalPiece() {
        return originalPiece;
    }

    @Override
    public int compareTo(@NotNull KeyOriginalPiece o) {
        return Integer.compare(index, o.index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyOriginalPiece that = (KeyOriginalPiece) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }
}
