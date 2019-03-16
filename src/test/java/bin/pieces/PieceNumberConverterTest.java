package bin.pieces;

import core.mino.Piece;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PieceNumberConverterTest {
    @Test
    void getFromPiece() {
        PieceNumberConverter defaultConverter = PieceNumberConverter.createDefaultConverter();
        assertThat(defaultConverter.get(Piece.S).getNumber()).isEqualTo(0);
        assertThat(defaultConverter.get(Piece.Z).getNumber()).isEqualTo(1);
        assertThat(defaultConverter.get(Piece.J).getNumber()).isEqualTo(2);
        assertThat(defaultConverter.get(Piece.L).getNumber()).isEqualTo(3);
        assertThat(defaultConverter.get(Piece.T).getNumber()).isEqualTo(4);
        assertThat(defaultConverter.get(Piece.O).getNumber()).isEqualTo(5);
        assertThat(defaultConverter.get(Piece.I).getNumber()).isEqualTo(6);
    }

    @Test
    void getFromNumber() {
        PieceNumberConverter defaultConverter = PieceNumberConverter.createDefaultConverter();
        assertThat(defaultConverter.get(0).getPiece()).isEqualTo(Piece.S);
        assertThat(defaultConverter.get(1).getPiece()).isEqualTo(Piece.Z);
        assertThat(defaultConverter.get(2).getPiece()).isEqualTo(Piece.J);
        assertThat(defaultConverter.get(3).getPiece()).isEqualTo(Piece.L);
        assertThat(defaultConverter.get(4).getPiece()).isEqualTo(Piece.T);
        assertThat(defaultConverter.get(5).getPiece()).isEqualTo(Piece.O);
        assertThat(defaultConverter.get(6).getPiece()).isEqualTo(Piece.I);
    }
}