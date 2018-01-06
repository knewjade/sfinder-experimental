package main.caller

import main.domain.Counter
import main.domain.FieldData
import main.domain.MinoIndex
import main.domain.Result
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ContentCallerTest {
    @Test
    fun call1() {
        val caller = ContentCaller("0")
        val results = caller.call()
        Assertions.assertThat(results.allCount).isEqualTo(Counter(0))
        Assertions.assertThat(results.details).isEmpty()
    }

    @Test
    fun call2() {
        val caller = ContentCaller("0?")
        val results = caller.call()
        Assertions.assertThat(results.allCount).isEqualTo(Counter(0))
        Assertions.assertThat(results.details).isEmpty()
    }

    @Test
    fun call3() {
        val caller = ContentCaller("7?")
        val results = caller.call()
        Assertions.assertThat(results.allCount).isEqualTo(Counter(7))
        Assertions.assertThat(results.details).isEmpty()
    }

    @Test
    fun call4() {
        val caller = ContentCaller("604800?172,604800,chD8OeAgWBAUAAAA;180,100,ehD8MeAgWBAUAAAA")
        val results = caller.call()
        Assertions.assertThat(results.allCount).isEqualTo(Counter(604800))
        Assertions.assertThat(results.details)
                .hasSize(2)
                .contains(Result(MinoIndex(172), Counter(604800), FieldData("chD8OeAgWBAUAAAA")))
                .contains(Result(MinoIndex(180), Counter(100), FieldData("ehD8MeAgWBAUAAAA")))
    }
}