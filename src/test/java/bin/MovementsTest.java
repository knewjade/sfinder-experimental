package bin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovementsTest {
// Format:
    //   YMMMMMMM
    //   RRRRHHHH

    @Test
    void testImpossible() {
        assertThat(Movements.impossible()).isEqualTo((short) 0);
    }

    @Test
    void testPossible() {
        assertThat(Movements.possible(0, 0, 0)).isEqualTo((short) 0b10000000_00000000);
        assertThat(Movements.possible(1, 0, 0)).isEqualTo((short) 0b10000001_00000000);
        assertThat(Movements.possible(0, 1, 0)).isEqualTo((short) 0b10000000_00010000);
        assertThat(Movements.possible(0, 0, 1)).isEqualTo((short) 0b10000000_00000001);
        assertThat(Movements.possible(63, 15, 15)).isEqualTo((short) 0b10111111_11111111);
    }

    @Test
    void testPossible2() {
        assertThat(Movements.possible(12, 15, 4))
                .isEqualTo(Movements.possible(Movements.possible(12, 15, 0), 4));
        assertThat(Movements.possible(1, 2, 3))
                .isEqualTo(Movements.possible(Movements.possible(1, 2, 0), 3));
        assertThat(Movements.possible(0, 0, 0))
                .isEqualTo(Movements.possible(Movements.possible(0, 0, 0), 0));
    }

    @Test
    void testPossibleForByte() {
        assertThat(Movements.possibleForByte(0, 0, false)).isEqualTo((byte) 0b0000_000_0);
        assertThat(Movements.possibleForByte(3, 4, false)).isEqualTo((byte) 0b0011_100_0);
        assertThat(Movements.possibleForByte(8, 1, true)).isEqualTo((byte) 0b1000_001_1);
    }

    @Test
    void testIsRangeIn() {
        assertThat(Movements.isRangeIn(0, 0, 0)).isTrue();
        assertThat(Movements.isRangeIn(1, 0, 0)).isTrue();
        assertThat(Movements.isRangeIn(0, 1, 0)).isTrue();
        assertThat(Movements.isRangeIn(0, 0, 1)).isTrue();
        assertThat(Movements.isRangeIn(63, 15, 15)).isTrue();

        assertThat(Movements.isRangeIn(64, 15, 15)).isFalse();
        assertThat(Movements.isRangeIn(63, 16, 15)).isFalse();
        assertThat(Movements.isRangeIn(63, 15, 16)).isFalse();
    }
}