package components

import core.field.FieldFactory
import core.mino.MinoFactory
import core.mino.MinoShifter
import core.srs.MinoRotation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SFSpinTest {
    @Test
    fun openings() {
        val height = 12
        val sfSpin = SFSpin(MinoFactory(), MinoShifter(), MinoRotation.create(), height)
        val results = sfSpin.run(FieldFactory.createField(height), 1)
        assertThat(results).hasSize(503634)
    }
}