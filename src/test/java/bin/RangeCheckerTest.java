package bin;

import core.mino.Piece;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;

import static core.mino.Piece.*;
import static org.assertj.core.api.Assertions.assertThat;

class RangeCheckerTest {
    private static RangeChecker createDefaultChecker(Integer... maxIndexes) {
        EnumMap<Piece, Integer> pieceToNumber = new EnumMap<>(Piece.class);
        pieceToNumber.put(S, 0);
        pieceToNumber.put(Z, 1);
        pieceToNumber.put(J, 2);
        pieceToNumber.put(L, 3);
        pieceToNumber.put(T, 4);
        pieceToNumber.put(O, 5);
        pieceToNumber.put(I, 6);
        return new RangeChecker(pieceToNumber, Arrays.asList(maxIndexes));
    }

    private Piece[] from(Piece... pieces) {
        return pieces;
    }

    @Test
    void case1() {
        RangeChecker rangeChecker = createDefaultChecker(1);
        assertThat(rangeChecker.check(from(S))).isTrue();
        assertThat(rangeChecker.check(from(Z))).isTrue();
        assertThat(rangeChecker.check(from(J))).isTrue();
        assertThat(rangeChecker.check(from(T))).isTrue();
        assertThat(rangeChecker.check(from(O))).isTrue();
        assertThat(rangeChecker.check(from(I))).isTrue();
    }

    @Test
    void case2() {
        RangeChecker rangeChecker = createDefaultChecker(2);
        assertThat(rangeChecker.check(from(S, S))).isFalse();
        assertThat(rangeChecker.check(from(S, Z))).isTrue();
        assertThat(rangeChecker.check(from(S, J))).isTrue();
        assertThat(rangeChecker.check(from(S, L))).isTrue();
        assertThat(rangeChecker.check(from(S, T))).isTrue();
        assertThat(rangeChecker.check(from(S, O))).isTrue();
        assertThat(rangeChecker.check(from(S, I))).isTrue();

        assertThat(rangeChecker.check(from(Z, Z))).isFalse();
        assertThat(rangeChecker.check(from(J, J))).isFalse();
        assertThat(rangeChecker.check(from(T, T))).isFalse();
        assertThat(rangeChecker.check(from(O, O))).isFalse();
        assertThat(rangeChecker.check(from(I, I))).isFalse();
    }

    @Test
    void case3() {
        RangeChecker rangeChecker = createDefaultChecker(3);
        assertThat(rangeChecker.check(from(S, Z, J))).isTrue();
        assertThat(rangeChecker.check(from(S, Z, S))).isFalse();
        assertThat(rangeChecker.check(from(S, Z, Z))).isFalse();
        assertThat(rangeChecker.check(from(S, S, S))).isFalse();
    }

    @Test
    void case12() {
        RangeChecker rangeChecker = createDefaultChecker(1, 2);
        assertThat(rangeChecker.check(from(J, L, T))).isTrue();
        assertThat(rangeChecker.check(from(J, J, T))).isTrue();
        assertThat(rangeChecker.check(from(J, L, J))).isTrue();
        assertThat(rangeChecker.check(from(J, J, J))).isFalse();
    }
}