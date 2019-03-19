package mainv2;

import core.neighbor.SimpleOriginalPiece;

import java.util.List;

class Target {
    private final List<SimpleOriginalPiece> operations;
    private final SimpleOriginalPiece lastPiece;
    private final short moveAndRotate;

    public Target(List<SimpleOriginalPiece> operations, SimpleOriginalPiece lastPiece, short moveAndRotate) {
        this.operations = operations;
        this.lastPiece = lastPiece;
        this.moveAndRotate = moveAndRotate;
    }

    public List<SimpleOriginalPiece> getOperations() {
        return operations;
    }

    public short getMoveAndRotate() {
        return moveAndRotate;
    }
}
