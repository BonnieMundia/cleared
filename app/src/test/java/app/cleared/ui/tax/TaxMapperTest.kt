package app.cleared.ui.tax

import app.cleared.data.derive.Tax
import app.cleared.data.export.CsvExport
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Frame `1d`. Personal income and company turnover are taxed differently and stay apart. */
class TaxMapperTest {

    private fun state(year: Int? = null, setAside: Long = 164_000) = TaxMapper.build(
        states = SampleData.states,
        platforms = SampleData.platforms,
        selectedYear = year,
        actualSetAsideKes = setAside,
        personalRate = 0.25,
        turnoverTaxRate = 0.03,
        setAsideLocation = "Equity savings",
        setAsideLastMoved = LocalDate.of(2026, 7, 28)
    )

    @Test
    fun `personal and company income are kept apart`() {
        val ui = state()
        // Kibo Studio is the only isCompany platform; everything else is personal.
        assertEquals("KES 986,100", ui.personal!!.figure)
        assertEquals("KES 274,500", ui.company!!.figure)
        assertEquals("Company income · Kibo Studio", ui.company!!.overline)
    }

    @Test
    fun `each block states the rate it applies`() {
        val ui = state()
        assertEquals("Set aside at 25%", ui.personal!!.rowLabel)
        assertEquals("KES 246,525", ui.personal!!.rowValue)
        assertEquals("Turnover tax at 3%", ui.company!!.rowLabel)
        assertEquals("KES 8,235", ui.company!!.rowValue)
    }

    @Test
    fun `the company line says which side of the threshold it sits on`() {
        assertTrue(state().company!!.context.endsWith("below the 5M threshold"))
    }

    @Test
    fun `the set-aside shows the shortfall against what is recommended`() {
        val ui = state()
        assertEquals("KES 164,000", ui.setAside)
        assertEquals("of KES 254,760", ui.setAsideOf)
        assertEquals("KES 90,760", ui.shortfall)
        assertEquals(164_000f / 254_760f, ui.setAsideFraction, 1e-4f)
        assertEquals("Held in Equity savings · last moved 28 Jul", ui.heldIn)
    }

    /** Nothing short means nothing to say — the line is absent rather than showing a zero. */
    @Test
    fun `a fully funded set-aside has no shortfall line`() {
        assertNull(state(setAside = 300_000).shortfall)
    }

    /**
     * Income is recognised when it lands, so a year with nothing landed in it is genuinely empty
     * rather than an error.
     */
    @Test
    fun `selecting a year with no income shows zeroes rather than failing`() {
        val ui = state(year = 1999)
        assertEquals("KES 0", ui.personal!!.figure)
        assertEquals("KES 0", ui.company!!.figure)
        assertEquals(0, ui.recordCount)
    }

    @Test
    fun `the year tabs come from the years anything actually landed in`() {
        val years = Tax.yearsWithIncome(SampleData.states)
        assertTrue(years.isNotEmpty())
        assertEquals(years.sortedDescending(), years)
        assertEquals(years, state().years)
    }

    @Test
    fun `the export label and caption name what is being exported`() {
        assertEquals("Export CSV · 2026", state(year = 2026).exportLabel)
        assertEquals("Export CSV · all", state().exportLabel)
        assertTrue(
            state().exportCaption.endsWith(
                "with every fee, rate and timestamp. Personal and company rows are tagged separately."
            )
        )
    }

    // ── The CSV itself ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `the csv tags personal and company rows separately`() {
        val csv = CsvExport.build(SampleData.states, SampleData.platforms)
        val lines = csv.trim().lines()
        assertTrue(lines.first().startsWith("record_id,platform,tagged_as,"))

        val kiboRows = lines.filter { it.contains("Kibo Studio") }
        assertTrue(kiboRows.isNotEmpty())
        assertTrue(kiboRows.all { it.contains(",company,") })

        val haloRows = lines.filter { it.contains("Halo Data") }
        assertTrue(haloRows.isNotEmpty())
        assertTrue(haloRows.all { it.contains(",personal,") })
    }

    /** A rejected record is in the export too — its hours are part of the story. */
    @Test
    fun `the csv includes rejected records`() {
        val csv = CsvExport.build(SampleData.states, SampleData.platforms)
        assertTrue(csv.lines().any { it.contains(",REJECTED,") })
    }

    @Test
    fun `the csv quotes any field that would break the format`() {
        val platforms = SampleData.platforms.map {
            if (it.id == SampleData.HALO) it.copy(name = "Halo, Data \"Ltd\"") else it
        }
        val csv = CsvExport.build(SampleData.states, platforms)
        assertTrue(csv.contains("\"Halo, Data \"\"Ltd\"\"\""))
    }

    @Test
    fun `the csv file name names the scope`() {
        assertEquals("cleared-2026.csv", CsvExport.fileName(2026))
        assertEquals("cleared-all.csv", CsvExport.fileName(null))
    }

    @Test
    fun `filtering the csv by year drops everything that landed elsewhere`() {
        val all = CsvExport.build(SampleData.states, SampleData.platforms).trim().lines().size
        val none = CsvExport.build(SampleData.states, SampleData.platforms, year = 1999).trim().lines().size
        assertTrue(all > none)
        assertEquals(1, none) // header only
        assertNotNull(CsvExport.build(SampleData.states, SampleData.platforms, year = 2026))
        assertFalse(CsvExport.build(SampleData.states, SampleData.platforms, year = 1999).contains("Halo Data"))
    }
}
