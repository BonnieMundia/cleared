package app.cleared.ui.discover

import app.cleared.data.discovery.FixtureDiscoverySource
import app.cleared.fixture.SampleData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frames `3a` and `3b`. */
class DiscoverMapperTest {

    private val scan = runBlocking { FixtureDiscoverySource { SampleData.NOW }.scan() }

    private fun state(filter: DiscoverFilter = DiscoverFilter.All) = DiscoverMapper.build(
        scan = scan,
        platforms = SampleData.platforms,
        routes = SampleData.routes,
        states = SampleData.states,
        rates = SampleData.RATES,
        filter = filter,
        now = SampleData.NOW
    )

    private fun detail(listingId: Long) = DiscoverMapper.detail(
        listingId = listingId,
        scan = scan,
        platforms = SampleData.platforms,
        routes = SampleData.routes,
        states = SampleData.states,
        rates = SampleData.RATES
    )

    @Test
    fun `listings are ranked by what they would actually pay`() {
        val titles = state().listings.filter { it.isPriced }.map { it.title }
        assertEquals("Landing page copy, fixed price", titles.first())
        assertEquals("Bounding box batch, 900 frames", titles.last())
    }

    /**
     * An unpriced listing sorts after every priced one, not below the worst-paying job on the board.
     *
     * "We do not know what this pays" is not "this pays nearly nothing", and the old code conflated
     * them: with no hours the rate floored to zero, so a listing nobody had estimated ranked last
     * and read as the worst work available. That is the failure mode every real source would hit,
     * because no board publishes hours.
     */
    @Test
    fun `an unpriced listing sorts after the priced ones, not below the worst`() {
        val rows = state().listings
        val unpriced = rows.filter { !it.isPriced }
        val priced = rows.filter { it.isPriced }

        assertEquals(1, unpriced.size)
        assertEquals("Audio QA, 12 h per week", unpriced.single().title)
        assertEquals(rows.takeLast(unpriced.size), unpriced)
        assertTrue("priced listings all come first", rows.take(priced.size).all { it.isPriced })
    }

    @Test
    fun `an unpriced listing shows no rate and says why`() {
        val row = state().listings.single { !it.isPriced }
        assertEquals("Not priced yet", row.rate)
        assertEquals("tap to estimate the hours", row.vsMedian)
        assertEquals("not stated — you decide", row.hours)
        assertFalse("unpriced is not below median; it is unknown", row.isBelowMedian)
    }

    /** A listing with no rate cannot be claimed to beat the median. */
    @Test
    fun `the above-median filter excludes unpriced listings`() {
        assertTrue(state(DiscoverFilter.AboveMedian).listings.all { it.isPriced })
    }

    /** The header quotes the best *priced* listing; an unpriced one cannot be the best. */
    @Test
    fun `the best-available figure ignores unpriced listings`() {
        val ui = state()
        assertEquals("KES 3,140", ui.bestRate)
    }

    /**
     * The breakdown stops where the arithmetic does. Everything above the division is still true
     * and still shown; only the last two lines depend on an estimate nobody has made.
     */
    @Test
    fun `an unpriced breakdown stops at what lands`() {
        val page = detail(4)
        assertFalse(page.isPriced)
        assertEquals("Not priced yet", page.rate)

        val labels = page.breakdown.map { it.label }
        assertTrue(labels.contains("Stated pay"))
        assertTrue(labels.contains("Lands as"))

        val hours = page.breakdown.last()
        assertEquals("Divided by hours", hours.label)
        assertEquals("—", hours.value)
        assertEquals("no estimate yet", hours.subLabel)
        assertFalse("there is no projected effective line", labels.contains("Projected effective"))
    }

    @Test
    fun `an unpriced listing explains what it needs rather than apologising`() {
        assertTrue(detail(4).riskNote.startsWith("No board states how long a job will take"))
    }

    /** Supplying the hours prices it, using the same arithmetic as a listing that came with them. */
    @Test
    fun `estimating the hours prices the listing`() {
        val estimated = scan.listings.map {
            if (it.id == 4L) it.copy(estHours = 24.0, assessmentHours = 5.0, hoursEstimatedByUser = true)
            else it
        }
        val ui = DiscoverMapper.detail(
            listingId = 4,
            scan = scan.copy(listings = estimated),
            platforms = SampleData.platforms,
            routes = SampleData.routes,
            states = SampleData.states,
            rates = SampleData.RATES
        )

        assertTrue(ui.isPriced)
        assertTrue(ui.rate.startsWith("KES "))
        assertEquals(29.0, ui.estHours + ui.assessmentHours, 1e-9)

        val hours = ui.breakdown.first { it.label == "Divided by hours" }
        assertEquals("29 h", hours.value)
        assertEquals("24 h of work + 5 h unpaid assessment", hours.subLabel)
        assertEquals("Projected effective", ui.breakdown.last().label)
    }

    /**
     * The screen and the detail agree, because they are the same number.
     *
     * design/sample_data.json gives 1,651 for the Halo listing while design/SCREENS.md `3b` works
     * the same listing out at 1,520. The first is the *gross* rate — stated pay at mid over hours,
     * with no commission and no withdrawal cost — and the second is DATA_MODEL.md's net formula.
     * Both screens use the net one here: a projection that ignores the commission and the fee
     * flatters every listing, and this screen exists to stop the user taking bad work.
     */
    @Test
    fun `the card and the detail show the same rate for the same listing`() {
        val card = state().listings.first { it.title.startsWith("Dialogue rating") }
        val page = detail(3)
        assertEquals(card.rate, page.rate)
    }

    /** Net is lower than the gross figure in the sample data, necessarily. */
    @Test
    fun `the projection is net of commission and the route`() {
        val page = detail(3)
        val rate = page.rate.removePrefix("KES ").replace(",", "").toLong()
        assertTrue("net must be below the gross 1,651 the sample states", rate < 1_651)
        assertTrue("but still a real figure", rate > 0)
    }

    /** Frame `3b` shows its working: every line of the arithmetic is on the screen. */
    @Test
    fun `the breakdown shows the whole calculation`() {
        val lines = detail(3).breakdown
        assertEquals("Stated pay", lines[0].label)
        assertEquals("USD 360.00", lines[0].value)

        assertTrue(lines.any { it.label.startsWith("Platform commission") })
        assertTrue(lines.any { it.label == "Withdrawal and FX, your usual route" })
        assertTrue(lines.any { it.label == "Lands as" })

        val hours = lines.first { it.label == "Divided by hours" }
        assertEquals("28 h", hours.value)
        assertEquals("26 h of work + 2 h unpaid assessment", hours.subLabel)

        val total = lines.last()
        assertEquals("Projected effective", total.label)
        assertTrue(total.isTotal)
    }

    /**
     * A listing on a platform that rejects 39% of submissions is shown at 61% of its headline rate.
     * That is the reason this feature lives in this app rather than in a browser tab.
     */
    @Test
    fun `the risk adjustment applies the platform's own approval rate`() {
        val vector = state().listings.first { it.title.startsWith("Bounding box") }
        assertTrue(vector.adjusted, vector.adjusted.contains("risk-adjusted at 61%"))
        assertTrue(detail(5).riskNote.contains("61% approval rate"))
    }

    /** No history means nothing to adjust against, and the screen says so rather than guessing. */
    @Test
    fun `a platform with no history has no risk adjustment`() {
        val meridian = state().listings.first { it.title.startsWith("Audio QA") }
        // Two separate unknowns, and the card must not conflate them: nobody has estimated the
        // hours, *and* there is no history with this platform to adjust against.
        assertEquals("no approval history", meridian.adjusted)
        assertEquals("no history with this platform", meridian.then)
        assertTrue(detail(4).platformStatsNote.startsWith("You have never worked"))
    }

    @Test
    fun `unpaid assessment hours are called out on the card`() {
        val halo = state().listings.first { it.title.startsWith("Dialogue rating") }
        assertTrue(halo.hasAssessment)
        assertEquals("26 h est + 2 h unpaid assessment", halo.hours)

        val northline = state().listings.first { it.title.startsWith("Landing page") }
        assertFalse(northline.hasAssessment)
        assertEquals("14 h est", northline.hours)
    }

    /** Below-median listings state their rate in red and carry the reason. */
    @Test
    fun `below-median listings are flagged with a reason`() {
        val vector = state().listings.first { it.title.startsWith("Bounding box") }
        assertTrue(vector.isBelowMedian)
        assertNotNull(vector.warning)
        assertTrue(vector.warning!!.contains("39% of what you submit here is rejected"))
    }

    @Test
    fun `the filters narrow the list`() {
        val all = state().listings.size
        assertEquals(5, all)

        assertTrue(state(DiscoverFilter.AboveMedian).listings.size < all)
        assertTrue(state(DiscoverFilter.NoAssessment).listings.all { !it.hasAssessment })
        assertEquals(2, state(DiscoverFilter.Writing).listings.size)
    }

    @Test
    fun `the scan caption names the sources and how many are shown`() {
        val caption = state(DiscoverFilter.Writing).scanCaption
        assertTrue(caption, caption.startsWith("Scanned 6 platform boards and 2 community feeds"))
        assertTrue(caption, caption.endsWith("2 of 5 shown"))
    }

    @Test
    fun `the header compares the best listing to the median`() {
        val ui = state()
        assertTrue(ui.bestCaption, ui.bestCaption.contains("× your median of KES 1,166/h"))
    }

    @Test
    fun `a listing that does not exist degrades rather than failing`() {
        val missing = detail(999)
        assertEquals("", missing.title)
        assertTrue(missing.breakdown.isEmpty())
        assertNull(missing.platformId)
    }
}
