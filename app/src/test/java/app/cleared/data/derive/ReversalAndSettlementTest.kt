package app.cleared.data.derive

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
import app.cleared.data.model.Stage
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/**
 * The two states design/DATA_MODEL.md marks "designed and ready to build": a payout that bounced,
 * and a record that pays in parts.
 */
class ReversalAndSettlementTest {

    private val now = SampleData.NOW
    private val platform = SampleData.platforms.first { it.id == SampleData.HALO }

    // ── Reversal ────────────────────────────────────────────────────────────────────────────────

    /**
     * CLAUDE.md: a REVERSED record contributes its hours to the platform's denominator, contributes
     * zero to the numerator, and is excluded from owedKes.
     */
    @Test
    fun `a reversed record keeps its hours, clears nothing, and is not owed`() {
        val reversed = reversedRecord()
        val state = RecordState.of(reversed)

        assertEquals(Stage.REVERSED, state.displayStage)
        assertFalse("reversed money sits in a wallet, not in flight", state.isOwed)
        assertEquals(0, Ledger.finalKesCleared(reversed).signum())

        val stats = PlatformStatistics.of(platform, listOf(state))
        assertEquals(9.0, stats.hoursTotal, 1e-6)
        assertEquals(0L, stats.totalPaidKes)
        assertEquals(0L, stats.effectiveKesPerHour)
    }

    /** It is not a rejection — the platform approved the work. The counts stay apart. */
    @Test
    fun `a reversal is counted separately from a rejection`() {
        val stats = PlatformStatistics.of(platform, listOf(RecordState.of(reversedRecord())))
        assertEquals(1, stats.reversedCount)
        assertEquals(0, stats.rejectedCount)
        assertEquals(null, stats.approvalRate)
    }

    /**
     * Fees are not refunded, so this is the one record whose net contribution can be negative. The
     * principal is back in the wallet and is not a loss; the fees are, and the row shows them.
     *
     * USD 10.00 commission at 128.40 plus a KES 500 return handling fee — KES 1,784 lost.
     */
    @Test
    fun `a reversed record's fees are kept and shown as a loss`() {
        val reversed = reversedRecord()
        assertEquals(0, Ledger.finalKesCleared(reversed).signum())
        assertEquals(1_784L, Money.toKes(Ledger.lostKes(reversed, SampleData.RATES)))
    }

    /**
     * CLAUDE.md: a re-issue (`supersedesRecordId` set, `carriesHours = false`) adds money once and
     * hours never.
     */
    @Test
    fun `a re-issue adds money once and hours never`() {
        val reversed = reversedRecord()
        val reissue = reissueOf(reversed.record.id)

        val states = listOf(RecordState.of(reversed), RecordState.of(reissue))
        val stats = PlatformStatistics.of(platform, states)

        // Hours are the predecessor's 9.0 and nothing else — the successor carries none.
        assertEquals(9.0, stats.hoursTotal, 1e-6)
        assertEquals(0.0, RecordState.of(reissue).billableHours(), 1e-6)

        // The money lands exactly once, on the successor.
        assertEquals(20_000L, stats.totalPaidKes)
        assertEquals(Money.toKes(BigDecimal("20000")), Money.toKes(Ledger.finalKesCleared(reissue)))

        // 20,000 KES over 9 hours, and the reversal still drags.
        assertEquals(2222L, stats.effectiveKesPerHour)
    }

    // ── Partial payment ─────────────────────────────────────────────────────────────────────────

    /**
     * CLAUDE.md: a part-paid record counts only its unlanded settlements as owed, and its week
     * subtotal matches the remainder rather than the record total.
     */
    @Test
    fun `a part paid record owes only its unlanded remainder`() {
        val record = partPaidRecord()
        val state = RecordState.of(record)

        assertTrue(state.isPartPaid)
        assertEquals(Money.minorOf("400.00"), state.owedMinor)
        assertEquals(0.6, state.clearedFraction, 1e-9)

        val totals = Pipeline.totals(listOf(state), SampleData.RATES, now)
        // USD 400.00 remaining at 128.40 = 51,360 KES. The record total is USD 1,000.00.
        assertEquals(51_360L, totals.owedKes)
        assertEquals(51_360L, totals.weekSubtotalsKes[SampleData.WEEK_2])
    }

    /** Hours are never split — the effective rate is a property of the record. */
    @Test
    fun `hours stay whole across settlements`() {
        val state = RecordState.of(partPaidRecord())
        assertEquals(20.0, state.billableHours(), 1e-6)
        assertEquals(2, state.settlementStates.size)
    }

    /** The numerator takes each settlement's cleared KES as it lands, so the rate moves twice. */
    @Test
    fun `a part paid record contributes only its landed settlement to the effective rate`() {
        val stats = PlatformStatistics.of(platform, listOf(RecordState.of(partPaidRecord())))
        // 60% of USD 1,000.00 landed at 100.00 = 60,000 KES, over the record's whole 20 hours.
        assertEquals(60_000L, stats.totalPaidKes)
        assertEquals(3000L, stats.effectiveKesPerHour)
    }

    @Test
    fun `a fully landed split record is not owed`() {
        val record = partPaidRecord(secondSettlementLanded = true)
        val state = RecordState.of(record)
        assertEquals(Stage.LANDED, state.displayStage)
        assertFalse(state.isPartPaid)
        assertFalse(state.isOwed)
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────────────────────

    private fun reversedRecord(): RecordDetail {
        val id = 7_001L
        val submitted = now.minus(Duration.ofDays(40))
        return RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = platform.id,
                grossMinor = Money.minorOf("200.00"),
                currency = Currency.USD,
                hoursWorked = 9.0,
                hoursUnpaid = 0.0,
                expectedWeekStart = SampleData.WEEK_1,
                createdAt = submitted
            ),
            // Halo Data reports approval directly, so no IN_REVIEW event is ever written. Frame `4a`
            // draws Submitted -> Approved: render whatever events exist, never a placeholder.
            events = listOf(
                ev(id, 1, Stage.SUBMITTED, submitted),
                ev(id, 2, Stage.APPROVED, submitted.plus(Duration.ofDays(4))),
                ev(id, 3, Stage.PAYOUT_ISSUED, submitted.plus(Duration.ofDays(9))),
                ev(id, 4, Stage.RECEIVED, submitted.plus(Duration.ofDays(12))),
                ev(id, 5, Stage.REVERSED, submitted.plus(Duration.ofDays(23)), "Name mismatch at the bank")
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

    private fun reissueOf(predecessorId: Long): RecordDetail {
        val id = 7_002L
        val issued = now.minus(Duration.ofDays(10))
        return RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = platform.id,
                grossMinor = Money.minorOf("200.00"),
                currency = Currency.USD,
                hoursWorked = 0.0,
                hoursUnpaid = 0.0,
                expectedWeekStart = SampleData.WEEK_1,
                createdAt = issued,
                supersedesRecordId = predecessorId,
                carriesHours = false
            ),
            events = listOf(
                ev(id, 1, Stage.PAYOUT_ISSUED, issued),
                ev(id, 2, Stage.RECEIVED, issued.plus(Duration.ofDays(2))),
                ev(id, 3, Stage.LANDED, issued.plus(Duration.ofDays(4)))
            ),
            conversions = listOf(
                ConversionSnapshotEntity(id, id, fromCurrency = Currency.USD, rateApplied = BigDecimal("100.00"), midRate = BigDecimal("100.00"), appliedAt = issued)
            )
        )
    }

    /** Lumen pays 60 / 40: the first settlement has landed, the second has not. */
    private fun partPaidRecord(secondSettlementLanded: Boolean = false): RecordDetail {
        val id = 7_003L
        val submitted = now.minus(Duration.ofDays(30))
        val first = 8_001L
        val second = 8_002L
        return RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = platform.id,
                grossMinor = Money.minorOf("1000.00"),
                currency = Currency.USD,
                hoursWorked = 20.0,
                hoursUnpaid = 0.0,
                expectedWeekStart = SampleData.WEEK_1,
                createdAt = submitted
            ),
            events = buildList {
                add(ev(id, 1, Stage.SUBMITTED, submitted))
                add(ev(id, 2, Stage.APPROVED, submitted.plus(Duration.ofDays(6))))
                add(ev(id, 3, Stage.PAYOUT_ISSUED, submitted.plus(Duration.ofDays(8)), settlementId = first))
                add(ev(id, 4, Stage.LANDED, submitted.plus(Duration.ofDays(12)), settlementId = first))
                add(ev(id, 5, Stage.PAYOUT_ISSUED, submitted.plus(Duration.ofDays(26)), settlementId = second))
                if (secondSettlementLanded) {
                    add(ev(id, 6, Stage.LANDED, submitted.plus(Duration.ofDays(29)), settlementId = second))
                }
            },
            settlements = listOf(
                SettlementEntity(first, id, "60% on approval", 0.6, Money.minorOf("600.00"), 1, SampleData.WEEK_1),
                SettlementEntity(second, id, "40% on publication", 0.4, Money.minorOf("400.00"), 2, SampleData.WEEK_2)
            ),
            conversions = listOf(
                ConversionSnapshotEntity(1, id, settlementId = first, fromCurrency = Currency.USD, rateApplied = BigDecimal("100.00"), midRate = BigDecimal("100.00"), appliedAt = submitted),
                ConversionSnapshotEntity(2, id, settlementId = second, fromCurrency = Currency.USD, rateApplied = BigDecimal("100.00"), midRate = BigDecimal("100.00"), appliedAt = submitted)
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
