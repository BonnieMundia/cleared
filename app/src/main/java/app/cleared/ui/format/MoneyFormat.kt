package app.cleared.ui.format

import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money as the app writes it, everywhere: `KES 247,119`, `USD 184.00`, `−EUR 32.00`.
 *
 * Three currencies are in play and `$` is ambiguous between them, so the three-letter code is
 * always present and a symbol never is. KES renders to zero decimals, USD and EUR to two. Negatives
 * take a real minus sign — U+2212, not a hyphen — and never parentheses.
 *
 * The formatter is locale-independent by construction: grouping is a comma and the decimal mark a
 * point regardless of device locale, because the figures have to line up in a mono column and a
 * locale that groups with spaces would break the alignment the whole design rests on.
 */
object MoneyFormat {

    /** U+2212 MINUS SIGN. Wider than a hyphen and the same width as a digit in Plex Mono. */
    const val MINUS = "−"

    private val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    private val byScale = (0..2).associateWith { scale ->
        DecimalFormat(if (scale == 0) "#,##0" else "#,##0." + "0".repeat(scale), symbols)
    }

    /** `KES 247,119`. The currency code, a single space, then the amount. */
    fun format(currency: Currency, amount: BigDecimal): String {
        val scaled = amount.setScale(currency.displayScale, Money.ROUNDING)
        val sign = if (scaled.signum() < 0) MINUS else ""
        val digits = byScale.getValue(currency.displayScale).format(scaled.abs())
        return "$sign${currency.name} $digits"
    }

    /** The same, from stored minor units. */
    fun formatMinor(currency: Currency, minor: Long): String =
        format(currency, Money.fromMinor(minor))

    /** A whole-shilling KES figure, already rounded by a derivation. */
    fun kes(amount: Long): String = format(Currency.KES, BigDecimal.valueOf(amount))

    /** Just the digits, for the hero figure where `KES` is set separately at its own size. */
    fun digits(currency: Currency, amount: BigDecimal): String {
        val scaled = amount.setScale(currency.displayScale, Money.ROUNDING)
        val sign = if (scaled.signum() < 0) MINUS else ""
        return sign + byScale.getValue(currency.displayScale).format(scaled.abs())
    }

    /** Hours, as the row and the Platforms card write them: `18.5 h`, `12 h`. */
    fun hours(value: Double): String {
        val rounded = BigDecimal.valueOf(value).setScale(1, Money.ROUNDING)
        val text = if (rounded.remainder(BigDecimal.ONE).signum() == 0) {
            rounded.setScale(0, Money.ROUNDING).toPlainString()
        } else {
            rounded.toPlainString()
        }
        return "$text h"
    }

    /** A rate, always two decimals: `145.82`. */
    fun rate(value: BigDecimal): String = byScale.getValue(2).format(value.setScale(2, Money.ROUNDING))

    /** A percentage, one decimal unless it is whole: `6.6%`, `94%`. */
    fun percent(value: Double, decimals: Int = 1): String {
        val rounded = BigDecimal.valueOf(value).setScale(decimals, Money.ROUNDING)
        val text = if (rounded.remainder(BigDecimal.ONE).signum() == 0) {
            rounded.setScale(0, Money.ROUNDING).toPlainString()
        } else {
            rounded.toPlainString()
        }
        return "$text%"
    }
}
