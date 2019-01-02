package line.step3;

import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.srs.Rotate;

class KeyOriginalPiece {
    private final OriginalPiece originalPiece;
    private final int index;

    KeyOriginalPiece(OriginalPiece originalPiece, int index) {
        this.originalPiece = originalPiece;
        this.index = index;
    }

    Piece getPiece() {
        return originalPiece.getPiece();
    }

    Rotate getRotate() {
        return originalPiece.getRotate();
    }

    int getX() {
        return originalPiece.getX();
    }

    int getY() {
        return originalPiece.getY();
    }

    int getIndex() {
        return index;
    }

    OriginalPiece getOriginalPiece() {
        return originalPiece;
    }
}
