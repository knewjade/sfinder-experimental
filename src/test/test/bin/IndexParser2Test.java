package bin;

import core.mino.Piece;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;

import static core.mino.Piece.*;
import static org.assertj.core.api.Assertions.assertThat;

class IndexParser2Test {
    private IndexParser2 createDefaultParser(Integer... maxIndexes) {
        EnumMap<Piece, Byte> pieceToNumber = new EnumMap<>(Piece.class);
        pieceToNumber.put(S, (byte) 0);
        pieceToNumber.put(Z, (byte) 1);
        pieceToNumber.put(J, (byte) 2);
        pieceToNumber.put(L, (byte) 3);
        pieceToNumber.put(T, (byte) 4);
        pieceToNumber.put(O, (byte) 5);
        pieceToNumber.put(I, (byte) 6);
        return new IndexParser2(pieceToNumber, Arrays.asList(maxIndexes));
    }

    private Piece[] from(Piece... pieces) {
        return pieces;
    }

    @Test
    void case1() {
        IndexParser2 parser = createDefaultParser(1);
        assertThat(parser.parse(from(S))).isEqualTo(0);
        assertThat(parser.parse(from(Z))).isEqualTo(1);
        assertThat(parser.parse(from(J))).isEqualTo(2);
        assertThat(parser.parse(from(L))).isEqualTo(3);
        assertThat(parser.parse(from(T))).isEqualTo(4);
        assertThat(parser.parse(from(O))).isEqualTo(5);
        assertThat(parser.parse(from(I))).isEqualTo(6); // 7^2 - 1
    }

    @Test
    void case11() {
        IndexParser2 parser = createDefaultParser(1, 1);
        assertThat(parser.parse(from(S, S))).isEqualTo(0);
        assertThat(parser.parse(from(S, Z))).isEqualTo(1);
        assertThat(parser.parse(from(Z, S))).isEqualTo(7);
        assertThat(parser.parse(from(J, S))).isEqualTo(14);
        assertThat(parser.parse(from(I, I))).isEqualTo(48);  // 7^2 - 1
    }

    @Test
    void case2() {
        IndexParser2 parser = createDefaultParser(2);
        assertThat(parser.parse(from(S, Z))).isEqualTo(0);
        assertThat(parser.parse(from(S, J))).isEqualTo(1);
        assertThat(parser.parse(from(Z, S))).isEqualTo(6);
        assertThat(parser.parse(from(Z, J))).isEqualTo(7);
        assertThat(parser.parse(from(J, S))).isEqualTo(12);
        assertThat(parser.parse(from(I, O))).isEqualTo(41);  // 7*6 - 1
    }

    @Test
    void case7() {
        IndexParser2 parser = createDefaultParser(7);
        assertThat(parser.parse(from(S, Z, J, L, T, O, I))).isEqualTo(0);
        assertThat(parser.parse(from(S, Z, J, L, T, I, O))).isEqualTo(1);
        assertThat(parser.parse(from(I, O, T, L, J, Z, S))).isEqualTo(5039);  // 7! - 1
    }

    @Test
    void case155() {
        IndexParser2 parser = createDefaultParser(1, 5, 5);
        assertThat(parser.parse(from(S, S, Z, J, L, T, S, Z, J, L, T))).isEqualTo(0);
        assertThat(parser.parse(from(S, S, Z, J, L, T, S, Z, J, L, O))).isEqualTo(1);
        assertThat(parser.parse(from(S, S, Z, J, L, T, S, Z, J, L, I))).isEqualTo(2);
        assertThat(parser.parse(from(S, S, Z, J, L, T, I, O, T, L, J))).isEqualTo(2519);  // 7p5 - 1
        assertThat(parser.parse(from(S, S, Z, J, L, O, S, Z, J, L, T))).isEqualTo(2520);  // 7p5
        assertThat(parser.parse(from(S, I, O, T, L, J, I, O, T, L, J))).isEqualTo(2520 * 2520 - 1);  // 7p5*7p5 - 1
        assertThat(parser.parse(from(Z, S, Z, J, L, T, S, Z, J, L, T))).isEqualTo(2520 * 2520);  // 7p5*7p5
        assertThat(parser.parse(from(I, I, O, T, L, J, I, O, T, L, J))).isEqualTo(2520 * 2520 * 7 - 1);  // 7*7p5*7p5 - 1
    }
}