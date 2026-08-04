package app.cleared.data.derive

import app.cleared.data.model.Currency
import app.cleared.data.model.Stage
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * The five figures design/PROMPTS.md says to be picky about, plus the acceptance checks in
 * design/README.md that depend on them.
 */
class PipelineTest {

    private fun totals(states: List<RecordState> = SampleData.states) =
        Pipeline.totals(states, SampleData.RATES, SampleData.NOW)

    /** derivedPipeline.owedKes */
    @Test
    fun `owed is 247119 KES`() {
        assertEquals(247_119L, totals().owedKes)
    }

    /** derivedPipeline.owedUsd / owedEur */
    @Test
    fun `owed components are USD 947-50 and EUR 850-00`() {
        val byCurrency = totals().owedByCurrency
        assertEquals(BigDecimal("947.50"), byCurrency[Currency.USD])
        assertEquals(BigDecimal("850.00"), byCurrency[Currency.EUR])
    }

    /** derivedPipeline.workPhaseKes / moneyPhaseKes */
    @Test
    fun `phase split is 188183 work and 58936 money`() {
        val t = totals()
        assertEquals(188_183L, t.workKes)
        assertEquals(58_936L, t.moneyKes)
        assertEquals(t.owedKes, t.workKes + t.moneyKes)
    }

    /** derivedPipeline.weekSubtotalsKes */
    @Test
    fun `week subtotals are 64393, 151730 and 30996`() {
        val weeks = totals().weekSubtotalsKes
        assertEquals(64_393L, weeks[SampleData.WEEK_1])
        assertEquals(151_730L, weeks[SampleData.WEEK_2])
        assertEquals(30_996L, weeks[SampleData.WEEK_3])
    }

    /** derivedPipeline.openCount — the rejected record is not open. */
    @Test
    fun `seven records are open`() {
        assertEquals(7, totals().openCount)
    }

    /**
     * README.md acceptance check: advancing a record to LANDED removes it from the owed hero figure
     * and from its week subtotal, with no manual refresh.
     */
    @Test
    fun `landing a record removes it from owed and from its week subtotal`() {
        val landed = advanceTo(1L, Stage.LANDED)
        val after = totals(landed)

        // Record 1 is Halo Data USD 184.00 in week 1, worth 184.00 x 128.40 = 23,625.60.
        assertEquals(247_119L - 23_626L, after.owedKes)
        assertEquals(64_393L - 23_626L, after.weekSubtotalsKes[SampleData.WEEK_1])
        assertEquals(6, after.openCount)
    }

    /** It was money-phase, so only the money side of the bar should move. */
    @Test
    fun `landing a record moves only its own phase`() {
        val after = totals(advanceTo(1L, Stage.LANDED))
        assertEquals(188_183L, after.workKes)
        assertEquals(58_936L - 23_626L, after.moneyKes)
    }

    /** Crossing the phase boundary moves value from violet to green without changing the total. */
    @Test
    fun `approving to payout issued moves value from work to money`() {
        val after = totals(advanceTo(4L, Stage.PAYOUT_ISSUED))

        // Record 4 is Kibo Studio USD 350.00 = 44,940.00 KES.
        assertEquals(247_119L, after.owedKes)
        assertEquals(188_183L - 44_940L, after.workKes)
        assertEquals(58_936L + 44_940L, after.moneyKes)
    }

    @Test
    fun `rejected and landed records do not advance`() {
        assertFalse(Stage.REJECTED.isAdvanceable)
        assertFalse(Stage.REVERSED.isAdvanceable)
        assertFalse(Stage.LANDED.isAdvanceable)
        assertTrue(Stage.RECEIVED.isAdvanceable)
        assertEquals(null, Stage.REJECTED.next())
        assertEquals(null, Stage.LANDED.next())
    }

    /** A rejected record contributes nothing to owed. */
    @Test
    fun `the rejected record is not owed`() {
        assertFalse(SampleData.stateOf(8L).isOwed)
    }

    private fun advanceTo(recordId: Long, stage: Stage): List<RecordState> =
        SampleData.all.map { detail ->
            if (detail.record.id != recordId) RecordState.of(detail)
            else RecordState.of(
                detail.copy(
                    events = detail.events + app.cleared.data.db.entity.StageEventEntity(
                        id = 99_000 + recordId,
                        recordId = recordId,
                        stage = stage,
                        occurredAt = SampleData.NOW,
                        source = app.cleared.data.model.EventSource.MANUAL,
                        idempotencyKey = "test:$recordId:${stage.name}"
                    )
                )
            )
        }
}
