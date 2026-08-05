package app.cleared.ui.record

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.derive.Ledger
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.SettlementState
import app.cleared.data.derive.Timeline
import app.cleared.data.derive.TimelinePhase
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import app.cleared.data.model.Stage
import app.cleared.ui.components.stageLabel
import app.cleared.ui.format.DateFormat
import app.cleared.ui.format.MoneyFormat
import java.math.BigDecimal
import java.math.MathContext
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/** How the hero figure is coloured — the money's condition, not its size. */
enum class HeroTone {
    /** Landed. `onMoneyContainer`. */
    Cleared,

    /** Reversed or part paid: the money exists, so `onSurface` rather than the rejected colour. */
    Neutral,

    /** Nothing cleared. */
    None
}

data class StatCell(val label: String, val value: String, val isNegative: Boolean = false)

/** A row of the "What happened to the money" table. */
data class LedgerRow(
    val label: String,
    val value: String,
    val subLabel: String? = null,
    val isTotal: Boolean = false,
    val isNegative: Boolean = false,
    /**
     * Green is reserved for money that actually cleared. A reversal's total is the sum sitting back
     * in a wallet — real, but not an outcome to congratulate — so it renders neutral.
     */
    val totalCleared: Boolean = false
)

data class SettlementCardUi(
    val id: Long,
    val label: String,
    val stage: Stage,
    val timing: String,
    val amount: String,
    val kes: String,
    val isLanded: Boolean
)

data class ReissueCardUi(
    val recordId: Long,
    val reference: String,
    val stage: Stage,
    val amount: String,
    val hours: String
)

data class RecordDetailUi(
    val recordId: Long,
    val platformName: String,
    val stage: Stage,
    val chipLabel: String,
    val subLine: String?,
    val heroFigure: String,
    val heroTone: HeroTone,
    val heroCaption: String?,
    val stats: List<StatCell>,
    val phases: List<TimelinePhase>,
    val ledger: List<LedgerRow>,
    val closingNote: String?,
    val isReversed: Boolean = false,
    val reversalReason: String? = null,
    val reissue: ReissueCardUi? = null,
    val isPartPaid: Boolean = false,
    val clearedFraction: Float = 0f,
    val clearedText: String? = null,
    val inFlightText: String? = null,
    val settlements: List<SettlementCardUi> = emptyList(),
    val settlementTerms: String? = null,
    val settlementFooter: String? = null,
    val loading: Boolean = false
)

/**
 * Builds frame `1e` and its two variants — `4a` reversed and `4b` part paid — from one record.
 *
 * The three are the same screen. What differs is which figures exist: a reversal has money that
 * arrived and left, so its hero is neutral and its cleared cell is a red zero; a split record has
 * money in two places at once, so its hero is the whole and the split bar says how much of it has
 * actually landed.
 */
object RecordDetailMapper {

    fun build(
        detail: RecordDetail,
        platform: PlatformEntity?,
        rates: Map<Currency, BigDecimal>,
        successor: RecordDetail? = null,
        p90Days: Int? = null,
        now: Instant = Instant.now()
    ): RecordDetailUi {
        val state = RecordState.of(detail)
        val record = detail.record
        val currency = record.currency
        val phases = Timeline.of(detail)
        val reversed = state.displayStage == Stage.REVERSED

        return RecordDetailUi(
            recordId = record.id,
            platformName = platform?.name ?: "Unknown platform",
            stage = state.displayStage,
            chipLabel = if (state.isPartPaid) "Part paid" else stageLabel(state.displayStage),
            subLine = subLine(detail),
            heroFigure = hero(detail, state, reversed, rates),
            heroTone = when {
                reversed || state.isPartPaid -> HeroTone.Neutral
                state.displayStage == Stage.LANDED -> HeroTone.Cleared
                else -> HeroTone.None
            },
            heroCaption = heroCaption(detail, state, reversed),
            stats = stats(detail, state, platform, rates),
            phases = phases,
            ledger = ledger(detail, state, reversed, rates),
            closingNote = closingNote(detail, state, reversed),
            isReversed = reversed,
            reversalReason = if (reversed) reversalReason(detail) else null,
            reissue = successor?.let { reissueCard(it, rates) },
            isPartPaid = state.isPartPaid,
            clearedFraction = state.clearedFraction.toFloat(),
            clearedText = if (state.isSplit) clearedText(detail) else null,
            inFlightText = if (state.isSplit) inFlightText(state, rates) else null,
            settlements = settlementCards(state, rates, p90Days),
            settlementTerms = settlementTerms(state, platform),
            settlementFooter = if (state.isPartPaid) {
                "Only the second settlement counts as owed. The first has left the pipeline."
            } else null
        )
    }

    private fun subLine(detail: RecordDetail): String? {
        val parts = listOfNotNull(
            detail.record.description,
            detail.record.externalRef?.let { "ref $it" }
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun hero(
        detail: RecordDetail,
        state: RecordState,
        reversed: Boolean,
        rates: Map<Currency, BigDecimal>
    ): String = when {
        // The money exists; it is sitting in the wrong place. Frame `4a` renders this in onSurface.
        reversed -> MoneyFormat.kes(Money.toKes(Ledger.arrivedKes(detail, rates)))
        // A split record's hero is the record's whole value, not the part that has landed.
        state.isSplit -> MoneyFormat.kes(Money.toKes(Ledger.arrivedKes(detail, rates)))
        state.displayStage == Stage.LANDED -> MoneyFormat.kes(Money.toKes(Ledger.finalKesCleared(detail)))
        else -> MoneyFormat.formatMinor(detail.record.currency, detail.record.grossMinor)
    }

    private fun heroCaption(detail: RecordDetail, state: RecordState, reversed: Boolean): String? {
        if (reversed) {
            val received = detail.events.firstOrNull { it.stage == Stage.RECEIVED }
            val where = received?.note ?: "the wallet"
            return "Sitting in $where since ${received?.let { DateFormat.shortDate(it.occurredAt) } ?: "arrival"} · " +
                "never reached the bank"
        }
        if (state.displayStage == Stage.LANDED) {
            val landed = detail.events.filter { it.stage == Stage.LANDED }.maxByOrNull { it.occurredAt }
            val where = landed?.note ?: "your account"
            return landed?.let { "Cleared to $where · ${DateFormat.date(it.occurredAt)}" }
        }
        return null
    }

    private fun stats(
        detail: RecordDetail,
        state: RecordState,
        platform: PlatformEntity?,
        rates: Map<Currency, BigDecimal>
    ): List<StatCell> {
        val record = detail.record
        val gross = StatCell("Gross", MoneyFormat.formatMinor(record.currency, record.grossMinor))
        val hours = StatCell("Hours logged", MoneyFormat.hours(record.hoursWorked + record.hoursUnpaid))

        return when {
            // A reversal's third cell is the one red figure on the screen.
            state.displayStage == Stage.REVERSED ->
                listOf(gross, hours, StatCell("Cleared", "KES 0", isNegative = true))

            // On a split record the effective rate belongs here and nowhere else.
            state.isSplit -> listOf(gross, hours, StatCell("Effective", effectiveRate(detail, state)))

            else -> {
                val endToEnd = Timeline.endToEndDays(detail)
                listOf(gross, hours, StatCell("End to end", endToEnd?.let { DateFormat.days(it) } ?: "—"))
            }
        }
    }

    /** Cleared KES over the record's whole hour total — never per settlement. */
    private fun effectiveRate(detail: RecordDetail, state: RecordState): String {
        val hours = detail.record.hoursWorked + detail.record.hoursUnpaid
        if (hours == 0.0) return "—"
        val cleared = Ledger.finalKesCleared(detail)
        val rate = cleared.divide(BigDecimal.valueOf(hours), MathContext.DECIMAL64)
        return "${MoneyFormat.kes(Money.toKes(rate))}/h"
    }

    /**
     * The money ledger, read as arithmetic rather than stored.
     *
     * `4a` differs in two places: the fees carry a `not refunded` sub-label, because a reversal
     * returns the principal and keeps them, and the footer reads `Back in the wallet` rather than
     * `Cleared`.
     */
    private fun ledger(
        detail: RecordDetail,
        state: RecordState,
        reversed: Boolean,
        rates: Map<Currency, BigDecimal>
    ): List<LedgerRow> {
        val record = detail.record
        val rows = mutableListOf<LedgerRow>()

        rows += LedgerRow("Gross", MoneyFormat.formatMinor(record.currency, record.grossMinor))

        detail.fees.filter { it.currency == record.currency }.forEach { fee ->
            rows += LedgerRow(
                label = fee.label,
                value = MoneyFormat.MINUS + MoneyFormat.formatMinor(fee.currency, fee.amountMinor),
                subLabel = if (reversed && !fee.refundable) "not refunded" else null,
                isNegative = true
            )
        }

        detail.conversions.firstOrNull { it.settlementId == null }?.let { snapshot ->
            val converted = Ledger.convertedAmount(detail)
            val spread = ((snapshot.midRate - snapshot.rateApplied)
                .divide(snapshot.midRate, MathContext.DECIMAL64))
                .multiply(BigDecimal(100))
            rows += LedgerRow(
                label = "Converted ${MoneyFormat.digits(record.currency, converted)} at",
                value = MoneyFormat.rate(snapshot.rateApplied),
                subLabel = "mid was ${MoneyFormat.rate(snapshot.midRate)} · " +
                    "${MoneyFormat.percent(spread.toDouble())} spread"
            )
        }

        detail.fees.filter { it.currency == Currency.KES }.forEach { fee ->
            rows += LedgerRow(
                label = fee.label,
                value = MoneyFormat.MINUS + MoneyFormat.formatMinor(fee.currency, fee.amountMinor),
                subLabel = if (reversed && !fee.refundable) "not refunded" else null,
                isNegative = true
            )
        }

        rows += if (reversed) {
            LedgerRow(
                "Back in the wallet",
                MoneyFormat.kes(Money.toKes(Ledger.arrivedKes(detail, rates))),
                isTotal = true
            )
        } else {
            LedgerRow(
                "Cleared",
                MoneyFormat.kes(Money.toKes(Ledger.finalKesCleared(detail))),
                isTotal = true,
                totalCleared = true
            )
        }

        return rows
    }

    private fun closingNote(detail: RecordDetail, state: RecordState, reversed: Boolean): String? {
        if (reversed) {
            return "The fees are not refunded, and the hours stay against this platform. " +
                "A re-issue carries the money; it carries none of the work."
        }
        if (state.displayStage != Stage.LANDED) return null
        val cost = Ledger.totalCostKes(detail)
        val mid = Ledger.midMarketKes(detail)
        if (mid.signum() == 0) return null
        val pct = cost.divide(mid, MathContext.DECIMAL64).multiply(BigDecimal(100)).toDouble()
        return "Getting this paid cost ${MoneyFormat.kes(Money.toKes(cost))} — " +
            "${MoneyFormat.percent(pct)} of the mid-market value. " +
            "You kept ${MoneyFormat.percent(100.0 - pct)}."
    }

    private fun reversalReason(detail: RecordDetail): String? =
        detail.events.firstOrNull { it.stage == Stage.REVERSED }?.note

    private fun reissueCard(successor: RecordDetail, rates: Map<Currency, BigDecimal>): ReissueCardUi {
        val state = RecordState.of(successor)
        return ReissueCardUi(
            recordId = successor.record.id,
            reference = successor.record.externalRef ?: "#${successor.record.id}",
            stage = state.displayStage,
            amount = MoneyFormat.formatMinor(successor.record.currency, successor.record.grossMinor),
            // Always zero: the work was already counted on the predecessor.
            hours = MoneyFormat.hours(successor.record.hoursWorked + successor.record.hoursUnpaid)
        )
    }

    private fun clearedText(detail: RecordDetail): String =
        "${MoneyFormat.kes(Money.toKes(Ledger.finalKesCleared(detail)))} cleared"

    private fun inFlightText(state: RecordState, rates: Map<Currency, BigDecimal>): String {
        val rate = app.cleared.data.derive.Pipeline.rateFor(state.record.currency, rates)
        val kes = Money.fromMinor(state.owedMinor).multiply(rate)
        return "${MoneyFormat.kes(Money.toKes(kes))} in flight"
    }

    private fun settlementTerms(state: RecordState, platform: PlatformEntity?): String? {
        if (!state.isSplit) return null
        val name = platform?.name?.substringBefore(' ') ?: "This platform"
        val split = state.settlementStates.joinToString(" / ") {
            (it.settlement.fraction * 100).roundToInt().toString()
        }
        return "$name pays $split"
    }

    private fun settlementCards(
        state: RecordState,
        rates: Map<Currency, BigDecimal>,
        p90Days: Int?
    ): List<SettlementCardUi> {
        if (!state.isSplit) return emptyList()
        val rate = app.cleared.data.derive.Pipeline.rateFor(state.record.currency, rates)
        return state.settlementStates.map { settlement ->
            SettlementCardUi(
                id = settlement.settlement.id,
                label = settlement.settlement.label,
                stage = settlement.stage,
                timing = timing(state, settlement, p90Days),
                amount = MoneyFormat.formatMinor(state.record.currency, settlement.settlement.amountMinor),
                kes = MoneyFormat.kes(
                    Money.toKes(Money.fromMinor(settlement.settlement.amountMinor).multiply(rate))
                ),
                isLanded = settlement.isLanded
            )
        }
    }

    /**
     * `Issued 18 Jul → landed 22 Jul · 4 d` once it has landed, and
     * `Issued 2 Aug → expected 8 Aug · p90 11 d` while it is still in flight.
     */
    private fun timing(state: RecordState, settlement: SettlementState, p90Days: Int?): String {
        val events = state.detail.events.filter { it.settlementId == settlement.settlement.id }
        val issued = events.firstOrNull { it.stage == Stage.PAYOUT_ISSUED }?.occurredAt
        val landed = events.firstOrNull { it.stage == Stage.LANDED }?.occurredAt

        if (issued == null) return settlement.settlement.label
        val issuedText = "Issued ${DateFormat.shortDate(issued)}"

        return if (landed != null) {
            val days = Duration.between(issued, landed).toDays()
            "$issuedText → landed ${DateFormat.shortDate(landed)} · $days d"
        } else if (p90Days != null) {
            val expected = issued.plus(Duration.ofDays(p90Days.toLong()))
            "$issuedText → expected ${DateFormat.shortDate(expected)} · p90 $p90Days d"
        } else {
            "$issuedText → in flight"
        }
    }
}
