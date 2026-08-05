package app.cleared.ui.settletime

import app.cleared.data.derive.SettleTime
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frame `2b` — the screen that shows why the threshold is p90 rather than an average. */
class SettleTimeMapperTest {

    private val halo = SampleData.platforms.first { it.id == SampleData.HALO }

    private fun state() =
        SettleTimeMapper.build(halo, SampleData.states, SampleData.NOW)

    @Test
    fun `the headline is the median over the landed sample`() {
        val ui = state()
        assertEquals("11 d", ui.medianDays)
        assertEquals("median · 15 records", ui.sampleCaption)
    }

    @Test
    fun `the stat strip states p50, the overdue threshold, and the drift`() {
        val ui = state()
        assertEquals("p50 11 d", ui.p50)
        assertEquals("p90 · overdue at 19 d", ui.p90)
        assertTrue(ui.drift, ui.drift.startsWith("Drift · "))
    }

    /** Twelve bars, and every landed record lands in exactly one of them. */
    @Test
    fun `the histogram has twelve bars covering the whole sample`() {
        val ui = state()
        assertEquals(SettleTime.HISTOGRAM_BARS, ui.buckets.size)
        assertEquals(15, ui.buckets.sumOf { it.count })
    }

    /**
     * The tail past p90 is what the threshold exists to catch, and it is the part the design draws
     * in amber. Halo's 45-day record is out there on its own.
     */
    @Test
    fun `the bars past p90 are marked as the tail`() {
        val ui = state()
        val tail = ui.buckets.filter { it.isTail }
        assertTrue(tail.isNotEmpty())
        assertTrue("every tail bucket starts at or past p90", tail.all { it.fromDays >= 19 })
        assertEquals("one record took far longer than the rest", 1, tail.sumOf { it.count })
    }

    /** The last bar is the overflow, so one four-month record cannot flatten the other eleven. */
    @Test
    fun `the last bar is an overflow bucket`() {
        val ui = state()
        assertTrue(ui.buckets.last().isOverflow)
        assertTrue(ui.buckets.last().label.endsWith("d+"))
        assertFalse(ui.buckets.first().isOverflow)
    }

    /**
     * The argument the screen exists to make. Halo's mean is 13.4 d against a p90 of 19: flagging
     * on the mean would nag about perfectly normal records.
     */
    @Test
    fun `the note explains why the mean would be the wrong threshold`() {
        val note = state().meanNote
        assertTrue(note, note.contains("13.4 d"))
        assertTrue(note, note.contains("recomputes it every time a record lands"))
    }

    /** Frame `2b`: most of the wait is in review, which is a work-phase problem. */
    @Test
    fun `the dwell bar names where the wait actually is`() {
        val ui = state()
        assertTrue(ui.dwell.isNotEmpty())
        assertEquals(1f, ui.dwell.sumOf { it.fraction.toDouble() }.toFloat(), 1e-3f)

        val worst = ui.dwell.maxByOrNull { it.days }!!
        assertEquals(app.cleared.data.model.Stage.IN_REVIEW, worst.stage)
        assertTrue(ui.dwellNote, ui.dwellNote.contains("In review"))
        assertTrue(ui.dwellNote, ui.dwellNote.endsWith("a work-phase problem, not a payment problem."))
    }

    /** At p90 nothing on Halo is late, so the chase card is absent rather than showing a zero. */
    @Test
    fun `the chase card is absent when nothing is past p90`() {
        assertEquals(0, state().overdueCount)
    }

    @Test
    fun `a platform with no landed history degrades to dashes rather than failing`() {
        val empty = SettleTimeMapper.build(halo, emptyList(), SampleData.NOW)
        assertEquals("—", empty.medianDays)
        assertTrue(empty.buckets.isEmpty())
        assertEquals("", empty.meanNote)
    }
}
