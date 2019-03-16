package bin.pieces;

import core.mino.Piece;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PieceNumberTest {
    @Test
    void createS() {
        PieceNumber pieceNumber = new PieceNumber(Piece.S, 0);
        assertThat(pieceNumber)
                .returns(Piece.S, PieceNumber::getPiece)
                .returns(0, PieceNumber::getNumber)
                .returns((byte) 1, PieceNumber::getBitByte);
    }

    @Test
    void createI() {
        PieceNumber pieceNumber = new PieceNumber(Piece.I, 6);
        assertThat(pieceNumber)
                .returns(Piece.I, PieceNumber::getPiece)
                .returns(6, PieceNumber::getNumber)
                .returns((byte) 0b1000000, PieceNumber::getBitByte);
    }
}