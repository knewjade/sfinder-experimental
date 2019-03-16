package main;

import core.neighbor.SimpleOriginalPiece;

import java.util.List;

class Target {
    private final List<SimpleOriginalPiece> operations;
    private final SimpleOriginalPiece lastPiece;

    Target(List<SimpleOriginalPiece> operations, SimpleOriginalPiece lastPiece) {
        this.operations = operations;
        this.lastPiece = lastPiece;
    }

    List<SimpleOriginalPiece> getOperations() {
        return operations;
    }
}
