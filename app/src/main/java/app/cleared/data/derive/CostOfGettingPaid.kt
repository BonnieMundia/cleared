package app.cleared.data.derive

import app.cleared.data.model.Currency
import app.cleared.data.model.FeeKind
import app.cleared.data.model.Money
import java.math.BigDecimal
import java.math.MathContext

/**
 * "Cost of getting paid · year to date" — the middle section of frame `1c`.
 *
 *     Σ FeeLine.amount converted to KES,
 *     plus FX spread computed as Σ (midRate − rateApplied) × convertedAmount,
 *     grouped by FeeLine.kind
 *
 * The spread is a real cost even though no line item is ever issued for it, which is why it is the
 * largest segment of the bar.
 */
data class CostBreakdown(
    val totalKes: Long,
    val byKind: Map<FeeKind, Long>,
    val landedKes: Long
) {
    /** "5.4% of everything that landed this year". */
    val pctOfLanded: Double
        get() = if (landedKes == 0L) 0.0 else totalKes.toDouble() / landedKes * 100.0
}

object CostOfGettingPaid {

    fun of(states: List<RecordState>, rates: Map<Currency, BigDecimal>): CostBreakdown {
        val byKind = mutableMapOf<FeeKind, BigDecimal>()
        var landed = BigDecimal.ZERO

        for (state in states) {
            val detail = state.detail
            landed += Ledger.finalKesCleared(detail)

            for (fee in detail.fees) {
                val kes = Money.fromMinor(fee.amountMinor)
                    .multiply(Pipeline.rateFor(fee.currency, rates))
                byKind[fee.kind] = (byKind[fee.kind] ?: BigDecimal.ZERO) + kes
            }

            val snapshot = detail.conversions.firstOrNull() ?: continue
            val converted = Ledger.convertedAmount(detail)
            val spread = (snapshot.midRate - snapshot.rateApplied).multiply(converted)
            if (spread.signum() != 0) {
                byKind[FeeKind.FX_SPREAD] = (byKind[FeeKind.FX_SPREAD] ?: BigDecimal.ZERO) + spread
            }
        }

        val rounded = byKind.mapValues { Money.toKes(it.value) }
        return CostBreakdown(
            totalKes = rounded.values.sum(),
            byKind = rounded,
            landedKes = Money.toKes(landed)
        )
    }

    /** The spread a single conversion cost, in KES. */
    fun spreadPct(midRate: BigDecimal, rateApplied: BigDecimal): BigDecimal =
        (midRate - rateApplied).divide(midRate, MathContext.DECIMAL64).multiply(BigDecimal(100))
}
