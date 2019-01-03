package line.commons;

import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.srs.Rotate;

public class KeyOriginalPiece {
    private final OriginalPiece originalPiece;
    private final int index;

    public KeyOriginalPiece(OriginalPiece originalPiece, int index) {
        this.originalPiece = originalPiece;
        this.index = index;
    }

    public Piece getPiece() {
        return originalPiece.getPiece();
    }

    public Rotate getRotate() {
        return originalPiece.getRotate();
    }

    public int getX() {
        return originalPiece.getX();
    }

    public int getY() {
        return originalPiece.getY();
    }

    public int getIndex() {
        return index;
    }

    public OriginalPiece getOriginalPiece() {
        return originalPiece;
    }
}
