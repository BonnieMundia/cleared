package app.cleared.ui.record

import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.FeeLineEntity
import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.db.entity.SettlementEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.model.Currency
import app.cleared.data.model.EventSource
import app.cleared.data.model.FeeKind
import app.cleared.data.model.Money
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/**
 * Frames `1e`, `4a` and `4b`. One screen, three shapes, decided by the record rather than the
 * caller.
 */
class RecordDetailMapperTest {

    private val lumen = SampleData.platforms.first { it.id == SampleData.LUMEN }
    private val halo = SampleData.platforms.first { it.id == SampleData.HALO }

    // ── `1e` a landed record, end to end ────────────────────────────────────────────────────────

    private val landed = RecordDetailMapper.build(
        detail = SampleData.landedRecordDetailExample,
        platform = lumen,
        rates = SampleData.RATES,
        now = SampleData.NOW
    )

    @Test
    fun `the header carries the cleared figure in green`() {
        assertEquals("Lumen Writers", landed.platformName)
        assertEquals("Landed", landed.chipLabel)
        assertEquals("KES 88,220", landed.heroFigure)
        assertEquals(HeroTone.Cleared, landed.heroTone)
        assertEquals("Long-form writing · 3 briefs · ref LW-2264", landed.subLine)
    }

    @Test
    fun `the header caption names where and when it cleared`() {
        assertEquals("Cleared to Equity Bank · 4 Jul 2026", landed.heroCaption)
    }

    /** `Gross EUR 640.00` · `Hours logged 18.5 h` · `End to end 22 d`. */
    @Test
    fun `the stat strip is gross, hours and end to end`() {
        assertEquals(listOf("Gross", "Hours logged", "End to end"), landed.stats.map { it.label })
        assertEquals("EUR 640.00", landed.stats[0].value)
        assertEquals("18.5 h", landed.stats[1].value)
        assertEquals("22 d", landed.stats[2].value)
    }

    /**
     * The ledger, exactly as design/SCREENS.md prints it: gross, the two EUR fees, the conversion
     * with its mid and spread, the KES fee, then what cleared.
     */
    @Test
    fun `the money ledger reads as the arithmetic it is`() {
        val rows = landed.ledger
        assertEquals("Gross", rows[0].label)
        assertEquals("EUR 640.00", rows[0].value)

        assertEquals("Platform commission 5%", rows[1].label)
        assertEquals("−EUR 32.00", rows[1].value)

        assertEquals("Payoneer withdrawal fee", rows[2].label)
        assertEquals("−EUR 1.50", rows[2].value)

        assertEquals("Converted 606.50 at", rows[3].label)
        assertEquals("145.82", rows[3].value)
        assertEquals("mid was 147.60 · 1.2% spread", rows[3].subLabel)

        assertEquals("Bank credit fee", rows[4].label)
        assertEquals("−KES 220", rows[4].value)

        assertEquals("Cleared", rows[5].label)
        assertEquals("KES 88,220", rows[5].value)
        assertTrue(rows[5].isTotal)
    }

    @Test
    fun `the closing note states what getting paid cost`() {
        assertEquals(
            "Getting this paid cost KES 6,244 — 6.6% of the mid-market value. You kept 93.4%.",
            landed.closingNote
        )
    }

    /** Six entries across two phases: 18 days of work, 4 days of money. */
    @Test
    fun `the timeline splits into a work phase and a money phase`() {
        assertEquals(2, landed.phases.size)
        assertEquals(Phase.WORK, landed.phases[0].phase)
        assertEquals(18L, landed.phases[0].durationDays)
        assertEquals(3, landed.phases[0].entries.size)

        assertEquals(Phase.MONEY, landed.phases[1].phase)
        assertEquals(4L, landed.phases[1].durationDays)
        assertEquals(3, landed.phases[1].entries.size)
        assertTrue(landed.phases[1].entries.last().isLast)
    }

    /** `+7 d` between Approved and the In review before it. */
    @Test
    fun `timeline entries carry the gap since the previous one`() {
        val work = landed.phases[0].entries
        assertEquals(0L, work[0].deltaDays)
        assertEquals(0L, work[1].deltaDays)
        assertEquals(7L, work[2].deltaDays)
        assertEquals(11L, landed.phases[1].entries[0].deltaDays)
    }

    // ── `4a` a payout that bounced ──────────────────────────────────────────────────────────────

    private val reversedUi = RecordDetailMapper.build(
        detail = reversedRecord(),
        platform = halo,
        rates = SampleData.RATES,
        successor = reissue(),
        now = SampleData.NOW
    )

    /** The money exists — it is in the wrong place. That is not the same as never having existed. */
    @Test
    fun `a reversed record's hero is neutral, not red`() {
        assertEquals("Reversed", reversedUi.chipLabel)
        assertEquals(HeroTone.Neutral, reversedUi.heroTone)
        // USD 200.00 less the USD 10.00 commission, converted at 100.00, less the KES 500 the bank
        // charged to hand it back.
        assertEquals("KES 18,500", reversedUi.heroFigure)
        assertTrue(reversedUi.isReversed)
    }

    /** The one red figure on the screen. */
    @Test
    fun `the third stat cell is a red zero`() {
        val cleared = reversedUi.stats[2]
        assertEquals("Cleared", cleared.label)
        assertEquals("KES 0", cleared.value)
        assertTrue(cleared.isNegative)
    }

    @Test
    fun `reversed fees are labelled not refunded and the footer changes`() {
        val commission = reversedUi.ledger.first { it.label.startsWith("Platform commission") }
        assertEquals("not refunded", commission.subLabel)

        val footer = reversedUi.ledger.last()
        assertEquals("Back in the wallet", footer.label)
        assertTrue(footer.isTotal)
    }

    /**
     * Halo Data reports approval directly, so no IN_REVIEW event is ever written and the timeline
     * draws Submitted → Approved. Render whatever events exist, never a placeholder.
     */
    @Test
    fun `a skipped stage leaves no placeholder in the timeline`() {
        val work = reversedUi.phases.first { it.phase == Phase.WORK }
        assertEquals(listOf(Stage.SUBMITTED, Stage.APPROVED), work.entries.map { it.stage })
    }

    /**
     * The chain breaks where the money was, not in a phase of its own.
     *
     * Frame `4a` runs the money phase green through `Received` and *then* breaks it: the dashed
     * connector and the hollow ring belong inside that block. Grouping strictly by phase put the
     * reversal in a third `ENDED` section and left `Received` drawing no connector at all, so the
     * break was invisible.
     */
    @Test
    fun `the reversal ends the money phase rather than opening one of its own`() {
        assertEquals(2, reversedUi.phases.size)

        val money = reversedUi.phases[1]
        assertEquals(Phase.MONEY, money.phase)
        assertEquals(
            listOf(Stage.PAYOUT_ISSUED, Stage.RECEIVED, Stage.REVERSED),
            money.entries.map { it.stage }
        )
        assertTrue("the reversal is the record's last event", money.entries.last().isLast)
    }

    /** A rejection ends the work phase the same way — it never enters the money phase at all. */
    @Test
    fun `a rejection ends the work phase rather than opening one of its own`() {
        val ui = RecordDetailMapper.build(
            detail = SampleData.pipelineById.getValue(8L),
            platform = halo,
            rates = SampleData.RATES,
            now = SampleData.NOW
        )
        assertEquals(1, ui.phases.size)
        assertEquals(Phase.WORK, ui.phases[0].phase)
        assertEquals(Stage.REJECTED, ui.phases[0].entries.last().stage)
    }

    /**
     * A payout can bounce before it was ever converted, and the money then sits in a wallet in its
     * original currency. Valuing it needs today's mid rather than a snapshot that does not exist —
     * without the fallback the hero read `KES 0` for a wallet holding USD 190.
     */
    @Test
    fun `a reversal with no conversion snapshot is valued at the current mid`() {
        val withoutSnapshot = reversedRecord().copy(conversions = emptyList())
        val ui = RecordDetailMapper.build(withoutSnapshot, halo, SampleData.RATES, now = SampleData.NOW)
        // USD 200.00 less USD 10.00 commission at 128.40, less the KES 500 handling fee.
        assertEquals("KES 23,896", ui.heroFigure)
    }

    /** Green is for money that cleared. A reversal's total is real but is not a good outcome. */
    @Test
    fun `the reversed total is not rendered as cleared`() {
        assertFalse(reversedUi.ledger.last().totalCleared)
        assertTrue(landed.ledger.last().totalCleared)
    }

    @Test
    fun `the reversal reason is carried through from the event log`() {
        assertEquals("Name mismatch at the bank", reversedUi.reversalReason)
    }

    /** Recovery is a new record linked back, never a return to an earlier stage. */
    @Test
    fun `the re-issue card carries money and no hours`() {
        val reissue = reversedUi.reissue!!
        assertEquals("HD-2291", reissue.reference)
        assertEquals("USD 200.00", reissue.amount)
        assertEquals("0 h", reissue.hours)
    }

    @Test
    fun `a reversed record with no re-issue yet has no card`() {
        val ui = RecordDetailMapper.build(reversedRecord(), halo, SampleData.RATES, now = SampleData.NOW)
        assertNull(ui.reissue)
    }

    // ── `4b` a record paid in parts ─────────────────────────────────────────────────────────────

    private val partPaid = RecordDetailMapper.build(
        detail = partPaidRecord(),
        platform = lumen,
        rates = SampleData.RATES,
        p90Days = 11,
        now = SampleData.NOW
    )

    @Test
    fun `a part paid record's hero is the whole and the bar says how much landed`() {
        assertEquals("Part paid", partPaid.chipLabel)
        assertTrue(partPaid.isPartPaid)
        assertEquals(HeroTone.Neutral, partPaid.heroTone)
        assertEquals(0.6f, partPaid.clearedFraction, 1e-6f)
        assertEquals("KES 60,000 cleared", partPaid.clearedText)
        assertEquals("KES 59,040 in flight", partPaid.inFlightText)
    }

    /** The rate belongs here and nowhere else on this screen. */
    @Test
    fun `the third stat cell is the effective rate`() {
        val effective = partPaid.stats[2]
        assertEquals("Effective", effective.label)
        // 60,000 KES landed over the record's whole 20 hours.
        assertEquals("KES 3,000/h", effective.value)
    }

    @Test
    fun `settlement cards carry their own timing`() {
        assertEquals(2, partPaid.settlements.size)
        val (first, second) = partPaid.settlements

        assertTrue(first.isLanded)
        assertEquals("60% on approval", first.label)
        assertTrue(first.timing, first.timing.contains("landed"))
        assertTrue(first.timing, first.timing.endsWith("4 d"))

        assertFalse(second.isLanded)
        assertTrue(second.timing, second.timing.contains("expected"))
        assertTrue(second.timing, second.timing.endsWith("p90 11 d"))
    }

    @Test
    fun `the settlement terms and footer come from the platform`() {
        assertEquals("Lumen pays 60 / 40", partPaid.settlementTerms)
        assertEquals(
            "Only the second settlement counts as owed. The first has left the pipeline.",
            partPaid.settlementFooter
        )
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────────────────────

    private fun reversedRecord(): RecordDetail {
        val id = 5_001L
        val submitted = SampleData.NOW.minus(Duration.ofDays(40))
        return RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = halo.id,
                grossMinor = Money.minorOf("200.00"),
                currency = Currency.USD,
                hoursWorked = 9.0,
                hoursUnpaid = 0.0,
                expectedWeekStart = SampleData.WEEK_1,
                createdAt = submitted
            ),
            events = listOf(
                ev(id, 1, Stage.SUBMITTED, submitted),
                ev(id, 2, Stage.APPROVED, submitted.plus(Duration.ofDays(4))),
                ev(id, 3, Stage.PAYOUT_ISSUED, submitted.plus(Duration.ofDays(9))),
                ev(id, 4, Stage.RECEIVED, submitted.plus(Duration.ofDays(12)), note = "Payoneer"),
                ev(id, 5, Stage.REVERSED, submitted.plus(Duration.ofDays(23)), note = "Name mismatch at the bank")
            ),
            fees = listOf(
                FeeLineEntity(1, id, kind = FeeKind.PLATFORM_COMMISSION, label = "Platform commission 5%", amountMinor = Money.minorOf("10.00"), currency = Currency.USD, occurredAt = submitted),
                FeeLineEntity(2, id, kind = FeeKind.RETURN_HANDLING_FEE, label = "Return handling fee", amountMinor = Money.minorOf("500"), currency = Currency.KES, occurredAt = submitted)
            ),
            conversions = listOf(
                ConversionSnapshotEntity(id, id, fromCurrency = Currency.USD, rateApplied = BigDecimal("100.00"), midRate = BigDecimal("100.00"), appliedAt = submitted)
            )
        )
    }

    private fun reissue(): RecordDetail {
        val id = 5_002L
        val issued = SampleData.NOW.minus(Duration.ofDays(6))
        return RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = halo.id,
                grossMinor = Money.minorOf("200.00"),
                currency = Currency.USD,
                hoursWorked = 0.0,
                hoursUnpaid = 0.0,
                externalRef = "HD-2291",
                expectedWeekStart = SampleData.WEEK_1,
                createdAt = issued,
                supersedesRecordId = 5_001L,
                carriesHours = false
            ),
            events = listOf(ev(id, 1, Stage.PAYOUT_ISSUED, issued))
        )
    }

    private fun partPaidRecord(): RecordDetail {
        val id = 5_003L
        val submitted = SampleData.NOW.minus(Duration.ofDays(30))
        val first = 6_001L
        val second = 6_002L
        return RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = lumen.id,
                grossMinor = Money.minorOf("1000.00"),
                currency = Currency.EUR,
                hoursWorked = 20.0,
                hoursUnpaid = 0.0,
                expectedWeekStart = SampleData.WEEK_1,
                createdAt = submitted
            ),
            events = listOf(
                ev(id, 1, Stage.SUBMITTED, submitted),
                ev(id, 2, Stage.APPROVED, submitted.plus(Duration.ofDays(6))),
                ev(id, 3, Stage.PAYOUT_ISSUED, submitted.plus(Duration.ofDays(8)), settlementId = first),
                ev(id, 4, Stage.LANDED, submitted.plus(Duration.ofDays(12)), settlementId = first),
                ev(id, 5, Stage.PAYOUT_ISSUED, submitted.plus(Duration.ofDays(26)), settlementId = second)
            ),
            settlements = listOf(
                SettlementEntity(first, id, "60% on approval", 0.6, Money.minorOf("600.00"), 1, SampleData.WEEK_1),
                SettlementEntity(second, id, "40% on publication", 0.4, Money.minorOf("400.00"), 2, SampleData.WEEK_2)
            ),
            conversions = listOf(
                ConversionSnapshotEntity(1, id, settlementId = first, fromCurrency = Currency.EUR, rateApplied = BigDecimal("100.00"), midRate = BigDecimal("100.00"), appliedAt = submitted)
            )
        )
    }

    private fun ev(
        recordId: Long,
        ordinal: Int,
        stage: Stage,
        at: Instant,
        note: String? = null,
        settlementId: Long? = null
    ) = StageEventEntity(
        id = recordId * 100 + ordinal,
        recordId = recordId,
        settlementId = settlementId,
        stage = stage,
        occurredAt = at,
        source = EventSource.MANUAL,
        idempotencyKey = "test:$recordId:$ordinal:${stage.name}",
        note = note
    )
}
