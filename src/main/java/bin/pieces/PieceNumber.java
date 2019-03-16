package bin.pieces;

import core.mino.Piece;

public class PieceNumber {
    private final Piece piece;
    private final int number;
    private final byte bitByte;

    public PieceNumber(Piece piece, int number) {
        this.piece = piece;
        this.number = number;
        this.bitByte = (byte) (1 << number);
    }

    public Piece getPiece() {
        return piece;
    }

    public int getNumber() {
        return number;
    }

    public byte getBitByte() {
        return bitByte;
    }
}
