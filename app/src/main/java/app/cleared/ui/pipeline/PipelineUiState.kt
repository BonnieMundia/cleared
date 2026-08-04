package app.cleared.ui.pipeline

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.derive.Ledger
import app.cleared.data.derive.Pipeline
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.StageResolver
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import app.cleared.data.model.Stage
import app.cleared.ui.components.RecordRowUi
import app.cleared.ui.components.agePillText
import app.cleared.ui.components.stageLabel
import app.cleared.ui.format.MoneyFormat
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/** A week group, or the "Needs attention" band that pins above them all. */
data class RowGroup(
    val title: String,
    val subtitleFigure: String?,
    val rows: List<RecordRowUi>,
    val isNeedsAttention: Boolean = false
)

data class PipelineUiState(
    val owedKes: Long = 0,
    val workKes: Long = 0,
    val moneyKes: Long = 0,
    val components: List<String> = emptyList(),
    val openCount: Int = 0,
    val overdueCount: Int = 0,
    val groups: List<RowGroup> = emptyList(),
    val offline: Boolean = false,
    val queuedWrites: Int = 0,
    val lastSyncedLabel: String? = null,
    val loading: Boolean = true
) {
    val isEmpty: Boolean get() = !loading && groups.isEmpty()

    /** Violet's share of the split bar. Green fills the rest. */
    val workFraction: Float
        get() = if (owedKes == 0L) 0f else (workKes.toDouble() / owedKes).toFloat().coerceIn(0f, 1f)

    val heroDigits: String get() = MoneyFormat.digits(Currency.KES, BigDecimal.valueOf(owedKes))
    val workLegend: String get() = "Work ${MoneyFormat.kes(workKes)}"
    val moneyLegend: String get() = "Money ${MoneyFormat.kes(moneyKes)}"

    /**
     * `7 open records · 2 past their usual settle time`.
     *
     * The second clause is dropped when nothing is overdue rather than rendered as a zero — the
     * design never shows a count of nothing, and "0 past their usual settle time" reads as a
     * complaint about a pipeline that is behaving.
     */
    val caption: String
        get() {
            val records = if (openCount == 1) "1 open record" else "$openCount open records"
            return if (overdueCount == 0) records
            else "$records · $overdueCount past their usual settle time"
        }
}

/**
 * Turns the derived record states into the rows and groups frame `1a` draws.
 *
 * Everything here is presentation: which figure a row shows, what its age pill says, which group it
 * belongs to. The arithmetic all happened in `data.derive` and is not repeated.
 */
object PipelineMapper {

    fun build(
        states: List<RecordState>,
        platforms: List<PlatformEntity>,
        rates: Map<Currency, BigDecimal>,
        p90ByPlatform: Map<Long, Int>,
        now: Instant,
        today: LocalDate,
        offline: Boolean = false,
        queuedWrites: Int = 0,
        lastSyncedLabel: String? = null
    ): PipelineUiState {
        val graceDays = platforms.associate { it.id to it.graceDays }
        val totals = Pipeline.totals(states, rates, now, p90ByPlatform, graceDays)
        val names = platforms.associate { it.id to it.name }

        // Reversed records pin above the week groups: the money reached a wallet and came back, so
        // it is not owed and has no arrival week to sit under.
        val reversed = states.filter { it.displayStage == Stage.REVERSED }
        val needsAttention = if (reversed.isEmpty()) null else RowGroup(
            title = "Needs attention",
            subtitleFigure = reversed.size.toString(),
            rows = reversed.map { row(it, names, rates, now, p90ByPlatform, graceDays) },
            isNeedsAttention = true
        )

        // What belongs on Pipeline: everything still owed, plus rejections whose week has not yet
        // passed — a record rejected last week is still news, one rejected in May is history and
        // belongs on its platform's card, not here. Landed records leave outright, and reversed
        // ones are already pinned above.
        val currentWeek = WeekLabel.currentWeekStart(today)
        val weekly = states
            .filter { it.displayStage != Stage.REVERSED }
            .filter { it.isOwed || (it.displayStage == Stage.REJECTED && !weekOf(it).isBefore(currentWeek)) }
            .groupBy { weekOf(it) }
            .toSortedMap()
            .map { (week, group) ->
                RowGroup(
                    title = WeekLabel.of(week, today),
                    subtitleFigure = totals.weekSubtotalsKes[week]?.let { MoneyFormat.kes(it) },
                    rows = group
                        .sortedWith(compareBy({ it.displayStage.order }, { it.record.id }))
                        .map { row(it, names, rates, now, p90ByPlatform, graceDays) }
                )
            }

        return PipelineUiState(
            owedKes = totals.owedKes,
            workKes = totals.workKes,
            moneyKes = totals.moneyKes,
            components = totals.owedByCurrency
                .toSortedMap(compareBy { it.name })
                .map { (currency, amount) -> MoneyFormat.format(currency, amount) },
            openCount = totals.openCount,
            overdueCount = totals.overdueCount,
            groups = listOfNotNull(needsAttention) + weekly,
            offline = offline,
            queuedWrites = queuedWrites,
            lastSyncedLabel = lastSyncedLabel,
            loading = false
        )
    }

    /**
     * A part-paid record lives under the week its **remainder** is expected, because that is the
     * amount the row shows and the week subtotal has to add up.
     */
    private fun weekOf(state: RecordState): LocalDate {
        if (!state.isSplit) return state.record.expectedWeekStart
        val firstUnlanded = state.settlementStates.firstOrNull { !it.isLanded }
        return firstUnlanded?.settlement?.expectedWeekStart ?: state.record.expectedWeekStart
    }

    private fun row(
        state: RecordState,
        names: Map<Long, String>,
        rates: Map<Currency, BigDecimal>,
        now: Instant,
        p90: Map<Long, Int>,
        graceDays: Map<Long, Int>
    ): RecordRowUi {
        val record = state.record
        val stage = state.displayStage
        val overdue = Pipeline.isOverdue(state, now, p90, graceDays)
        val days = StageResolver.daysInCurrentPhase(state.detail, now)

        // A split record shows the remaining settlement, not the record total.
        val shownMinor = if (state.isSplit) state.owedMinor else record.grossMinor
        val rate = Pipeline.rateFor(record.currency, rates)

        return RecordRowUi(
            id = record.id,
            platformName = names[record.platformId] ?: "Unknown platform",
            stage = stage,
            grossText = MoneyFormat.formatMinor(record.currency, shownMinor),
            kesText = kesText(state, shownMinor, rate, rates),
            ageText = agePillText(
                days = days,
                daysOver = Pipeline.daysOver(state, now, p90, graceDays),
                closed = stage == Stage.REJECTED,
                stalled = stage == Stage.REVERSED
            ),
            overdue = overdue || stage == Stage.REVERSED,
            clearedFraction = if (state.isPartPaid) state.clearedFraction.toFloat() else null,
            chipLabel = chipLabel(state)
        )
    }

    private fun kesText(
        state: RecordState,
        shownMinor: Long,
        rate: BigDecimal,
        rates: Map<Currency, BigDecimal>
    ): String = when (state.displayStage) {
        Stage.REJECTED -> "no payout"
        Stage.REVERSED -> "${MoneyFormat.kes(Money.toKes(Ledger.lostKes(state.detail, rates)))} lost"
        Stage.LANDED -> "${MoneyFormat.kes(Money.toKes(Ledger.finalKesCleared(state.detail)))} cleared"
        else -> MoneyFormat.kes(Money.toKes(Money.fromMinor(shownMinor).multiply(rate)))
    }

    /** `Part paid · 40% left` — a rendering of the settlement set, not of a stage. */
    private fun chipLabel(state: RecordState): String {
        if (!state.isPartPaid) return stageLabel(state.displayStage)
        val left = ((1.0 - state.clearedFraction) * 100).roundToInt()
        return "Part paid · $left% left"
    }
}
