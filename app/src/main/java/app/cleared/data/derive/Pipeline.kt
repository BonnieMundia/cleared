package app.cleared.data.derive

import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * The Pipeline hero, its phase split, and the week subtotals under it — frame `1a`.
 *
 *     owedKes = Σ over records whose phase is WORK or MONEY of
 *               grossAmount × currentMidRate(currency)
 *
 * LANDED, REJECTED and REVERSED are all excluded. Reversed money is sitting in a wallet, not in
 * flight. For a split record the sum runs over its unlanded settlements, not the record's gross.
 */
data class PipelineTotals(
    val owedKes: Long,
    val workKes: Long,
    val moneyKes: Long,
    val owedByCurrency: Map<Currency, BigDecimal>,
    val weekSubtotalsKes: Map<LocalDate, Long>,
    val openCount: Int,
    val overdueCount: Int
)

object Pipeline {

    /**
     * @param rates the current mid rate per currency. KES needs no entry; it is its own rate.
     * @param overdueP90Days the platform's own p90 settle time, by platform id. Overdue fires on
     *        this and nothing else — not a global constant, not a mean.
     */
    fun totals(
        states: List<RecordState>,
        rates: Map<Currency, BigDecimal>,
        now: Instant,
        overdueP90Days: Map<Long, Int> = emptyMap(),
        graceDays: Map<Long, Int> = emptyMap()
    ): PipelineTotals {
        var work = BigDecimal.ZERO
        var money = BigDecimal.ZERO
        val byCurrency = mutableMapOf<Currency, BigDecimal>()
        val byWeek = mutableMapOf<LocalDate, BigDecimal>()
        var open = 0
        var overdue = 0

        for (state in states) {
            if (!state.isOwed) continue
            open++

            val currency = state.record.currency
            val rate = rateFor(currency, rates)

            for ((phase, minor) in state.owedMinorByPhase) {
                val amount = Money.fromMinor(minor)
                val kes = amount.multiply(rate)
                when (phase) {
                    Phase.WORK -> work += kes
                    Phase.MONEY -> money += kes
                    else -> Unit
                }
                byCurrency[currency] = (byCurrency[currency] ?: BigDecimal.ZERO) + amount
            }

            for (week in weekBuckets(state)) {
                byWeek[week.key] = (byWeek[week.key] ?: BigDecimal.ZERO) + week.value.multiply(rate)
            }

            if (isOverdue(state, now, overdueP90Days, graceDays)) overdue++
        }

        return PipelineTotals(
            owedKes = Money.toKes(work + money),
            workKes = Money.toKes(work),
            moneyKes = Money.toKes(money),
            owedByCurrency = byCurrency.mapValues { it.value.setScale(Currency.STORAGE_SCALE, Money.ROUNDING) },
            weekSubtotalsKes = byWeek.toSortedMap().mapValues { Money.toKes(it.value) },
            openCount = open,
            overdueCount = overdue
        )
    }

    /**
     * A part-paid row lives under the week its remainder is expected, and shows the remainder
     * rather than the record total — otherwise the week subtotal would not add up.
     */
    private fun weekBuckets(state: RecordState): List<Map.Entry<LocalDate, BigDecimal>> {
        if (!state.isSplit) {
            return mapOf(state.record.expectedWeekStart to Money.fromMinor(state.owedMinor)).entries.toList()
        }
        val buckets = mutableMapOf<LocalDate, BigDecimal>()
        for (s in state.settlementStates) {
            if (s.isLanded) continue
            if (s.phase != Phase.WORK && s.phase != Phase.MONEY) continue
            val week = s.settlement.expectedWeekStart ?: state.record.expectedWeekStart
            buckets[week] = (buckets[week] ?: BigDecimal.ZERO) + Money.fromMinor(s.settlement.amountMinor)
        }
        return buckets.entries.toList()
    }

    fun rateFor(currency: Currency, rates: Map<Currency, BigDecimal>): BigDecimal =
        if (currency == Currency.KES) BigDecimal.ONE else rates[currency]
            ?: error("No mid rate for $currency")

    /**
     *     isOverdue = phase in (WORK, MONEY)
     *                 && daysInCurrentPhase > platformSettleP90(platformId) + graceDays
     *
     * Settle times are long-tailed, so this is p90 and never a mean: on the sample data one
     * platform's mean is 13.4 d against a median of 11 d and a p90 of 19 d. Flagging on the mean
     * would miss every genuinely stuck record while nagging about normal ones.
     */
    fun isOverdue(
        state: RecordState,
        now: Instant,
        overdueP90Days: Map<Long, Int>,
        graceDays: Map<Long, Int> = emptyMap()
    ): Boolean {
        // A record that has landed cannot be late; it has arrived. DATA_MODEL.md's "phase in
        // (WORK, MONEY)" includes LANDED because landing is a money-phase stage, so this has to be
        // said separately — otherwise every long-settled record in the history reads as overdue,
        // which is what the settle-time screen's chase card first showed.
        if (state.displayStage == Stage.LANDED) return false

        val phase = state.displayStage.phase
        if (phase != Phase.WORK && phase != Phase.MONEY) return false
        val p90 = overdueP90Days[state.record.platformId] ?: return false
        val grace = graceDays[state.record.platformId] ?: 0
        return StageResolver.daysInCurrentPhase(state.detail, now) > (p90 + grace)
    }

    /** Days past the threshold, for the `31d · 7 over` age pill. Null when not overdue. */
    fun daysOver(
        state: RecordState,
        now: Instant,
        overdueP90Days: Map<Long, Int>,
        graceDays: Map<Long, Int> = emptyMap()
    ): Long? {
        if (!isOverdue(state, now, overdueP90Days, graceDays)) return null
        val threshold = (overdueP90Days[state.record.platformId] ?: return null) +
            (graceDays[state.record.platformId] ?: 0)
        return StageResolver.daysInCurrentPhase(state.detail, now) - threshold
    }
}
