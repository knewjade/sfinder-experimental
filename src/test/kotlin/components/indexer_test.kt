package components

import core.mino.MinoFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IndexerTest {
    @Test
    fun maxIndex() {
        val indexer = Indexer(MinoFactory(), 12)
        assertThat(indexer.maxIndex).isEqualTo(3360)
    }

    @Test
    fun parse() {
        val indexer = Indexer(MinoFactory(), 12)
        (0 until indexer.maxIndex).forEach { index ->
            val operation = indexer.toMinoOperationWithKey(index)
            val result = indexer.toIndex(operation.piece, operation.rotate, operation.x, operation.y)
            assertThat(result).isEqualTo(index)
        }
    }
}