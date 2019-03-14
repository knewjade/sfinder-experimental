package bin;

import core.mino.Piece;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static core.mino.Piece.*;
import static org.assertj.core.api.Assertions.assertThat;

class IndexParserTest {
    @Test
    void case1() {
        IndexParser parser = new IndexParser(Collections.singletonList(1));
        assertThat(parser.parse(to(T))).isEqualTo(0);
        assertThat(parser.parse(to(I))).isEqualTo(1);
        assertThat(parser.parse(to(L))).isEqualTo(2);
        assertThat(parser.parse(to(J))).isEqualTo(3);
        assertThat(parser.parse(to(S))).isEqualTo(4);
        assertThat(parser.parse(to(Z))).isEqualTo(5);
        assertThat(parser.parse(to(O))).isEqualTo(6); // 7^2 - 1
    }

    private Piece[] to(Piece... pieces) {
        return pieces;
    }

    @Test
    void case11() {
        IndexParser parser = new IndexParser(Arrays.asList(1, 1));
        assertThat(parser.parse(to(T, T))).isEqualTo(0);
        assertThat(parser.parse(to(T, I))).isEqualTo(1);
        assertThat(parser.parse(to(I, T))).isEqualTo(7);
        assertThat(parser.parse(to(L, T))).isEqualTo(14);
        assertThat(parser.parse(to(O, O))).isEqualTo(48);  // 7^2 - 1
    }

    @Test
    void case2() {
        IndexParser parser = new IndexParser(Collections.singletonList(2));
        assertThat(parser.parse(to(T, I))).isEqualTo(0);
        assertThat(parser.parse(to(T, L))).isEqualTo(1);
        assertThat(parser.parse(to(I, T))).isEqualTo(6);
        assertThat(parser.parse(to(I, L))).isEqualTo(7);
        assertThat(parser.parse(to(L, T))).isEqualTo(12);
        assertThat(parser.parse(to(O, Z))).isEqualTo(41);  // 7*6 - 1
    }

    @Test
    void case7() {
        IndexParser parser = new IndexParser(Collections.singletonList(7));
        assertThat(parser.parse(to(T, I, L, J, S, Z, O))).isEqualTo(0);
        assertThat(parser.parse(to(T, I, L, J, S, O, Z))).isEqualTo(1);
        assertThat(parser.parse(to(O, Z, S, J, L, I, T))).isEqualTo(5039);  // 7! - 1
    }

    @Test
    void case155() {
        IndexParser parser = new IndexParser(Arrays.asList(1, 5, 5));
        assertThat(parser.parse(to(T, T, I, L, J, S, T, I, L, J, S))).isEqualTo(0);
        assertThat(parser.parse(to(T, T, I, L, J, S, T, I, L, J, Z))).isEqualTo(1);
        assertThat(parser.parse(to(T, T, I, L, J, S, T, I, L, J, O))).isEqualTo(2);
        assertThat(parser.parse(to(T, T, I, L, J, S, O, Z, S, J, L))).isEqualTo(2519);  // 7p5 - 1
        assertThat(parser.parse(to(T, T, I, L, J, Z, T, I, L, J, S))).isEqualTo(2520);  // 7p5
        assertThat(parser.parse(to(T, O, Z, S, J, L, O, Z, S, J, L))).isEqualTo(2520 * 2520 - 1);  // 7p5*7p5 - 1
        assertThat(parser.parse(to(I, T, I, L, J, S, T, I, L, J, S))).isEqualTo(2520 * 2520);  // 7p5*7p5
        assertThat(parser.parse(to(O, O, Z, S, J, L, O, Z, S, J, L))).isEqualTo(2520 * 2520 * 7 - 1);  // 7*7p5*7p5 - 1
    }
}