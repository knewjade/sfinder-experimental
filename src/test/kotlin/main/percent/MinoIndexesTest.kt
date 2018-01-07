package main.percent

import lib.Randoms
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class MinoIndexesTest {
    @Test
    fun test1() {
        val numbers = (1..10).toList()
        val indexes = MinoIndexes(numbers)
        assertThat(indexes.numbers).containsAll(numbers)
    }

    @Test
    fun test2() {
        val numbers = (10 downTo 1).toList()
        val indexes = MinoIndexes(numbers)
        assertThat(indexes.numbers).containsAll(numbers)
    }

    @Test
    fun test3() {
        val numbers = (10 downTo 1).toList()
        val indexes = MinoIndexes(numbers)
        numbers.forEach {
            assertThat(indexes.contains(it)).`as`(it.toString()).isTrue()
        }
        assertThat(indexes.contains(100)).isFalse()
    }

    @Test
    fun random() {
        val randoms = Randoms()
        val all = (0..1023).toList()

        val numbers = randoms.sample(all, 10).toSet()
        val indexes = MinoIndexes(numbers.toList())

        all.forEach {
            assertThat(indexes.contains(it)).isEqualTo(numbers.contains(it))
        }
    }
}