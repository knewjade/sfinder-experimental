package main

import main.domain.*
import main.percent.ResultsSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class ResultsSerializerTest {
    @Test
    fun test1() {
        val serializer = ResultsSerializer()
        val results = Results(Counter(0), listOf())
        assertThat(serializer.str(results)).isEqualTo("0")
    }

    @Test
    fun test2() {
        val serializer = ResultsSerializer()
        val results = Results(Counter(7), listOf())
        assertThat(serializer.str(results)).isEqualTo("7")
    }

    @Test
    fun test3() {
        val serializer = ResultsSerializer()
        val details = listOf(
                Result(MinoIndex(0), Counter(0), FieldData("9gA8IeA8IeA8IeA8SeAgH"))
        )
        val results = Results(Counter(7), details)
        assertThat(serializer.str(results)).isEqualTo("7?0,0,9gA8IeA8IeA8IeA8SeAgH")
    }

    @Test
    fun test4() {
        val serializer = ResultsSerializer()
        val details = listOf(
                Result(MinoIndex(0), Counter(0), FieldData("9gA8IeA8IeA8IeA8SeAgH")),
                Result(MinoIndex(1), Counter(100), FieldData("bhD8PeAgH"))
        )
        val results = Results(Counter(100), details)
        assertThat(serializer.str(results)).isEqualTo("100?0,0,9gA8IeA8IeA8IeA8SeAgH;1,100,bhD8PeAgH")
    }
}