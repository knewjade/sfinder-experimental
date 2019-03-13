import core.neighbor.SimpleOriginalPiece;

import java.util.Objects;

class IndexPiecePair {
    private final int index;
    private final SimpleOriginalPiece simpleOriginalPiece;

    IndexPiecePair(int index, SimpleOriginalPiece simpleOriginalPiece) {
        this.index = index;
        this.simpleOriginalPiece = simpleOriginalPiece;
    }

    int getIndex() {
        return index;
    }

    SimpleOriginalPiece getSimpleOriginalPiece() {
        return simpleOriginalPiece;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexPiecePair that = (IndexPiecePair) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }
}
