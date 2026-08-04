package app.cleared.data.derive

import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Overdue fires on the platform's own p90, recomputed when a record lands — not a global constant
 * and not a mean.
 */
class SettleTimeTest {

    private val now = SampleData.NOW
    private val states = SampleData.states

    /** `platforms[2].stats`: p50 11 d, p90 19 d, mean 13.4 d. */
    @Test
    fun `Halo Data settles at p50 11 days and p90 19 days with a mean of 13-4`() {
        val stats = SettleTime.of(SampleData.HALO, states, now)
        assertEquals(11, stats.p50Days)
        assertEquals(19, stats.p90Days)
        assertEquals(13.4, stats.meanDays!!, 1e-9)
    }

    /**
     * The reason the rule is p90. On Halo Data's distribution the mean sits at 13.4 d: flagging on
     * it would nag about records at 14 d, which are normal, and stay silent about the 45 d one.
     */
    @Test
    fun `flagging on the mean would be wrong in both directions`() {
        val stats = SettleTime.of(SampleData.HALO, states, now)
        val mean = stats.meanDays!!
        val p90 = stats.p90Days!!

        assertTrue("the mean is dragged below p90 by the bulk of normal records", mean < p90)
        assertTrue("normal records sit above the mean", stats.sampleDays.count { it > mean } >= 4)
    }

    @Test
    fun `every platform's percentiles match the sample data`() {
        val expected = mapOf(
            SampleData.LUMEN to (24 to 41),
            SampleData.KIBO to (6 to 12),
            SampleData.HALO to (11 to 19),
            SampleData.NORTHLINE to (9 to 16),
            SampleData.VECTOR to (38 to 61)
        )
        for ((id, pair) in expected) {
            val stats = SettleTime.of(id, states, now)
            assertEquals("p50 for platform $id", pair.first, stats.p50Days)
            assertEquals("p90 for platform $id", pair.second, stats.p90Days)
        }
    }

    @Test
    fun `percentiles are nearest-rank over the sorted sample`() {
        val sample = listOf(1L, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        assertEquals(5, SettleTime.percentile(sample, 0.50))
        assertEquals(9, SettleTime.percentile(sample, 0.90))
        assertEquals(null, SettleTime.percentile(emptyList(), 0.90))
    }

    /**
     * Frame `2b`: "Nearly half the wait is In review — a work-phase problem, not a payment problem."
     */
    @Test
    fun `most of the wait is in review, not in payment`() {
        val dwell = SettleTime.of(SampleData.HALO, states, now).stageDwellDays
        val inReview = dwell[app.cleared.data.model.Stage.IN_REVIEW] ?: 0.0
        val payout = dwell[app.cleared.data.model.Stage.PAYOUT_ISSUED] ?: 0.0
        assertTrue("in review $inReview should dominate payout $payout", inReview > payout)
    }

    /**
     * A divergence worth naming rather than papering over.
     *
     * design/sample_data.json marks records 5 and 6 overdue and the prototype row reads
     * `31d · 7 over` — 31 days against Lumen Writers' **p50** of 24. But DATA_MODEL.md, CLAUDE.md
     * rule 8 and the README acceptance checks all say overdue is **p90**, and at p90 (41 d for
     * Lumen, 61 d for Vector Annotate) neither record is late.
     *
     * README.md says the sample data is "realistic but illustrative" while the rule is stated three
     * times as load-bearing, so the rule wins and this test pins that choice down.
     */
    @Test
    fun `overdue fires at p90, which the sample data's own flags disagree with`() {
        val p90 = SettleTime.p90ByPlatform(SampleData.platforms.map { it.id }, states, now)
        assertEquals(41, p90[SampleData.LUMEN])
        assertEquals(61, p90[SampleData.VECTOR])

        val record5 = SampleData.stateOf(5L)
        val record6 = SampleData.stateOf(6L)
        assertFalse("31 d against a p90 of 41 d is not late", Pipeline.isOverdue(record5, now, p90))
        assertFalse("41 d against a p90 of 61 d is not late", Pipeline.isOverdue(record6, now, p90))

        // The same two records against p50, which is what the sample's `overdue: true` flags imply.
        val p50 = SampleData.platforms.associate { it.id to SettleTime.of(it.id, states, now).p50Days!! }
        assertTrue(Pipeline.isOverdue(record5, now, p50))
        assertTrue(Pipeline.isOverdue(record6, now, p50))
        assertEquals(7L, Pipeline.daysOver(record5, now, p50))
    }

    /** Grace days are user-configurable and push the threshold out, never in. */
    @Test
    fun `grace days extend the threshold`() {
        val p50 = mapOf(SampleData.LUMEN to 24)
        val record5 = SampleData.stateOf(5L)
        assertTrue(Pipeline.isOverdue(record5, now, p50))
        assertFalse(Pipeline.isOverdue(record5, now, p50, graceDays = mapOf(SampleData.LUMEN to 10)))
    }
}
