package app.cleared.ui.format

import app.cleared.data.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

/**
 * CLAUDE.md rule 6, as tests: money always carries a three-letter code and never a symbol, KES to
 * zero decimals, USD and EUR to two, negatives with a minus sign and never parentheses.
 */
class MoneyFormatTest {

    @Test
    fun `KES has no decimals and groups with commas`() {
        assertEquals("KES 247,119", MoneyFormat.format(Currency.KES, BigDecimal("247119")))
        assertEquals("KES 88,220", MoneyFormat.format(Currency.KES, BigDecimal("88219.83")))
        assertEquals("KES 0", MoneyFormat.format(Currency.KES, BigDecimal.ZERO))
    }

    @Test
    fun `USD and EUR carry two decimals`() {
        assertEquals("USD 184.00", MoneyFormat.format(Currency.USD, BigDecimal("184")))
        assertEquals("EUR 640.00", MoneyFormat.format(Currency.EUR, BigDecimal("640.00")))
        assertEquals("USD 947.50", MoneyFormat.format(Currency.USD, BigDecimal("947.5")))
    }

    /** `−EUR 32.00` — a real minus sign ahead of the code, never a bracket. */
    @Test
    fun `negatives use a minus sign and never parentheses`() {
        val formatted = MoneyFormat.format(Currency.EUR, BigDecimal("-32.00"))
        assertEquals("−EUR 32.00", formatted)
        assertTrue(formatted.startsWith(MoneyFormat.MINUS))
        assertFalse(formatted.contains("("))
        assertFalse(formatted.contains("-"))
    }

    @Test
    fun `no currency symbol ever appears`() {
        val samples = listOf(
            MoneyFormat.format(Currency.USD, BigDecimal("184")),
            MoneyFormat.format(Currency.EUR, BigDecimal("640")),
            MoneyFormat.format(Currency.KES, BigDecimal("247119"))
        )
        for (s in samples) {
            assertFalse(s, s.any { it in "$€£¥" })
            assertTrue(s, s.substringAfter(MoneyFormat.MINUS).take(3).all { it.isUpperCase() })
        }
    }

    /**
     * Grouping stays a comma whatever the device locale is. A locale that groups with a space or a
     * point would break the mono column alignment the whole design rests on.
     */
    @Test
    fun `formatting is independent of the default locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("KES 247,119", MoneyFormat.format(Currency.KES, BigDecimal("247119")))
            assertEquals("USD 1,234.56", MoneyFormat.format(Currency.USD, BigDecimal("1234.56")))
            Locale.setDefault(Locale.forLanguageTag("sw-KE"))
            assertEquals("KES 247,119", MoneyFormat.format(Currency.KES, BigDecimal("247119")))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `rounding is half up, once`() {
        assertEquals("KES 64,393", MoneyFormat.format(Currency.KES, BigDecimal("64392.6")))
        assertEquals("KES 58,936", MoneyFormat.format(Currency.KES, BigDecimal("58935.6")))
        assertEquals("KES 188,183", MoneyFormat.format(Currency.KES, BigDecimal("188183.4")))
    }

    @Test
    fun `minor units round-trip`() {
        assertEquals("USD 184.00", MoneyFormat.formatMinor(Currency.USD, 18_400))
        assertEquals("EUR 606.50", MoneyFormat.formatMinor(Currency.EUR, 60_650))
    }

    @Test
    fun `hours drop a trailing zero but keep a half`() {
        assertEquals("18.5 h", MoneyFormat.hours(18.5))
        assertEquals("12 h", MoneyFormat.hours(12.0))
        assertEquals("1.5 h", MoneyFormat.hours(1.5))
    }

    @Test
    fun `rates always show two decimals`() {
        assertEquals("145.82", MoneyFormat.rate(BigDecimal("145.82")))
        assertEquals("128.40", MoneyFormat.rate(BigDecimal("128.4")))
    }

    @Test
    fun `percentages drop a trailing zero`() {
        assertEquals("6.6%", MoneyFormat.percent(6.609))
        assertEquals("94%", MoneyFormat.percent(94.0))
        assertEquals("5.4%", MoneyFormat.percent(5.38))
    }
}
