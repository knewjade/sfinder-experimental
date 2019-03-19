package helper;

import core.neighbor.SimpleOriginalPiece;

import java.util.List;

public class Target {
    private final List<SimpleOriginalPiece> operations;
    private final SimpleOriginalPiece lastPiece;

    public Target(List<SimpleOriginalPiece> operations, SimpleOriginalPiece lastPiece) {
        this.operations = operations;
        this.lastPiece = lastPiece;
    }

    public List<SimpleOriginalPiece> getOperations() {
        return operations;
    }
}
