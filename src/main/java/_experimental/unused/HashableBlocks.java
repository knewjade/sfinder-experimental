package _experimental.unused;

import core.mino.Piece;

import java.util.List;

// num of pieces <= 10 であること
// nullを含まないこと
class HashableBlocks {
    private final List<Piece> pieces;

    private final int hash;

    public HashableBlocks(List<Piece> pieces) {
        this.pieces = pieces;
        this.hash = calculateHash(pieces);
    }

    private int calculateHash(List<Piece> pieces) {
        int size = pieces.size();
        int number = pieces.get(size - 1).getNumber();
        for (int index = size - 2; 0 <= index; index--) {
            number *= 8;
            number += pieces.get(index).getNumber();
        }
        return number;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashableBlocks pieces = (HashableBlocks) o;
        return hash == pieces.hash;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
