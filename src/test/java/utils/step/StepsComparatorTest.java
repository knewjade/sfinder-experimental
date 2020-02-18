package utils.step;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepsComparatorTest {
    @Test
    void equals() {
        StepsComparator comparator = new StepsComparator();
        assertThat(comparator.compare(Steps.impossible(), Steps.impossible())).isEqualTo(0);
        assertThat(comparator.compare(Steps.possible(10, 10), Steps.possible(10, 10))).isEqualTo(0);
    }

    @Test
    void compare() {
        StepsComparator comparator = new StepsComparator();

        // 解ありと解なしのケース
        compare(comparator, Steps.impossible(), Steps.possible(63, 15));

        // 合計値が異なるケース
        compare(comparator, Steps.possible(10, 10), Steps.possible(5, 5));
        compare(comparator, Steps.possible(10, 10), Steps.possible(5, 10));
        compare(comparator, Steps.possible(10, 10), Steps.possible(10, 5));

        compare(comparator, Steps.possible(9, 11), Steps.possible(9, 10));
        compare(comparator, Steps.possible(11, 9), Steps.possible(9, 10));

        // 合計値が同じケース  // sum = 20
        // moveが少ないケース
        compare(comparator, Steps.possible(10, 10), Steps.possible(9, 11));
        compare(comparator, Steps.possible(10, 10), Steps.possible(9, 10));

        //// moveが同じケース
        ///// holdが少ないケース
        compare(comparator, Steps.possible(10, 10), Steps.possible(10, 9));
    }

    void compare(StepsComparator comparator, short lowPriority, short highPriority) {
        assertThat(comparator.compare(lowPriority, lowPriority)).isEqualTo(0);
        assertThat(comparator.compare(highPriority, lowPriority)).isLessThan(0);
        assertThat(comparator.compare(lowPriority, highPriority)).isGreaterThan(0);
        assertThat(comparator.compare(highPriority, highPriority)).isEqualTo(0);
    }

    @Test
    void shouldUpdate() {
        StepsComparator comparator = new StepsComparator();

        assertThat(comparator.shouldUpdate(Steps.impossible(), Steps.possible(63, 15))).isTrue();
        assertThat(comparator.shouldUpdate(Steps.possible(63, 15), Steps.impossible())).isFalse();
        assertThat(comparator.shouldUpdate(Steps.possible(63, 15), Steps.possible(63, 15))).isFalse();
    }
}