package main.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FieldDataTest {
    @Test
    fun test1() {
        val fieldData = FieldData("HhA8IeB8HeA8SeAgWBAUAAAA")
        assertThat(fieldData.raw).isEqualTo("HhA8IeB8HeA8SeAgWBAUAAAA")
        assertThat(fieldData.representation).isEqualTo("HhA8IeB8HeA8SeAgWBAUAAAA")
        assertThat(fieldData.messageId).isEqualTo("HhA8IeB8HeA8SeAgWBAUAAAA")
    }

    @Test
    fun test2() {
        val fieldData = FieldData("/gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
        assertThat(fieldData.raw).isEqualTo("/gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
        assertThat(fieldData.representation).isEqualTo("_gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
        assertThat(fieldData.messageId).isEqualTo("_gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
    }

    @Test
    fun test3() {
        val fieldData = FieldData("_gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
        assertThat(fieldData.raw).isEqualTo("/gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
        assertThat(fieldData.representation).isEqualTo("_gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
        assertThat(fieldData.messageId).isEqualTo("_gA8FeA8BeA8FeD8FeD8FeA8JeAgWBAUAAAA")
    }

    @Test
    fun test4() {
        val fieldData = FieldData("+gD8HeA8FeD8FeC8QeAgWBAUAAAA")
        assertThat(fieldData.raw).isEqualTo("+gD8HeA8FeD8FeC8QeAgWBAUAAAA")
        assertThat(fieldData.representation).isEqualTo("+gD8HeA8FeD8FeC8QeAgWBAUAAAA")
        assertThat(fieldData.messageId).isEqualTo("-gD8HeA8FeD8FeC8QeAgWBAUAAAA")
    }

    @Test
    fun test5() {
        val fieldData = FieldData("+gD8DeA8CeA8EeE8BeB8AeD8CeB8AeA8JeAgWBAUAA?AA")
        assertThat(fieldData.raw).isEqualTo("+gD8DeA8CeA8EeE8BeB8AeD8CeB8AeA8JeAgWBAUAAAA")
        assertThat(fieldData.representation).isEqualTo("+gD8DeA8CeA8EeE8BeB8AeD8CeB8AeA8JeAgWBAUAAAA")
        assertThat(fieldData.messageId).isEqualTo("-gD8DeA8CeA8EeE8BeB8AeD8CeB8AeA8JeAgWBAUAAAA")
    }
}

