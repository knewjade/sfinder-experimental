package best

import common.datastore.PieceCounter
import core.mino.Piece
import main.domain.Counter
import main.domain.Cycle
import main.domain.FieldData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BestKtTest {
    @Test
    fun getPieceCounter() {
        assertThat(getPieceCounters(Cycle(1))).hasSize(32)
    }

    @Test
    fun best() {
        val pieceCounter = PieceCounter(listOf(Piece.T, Piece.I, Piece.J, Piece.L))
        val best = getBest(pieceCounter, Cycle(1))!!
        assertThat(best).returns(Counter(3196), { pair -> pair.first.success })
        assertThat(best).returns(FieldData("9gA8IeA8BeA8FeE8BeG8EeA8JeAgWBAUAAAA"), { pair -> pair.first.fieldData })
        assertThat(best).returns(5040, { pair -> pair.second })
    }

    @Test
    fun color() {
        val pieceCounter = PieceCounter(listOf(Piece.T, Piece.I, Piece.J, Piece.L))
        val fieldData = FieldData("9gA8IeA8BeA8FeE8BeG8EeA8JeAgWBAUAAAA")
        val obj = Obj()
        val colorize = colorize(pieceCounter, fieldData, obj)
        assertThat(colorize).isEqualTo("9gglIeglBewwFehlywBei0zhEeg0JeAgH")
    }
}