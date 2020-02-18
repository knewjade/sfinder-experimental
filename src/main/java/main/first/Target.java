package main.first;

import core.neighbor.SimpleOriginalPiece;

import java.util.List;

class Target {
    private final int solutionIndex;
    private final List<SimpleOriginalPiece> operations;
    private final SimpleOriginalPiece lastPiece;
    private final short moveAndRotate;

    public Target(int solutionIndex, List<SimpleOriginalPiece> operations, SimpleOriginalPiece lastPiece, short moveAndRotate) {
        this.solutionIndex = solutionIndex;
        this.operations = operations;
        this.lastPiece = lastPiece;
        this.moveAndRotate = moveAndRotate;
    }

    public int getSolutionIndex() {
        return solutionIndex;
    }

    public List<SimpleOriginalPiece> getOperations() {
        return operations;
    }

    public short getMoveAndRotate() {
        return moveAndRotate;
    }
}
