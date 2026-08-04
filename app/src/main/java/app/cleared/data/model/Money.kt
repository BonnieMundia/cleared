package app.cleared.data.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money is BigDecimal in arithmetic and a scaled Long of minor units in storage. Never a Double.
 *
 * Rounding is HALF_UP everywhere and applied once, at the point a figure is presented — never
 * between intermediate steps. That is what makes the sample figures come out exact: the work
 * component of "owed" is 188,183.4 and the money component 58,935.6, which round to 188,183 and
 * 58,936 and still sum to the 247,119 total.
 */
object Money {

    val ROUNDING: RoundingMode = RoundingMode.HALF_UP

    /** Decimal amount to stored minor units. */
    fun toMinor(amount: BigDecimal): Long =
        amount.setScale(Currency.STORAGE_SCALE, ROUNDING).movePointRight(Currency.STORAGE_SCALE).longValueExact()

    /** Stored minor units back to a decimal amount. */
    fun fromMinor(minor: Long): BigDecimal = BigDecimal.valueOf(minor, Currency.STORAGE_SCALE)

    /** Parses the decimal strings used in design/sample_data.json. */
    fun minorOf(amount: String): Long = toMinor(BigDecimal(amount))

    /**
     * A KES figure as it is shown and asserted: whole shillings, rounded once.
     */
    fun toKes(amount: BigDecimal): Long = amount.setScale(0, ROUNDING).longValueExact()

    /**
     * A percentage held as a Double in the schema, lifted into exact decimal arithmetic.
     * `BigDecimal.valueOf` goes through the canonical decimal string, so 0.05 is exactly 0.05 and
     * not the binary approximation of it.
     */
    fun pct(value: Double): BigDecimal = BigDecimal.valueOf(value)

    /** `1 - pct`, the multiplier left after a commission or spread is taken. */
    fun remainderOf(pctValue: Double): BigDecimal = BigDecimal.ONE.subtract(pct(pctValue))
}
