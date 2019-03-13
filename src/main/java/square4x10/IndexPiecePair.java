package square4x10;

import core.neighbor.SimpleOriginalPiece;

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
}
