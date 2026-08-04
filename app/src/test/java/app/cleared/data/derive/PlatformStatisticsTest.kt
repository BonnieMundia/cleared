package app.cleared.data.derive

import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The effective rate is the product thesis: everything a platform ever paid, divided by every hour
 * it was ever given, including the hours it never paid for.
 */
class PlatformStatisticsTest {

    private val stats = PlatformStatistics.all(SampleData.platforms, SampleData.states)
    private fun of(id: Long) = stats.first { it.platform.id == id }

    /** The figure design/PROMPTS.md singles out. */
    @Test
    fun `Vector Annotate clears 402 KES per hour`() {
        assertEquals(402L, of(SampleData.VECTOR).effectiveKesPerHour)
    }

    /** All five, from `platforms[].stats.effectiveKesPerHour`. */
    @Test
    fun `every platform's effective rate matches the sample data`() {
        assertEquals(2275L, of(SampleData.LUMEN).effectiveKesPerHour)
        assertEquals(1476L, of(SampleData.KIBO).effectiveKesPerHour)
        assertEquals(1166L, of(SampleData.HALO).effectiveKesPerHour)
        assertEquals(1123L, of(SampleData.NORTHLINE).effectiveKesPerHour)
        assertEquals(402L, of(SampleData.VECTOR).effectiveKesPerHour)
    }

    @Test
    fun `total paid matches the sample data`() {
        assertEquals(486_900L, of(SampleData.LUMEN).totalPaidKes)
        assertEquals(274_500L, of(SampleData.KIBO).totalPaidKes)
        assertEquals(312_400L, of(SampleData.HALO).totalPaidKes)
        assertEquals(148_200L, of(SampleData.NORTHLINE).totalPaidKes)
        assertEquals(38_600L, of(SampleData.VECTOR).totalPaidKes)
    }

    @Test
    fun `hours logged and unpaid match the sample data`() {
        assertEquals(214.0, of(SampleData.LUMEN).hoursTotal, 1e-6)
        assertEquals(186.0, of(SampleData.KIBO).hoursTotal, 1e-6)
        assertEquals(268.0, of(SampleData.HALO).hoursTotal, 1e-6)
        assertEquals(132.0, of(SampleData.NORTHLINE).hoursTotal, 1e-6)
        assertEquals(96.0, of(SampleData.VECTOR).hoursTotal, 1e-6)

        assertEquals(9.0, of(SampleData.LUMEN).hoursUnpaid, 1e-6)
        assertEquals(6.0, of(SampleData.HALO).hoursUnpaid, 1e-6)
        assertEquals(31.0, of(SampleData.VECTOR).hoursUnpaid, 1e-6)
    }

    /** `_platformNotes`: median 1166, flag below 0.6 x median, Vector Annotate the only one flagged. */
    @Test
    fun `Vector Annotate is the only flagged platform`() {
        val median = PlatformStatistics.medianRate(stats)
        assertEquals(1166L, median)

        val flagged = stats.filter { PlatformStatistics.isPoor(it, median) }
        assertEquals(listOf("Vector Annotate"), flagged.map { it.platform.name })
    }

    @Test
    fun `approval rates match the sample data`() {
        assertEquals(88, of(SampleData.LUMEN).approvalPct)
        assertEquals(100, of(SampleData.KIBO).approvalPct)
        assertEquals(94, of(SampleData.HALO).approvalPct)
        assertEquals(91, of(SampleData.NORTHLINE).approvalPct)
        assertEquals(61, of(SampleData.VECTOR).approvalPct)
    }

    /**
     * CLAUDE.md: "Test that a rejected record still appears in its platform's hour total."
     *
     * Record 8 is Halo Data, 1.5 h, rejected. Drop it and Halo's hour total falls by exactly those
     * 1.5 hours and its effective rate rises — which is the behaviour the app exists to prevent.
     */
    @Test
    fun `a rejected record keeps its hours against the platform`() {
        val withRejected = of(SampleData.HALO)
        assertEquals(268.0, withRejected.hoursTotal, 1e-6)
        assertEquals(1, withRejected.rejectedCount)

        val without = PlatformStatistics.of(
            SampleData.platforms.first { it.id == SampleData.HALO },
            SampleData.states.filter { it.record.id != 8L }
        )
        assertEquals(266.5, without.hoursTotal, 1e-6)
        assertTrue(
            "dropping a rejected record must flatter the platform, which is why we never do it",
            without.effectiveKesPerHour > withRejected.effectiveKesPerHour
        )
        assertFalse(withRejected.effectiveKesPerHour == without.effectiveKesPerHour)
    }
}
