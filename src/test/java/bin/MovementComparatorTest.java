package bin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovementComparatorTest {
    @Test
    void equals() {
        MovementComparator comparator = new MovementComparator();
        assertThat(comparator.compare(Movements.impossible(), Movements.impossible())).isEqualTo(0);
    }

    @Test
    void compare() {
        MovementComparator comparator = new MovementComparator();

        // 解ありと解なしのケース
        compare(comparator, Movements.impossible(), Movements.possible(63, 15, 15));

        // 合計値が異なるケース
        compare(comparator, Movements.possible(10, 10, 10), Movements.possible(5, 5, 5));
        compare(comparator, Movements.possible(10, 10, 10), Movements.possible(5, 10, 10));
        compare(comparator, Movements.possible(10, 10, 10), Movements.possible(10, 5, 10));
        compare(comparator, Movements.possible(10, 10, 10), Movements.possible(10, 10, 5));

        compare(comparator, Movements.possible(9, 11, 11), Movements.possible(9, 10, 11));
        compare(comparator, Movements.possible(11, 9, 11), Movements.possible(9, 10, 11));
        compare(comparator, Movements.possible(11, 11, 9), Movements.possible(9, 10, 11));

        compare(comparator, Movements.possible(9, 11, 11), Movements.possible(11, 10, 9));
        compare(comparator, Movements.possible(11, 9, 11), Movements.possible(11, 10, 9));
        compare(comparator, Movements.possible(11, 11, 9), Movements.possible(11, 10, 9));

        // 合計値が同じケース  // sum = 30
        // moveが少ないケース
        compare(comparator, Movements.possible(10, 10, 10), Movements.possible(9, 11, 10));
        compare(comparator, Movements.possible(10, 10, 10), Movements.possible(9, 10, 11));

        //// moveが同じケース
        ///// holdが少ないケース
        compare(comparator, Movements.possible(10, 10, 10), Movements.possible(10, 11, 9));

        // 合計値で比較しているため、moveとholdが同じで、rotateが小さいケースは存在しない
    }

    void compare(MovementComparator comparator, short lowPriority, short highPriority) {
        assertThat(comparator.compare(lowPriority, lowPriority)).isEqualTo(0);
        assertThat(comparator.compare(highPriority, lowPriority)).isLessThan(0);
        assertThat(comparator.compare(lowPriority, highPriority)).isGreaterThan(0);
        assertThat(comparator.compare(highPriority, highPriority)).isEqualTo(0);
    }

    @Test
    void shouldUpdate() {
        MovementComparator comparator = new MovementComparator();

        assertThat(comparator.shouldUpdate(Movements.impossible(), Movements.possible(63, 15, 15))).isTrue();
        assertThat(comparator.shouldUpdate(Movements.possible(63, 15, 15), Movements.impossible())).isFalse();
        assertThat(comparator.shouldUpdate(Movements.possible(63, 15, 15), Movements.possible(63, 15, 15))).isFalse();
    }
}