package bin.pieces;

import core.mino.Piece;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PieceNumberConverterTest {
    @Test
    void defaultConverter() {
        PieceNumberConverter defaultConverter = PieceNumberConverter.createDefaultConverter();
        assertThat(defaultConverter.get(Piece.S).getNumber()).isEqualTo(0);
        assertThat(defaultConverter.get(Piece.Z).getNumber()).isEqualTo(1);
        assertThat(defaultConverter.get(Piece.J).getNumber()).isEqualTo(2);
        assertThat(defaultConverter.get(Piece.L).getNumber()).isEqualTo(3);
        assertThat(defaultConverter.get(Piece.T).getNumber()).isEqualTo(4);
        assertThat(defaultConverter.get(Piece.O).getNumber()).isEqualTo(5);
        assertThat(defaultConverter.get(Piece.I).getNumber()).isEqualTo(6);
    }
}