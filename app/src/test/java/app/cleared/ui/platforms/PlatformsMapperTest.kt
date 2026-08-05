package app.cleared.ui.platforms

import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frame `1b` — the screen whose job is to make a bad platform obvious at a glance. */
class PlatformsMapperTest {

    private fun state(sort: PlatformSort = PlatformSort.EffectiveRate) =
        PlatformsMapper.build(SampleData.states, SampleData.platforms, sort, SampleData.NOW)

    private fun card(name: String) = state().cards.first { it.name == name }

    /** Effective rate, descending, is the default — the best platform is the first card. */
    @Test
    fun `platforms sort by effective rate descending by default`() {
        val cards = state().cards
        assertEquals(
            listOf("Lumen Writers", "Kibo Studio", "Halo Data", "Northline Freelance", "Vector Annotate"),
            cards.map { it.name }
        )
        assertEquals(listOf("01", "02", "03", "04", "05"), cards.map { it.rank })
    }

    @Test
    fun `the headline is effective KES per hour`() {
        assertEquals("KES 2,275", card("Lumen Writers").effectiveRate)
        assertEquals("KES 1,476", card("Kibo Studio").effectiveRate)
        assertEquals("KES 1,166", card("Halo Data").effectiveRate)
        assertEquals("KES 1,123", card("Northline Freelance").effectiveRate)
        assertEquals("KES 402", card("Vector Annotate").effectiveRate)
    }

    /** `+1,109 vs median` — Lumen's 2,275 against the 1,166 median. */
    @Test
    fun `each card compares itself to the median`() {
        assertEquals(1_166L, state().medianRate)
        assertEquals("+1,109 vs median", card("Lumen Writers").vsMedian)
        assertEquals("at the median", card("Halo Data").vsMedian)
        assertEquals("−764 vs median", card("Vector Annotate").vsMedian)
    }

    /**
     * The entire point of the screen. Vector Annotate is under 0.6 × the median, so its rate is
     * stated in red and it carries the warning block; nobody else does.
     */
    @Test
    fun `only Vector Annotate is flagged, and it says why`() {
        assertEquals(listOf("Vector Annotate"), state().cards.filter { it.isPoor }.map { it.name })
        assertEquals(
            "Lowest rate · 32% of your hours here were never paid",
            card("Vector Annotate").warning
        )
        assertNull(card("Lumen Writers").warning)
    }

    /** Hours logged includes the ones nobody paid for; the bar shows what share that is. */
    @Test
    fun `the hours bar separates paid from unpaid`() {
        val halo = card("Halo Data")
        assertEquals("268 h logged", halo.hoursLogged)
        assertEquals("6 h unpaid (2%)", halo.hoursUnpaid)
        assertEquals((268.0 - 6.0).toFloat() / 268f, halo.paidFraction, 1e-4f)

        val vector = card("Vector Annotate")
        assertEquals("96 h logged", vector.hoursLogged)
        assertEquals("31 h unpaid (32%)", vector.hoursUnpaid)
        assertTrue(vector.hasUnpaidHours)

        assertFalse(card("Kibo Studio").hasUnpaidHours)
    }

    @Test
    fun `the stats row carries approval, days to land and total paid`() {
        val halo = card("Halo Data")
        assertEquals("94% approved", halo.approval)
        assertEquals("11 d to land", halo.daysToLand)
        assertEquals("KES 312,400", halo.totalPaid)
    }

    @Test
    fun `sorting by total paid reorders the cards`() {
        assertEquals(
            listOf("Lumen Writers", "Halo Data", "Kibo Studio", "Northline Freelance", "Vector Annotate"),
            state(PlatformSort.TotalPaid).cards.map { it.name }
        )
    }

    @Test
    fun `sorting by approval puts the platform that never rejects first`() {
        assertEquals("Kibo Studio", state(PlatformSort.Approval).cards.first().name)
        assertEquals("Vector Annotate", state(PlatformSort.Approval).cards.last().name)
    }

    /** Fewest days first: waiting less is better, so this sort ascends where the others descend. */
    @Test
    fun `sorting by days to land is ascending`() {
        assertEquals(
            listOf("Kibo Studio", "Northline Freelance", "Halo Data", "Lumen Writers", "Vector Annotate"),
            state(PlatformSort.DaysToLand).cards.map { it.name }
        )
    }

    /**
     * A platform that bounces payouts is a different problem from one that rejects work. The count
     * is carried separately and only shown when it is not zero.
     */
    @Test
    fun `reversals are counted apart from rejections`() {
        assertTrue(state().cards.all { it.reversedCount == 0 })
    }

    @Test
    fun `the sub-line names the kind of work and the currency it pays in`() {
        assertEquals("AI training tasks · pays in USD", card("Halo Data").subLine)
        assertEquals("Writing work · pays in EUR", card("Lumen Writers").subLine)
        assertEquals("Your own company · pays in USD", card("Kibo Studio").subLine)
    }
}
