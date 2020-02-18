package main.verify;

import core.mino.Piece;

import java.util.List;

class Order {
    private final List<Piece> pieces;
    private final Piece hold;
    private final int holdCount;

    Order(List<Piece> pieces, Piece hold, int holdCount) {
        this.pieces = pieces;
        this.hold = hold;
        this.holdCount = holdCount;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public Piece getHold() {
        return hold;
    }

    public int getHoldCount() {
        return holdCount;
    }
}