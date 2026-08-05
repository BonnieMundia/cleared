package app.cleared.data.derive

import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import app.cleared.data.model.Stage
import java.math.BigDecimal

/**
 * "What happened to the money" — frame `1e`, read as arithmetic rather than stored.
 *
 * Gross, less the fees charged in the record's own currency, converted at the rate that was
 * actually applied, less the fees charged in KES. On the sample record that is
 * EUR 640.00 − 32.00 − 1.50 = 606.50 at 145.82 = KES 88,439.83, less KES 220 = **KES 88,220**.
 */
object Ledger {

    /** Fees charged in the record's own currency, minor units. */
    fun feesInRecordCurrency(detail: RecordDetail): Long =
        detail.fees.filter { it.currency == detail.record.currency }.sumOf { it.amountMinor }

    /** Fees charged directly in KES, minor units. */
    fun feesInKes(detail: RecordDetail): Long =
        detail.fees.filter { it.currency == Currency.KES }.sumOf { it.amountMinor }

    /** The amount that reached the conversion — gross less same-currency fees. */
    fun convertedAmount(detail: RecordDetail): BigDecimal =
        Money.fromMinor(detail.record.grossMinor - feesInRecordCurrency(detail))

    /**
     * KES actually cleared, exact to the cent before the single rounding at the end.
     *
     * Returns zero for anything that has not landed: only LANDED money counts, which is what makes
     * a reversal contribute nothing to a platform's numerator while keeping its hours.
     */
    fun finalKesCleared(detail: RecordDetail): BigDecimal {
        val state = RecordState.of(detail)
        if (state.isSplit) {
            return state.settlementStates
                .filter { it.isLanded }
                .fold(BigDecimal.ZERO) { acc, s -> acc + settlementKesCleared(detail, s.settlement.id, s.settlement.amountMinor) }
        }
        if (state.displayStage != Stage.LANDED) return BigDecimal.ZERO
        return clearedFrom(detail, convertedAmount(detail), feesInKes(detail))
    }

    /**
     * A split record's numerator takes each settlement's cleared KES as it lands, so a record part
     * way through contributes part of its value — which is why the effective rate moves twice.
     */
    private fun settlementKesCleared(detail: RecordDetail, settlementId: Long, amountMinor: Long): BigDecimal {
        val ownFees = detail.fees.filter { it.settlementId == settlementId }
        val sameCurrencyFees = ownFees.filter { it.currency == detail.record.currency }.sumOf { it.amountMinor }
        val kesFees = ownFees.filter { it.currency == Currency.KES }.sumOf { it.amountMinor }
        val converted = Money.fromMinor(amountMinor - sameCurrencyFees)
        return clearedFrom(detail, converted, kesFees, settlementId)
    }

    /**
     * @param rates the current mid, used only when no snapshot exists. A landed record always has
     *        one and keeps it forever; a payout that bounced on the way to the bank may never have
     *        converted at all, and the money sitting in the wallet is then worth today's mid.
     */
    private fun clearedFrom(
        detail: RecordDetail,
        converted: BigDecimal,
        kesFeesMinor: Long,
        settlementId: Long? = null,
        rates: Map<Currency, BigDecimal> = emptyMap()
    ): BigDecimal {
        if (detail.record.currency == Currency.KES) {
            return converted - Money.fromMinor(kesFeesMinor)
        }
        val rate = detail.conversions.firstOrNull { it.settlementId == settlementId }?.rateApplied
            ?: detail.conversions.firstOrNull()?.rateApplied
            ?: rates[detail.record.currency]
            ?: return BigDecimal.ZERO
        return converted.multiply(rate) - Money.fromMinor(kesFeesMinor)
    }

    /**
     * What actually reached a wallet or a bank, whether or not it stayed there.
     *
     * For a landed record this is [finalKesCleared]. For a reversed one it is the figure frame `4a`
     * puts in the hero — the money exists, it is just in the wrong place, so it is rendered in
     * `onSurface` rather than in the rejected colour.
     */
    fun arrivedKes(detail: RecordDetail, rates: Map<Currency, BigDecimal> = emptyMap()): BigDecimal =
        clearedFrom(detail, convertedAmount(detail), feesInKes(detail), rates = rates)

    /**
     * What the record was worth at mid-market — the yardstick the cost percentage is measured
     * against. `1e` reads "6.6% of the mid-market value".
     */
    fun midMarketKes(detail: RecordDetail): BigDecimal {
        if (detail.record.currency == Currency.KES) return Money.fromMinor(detail.record.grossMinor)
        val snapshot = detail.conversions.firstOrNull() ?: return BigDecimal.ZERO
        return Money.fromMinor(detail.record.grossMinor).multiply(snapshot.midRate)
    }

    /** Total cost of getting this record paid, in KES: mid-market value less what cleared. */
    fun totalCostKes(detail: RecordDetail): BigDecimal = midMarketKes(detail) - finalKesCleared(detail)

    /**
     * What a reversal actually cost, in KES.
     *
     * The principal went back to the wallet, so it is not a loss and the record is not owed. The
     * fees are not refunded, and they are — which is why `4c` renders `KES 1,412 lost` rather than a
     * zero, and why this is the only record type whose net contribution can be negative.
     */
    fun lostKes(detail: RecordDetail, rates: Map<Currency, BigDecimal>): BigDecimal =
        detail.fees
            .filterNot { it.refundable }
            .fold(BigDecimal.ZERO) { acc, fee ->
                acc + Money.fromMinor(fee.amountMinor).multiply(rateFor(fee.currency, rates))
            }

    private fun rateFor(currency: Currency, rates: Map<Currency, BigDecimal>): BigDecimal =
        if (currency == Currency.KES) BigDecimal.ONE else rates[currency] ?: error("No mid rate for $currency")
}
