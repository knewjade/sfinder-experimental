package utils.frame;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FramesComparatorTest {
    @Test
    void equals() {
        FramesComparator comparator = new FramesComparator();
        assertThat(comparator.compare(Frames.impossible(), Frames.impossible())).isEqualTo(0);
        assertThat(comparator.compare(Frames.possible(50, 5), Frames.possible(50, 5))).isEqualTo(0);
    }

    @Test
    void compare() {
        FramesComparator comparator = new FramesComparator();

        // 解ありと解なしのケース
        compare(comparator, Frames.impossible(), Frames.possible(50, 8));

        // 合計値が異なるケース
        compare(comparator, Frames.possible(50, 5), Frames.possible(45, 3));
        compare(comparator, Frames.possible(50, 5), Frames.possible(52, 0));
        compare(comparator, Frames.possible(50, 5), Frames.possible(48, 6));
    }

    void compare(FramesComparator comparator, byte lowPriority, byte highPriority) {
        assertThat(comparator.compare(lowPriority, lowPriority)).isEqualTo(0);
        assertThat(comparator.compare(highPriority, lowPriority)).isLessThan(0);
        assertThat(comparator.compare(lowPriority, highPriority)).isGreaterThan(0);
        assertThat(comparator.compare(highPriority, highPriority)).isEqualTo(0);
    }
}