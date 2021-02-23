package components

import TSpinPossibleValidator
import core.field.FieldFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TSpinPossibleValidator {
    @Test
    fun validate() {
        val validator = TSpinPossibleValidator(2, 2)

        run {
            val field = FieldFactory.createField(
                "" +
                        "__________" +
                        "__________"
            )
            assertThat(validator.validate(field, 2)).isFalse
        }

        run {
            val field = FieldFactory.createField(
                "" +
                        "X___XXXXXX" +
                        "XX_XXXXXXX"
            )
            assertThat(validator.validate(field, 1)).isTrue
        }

        run {
            val field = FieldFactory.createField(
                "" +
                        "X___XXX___" +
                        "XX_XXXX___"
            )
            assertThat(validator.validate(field, 2)).isFalse
            assertThat(validator.validate(field, 3)).isTrue
        }
    }
}