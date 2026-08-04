package app.cleared.data.derive

import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frame `1d`, against the `tax2026` block of design/sample_data.json. */
class TaxTest {

    /** 742,180 at 25% and 418,600 at 3%, giving a recommended 198,103 and a shortfall of 34,103. */
    @Test
    fun `the set-aside matches the sample data`() {
        val (personal, company) = Tax.setAside(742_180, 418_600)
        assertEquals(185_545L, personal)
        assertEquals(12_558L, company)
        assertEquals(198_103L, personal + company)
        assertEquals(34_103L, personal + company - 164_000L)
    }

    /** Both rates are user-editable: tax law changes and this app should not need a release. */
    @Test
    fun `both rates are configurable`() {
        val (personal, company) = Tax.setAside(742_180, 418_600, personalRate = 0.30, turnoverTaxRate = 0.015)
        assertEquals(222_654L, personal)
        assertEquals(6_279L, company)
    }

    /** Kibo Studio is the user's own business, so its income is turnover, not personal income. */
    @Test
    fun `company income is kept apart from personal income`() {
        val summary = Tax.summarise(SampleData.platforms, SampleData.states, actualSetAsideKes = 100_000)

        // Kibo Studio is the only isCompany platform in the sample.
        assertEquals(274_500L, summary.companyIncomeKes)
        assertEquals(486_900L + 312_400L + 148_200L + 38_600L, summary.personalIncomeKes)
        assertEquals(4, summary.personalPlatformCount)
        assertTrue(summary.shortfallKes > 0)
    }

    /** Only landed money is income. Nothing in flight is taxed. */
    @Test
    fun `unlanded records are not income`() {
        val summary = Tax.summarise(SampleData.platforms, SampleData.states, actualSetAsideKes = 0)
        val landedOnly = Tax.summarise(
            SampleData.platforms,
            SampleData.states.filter { it.record.id > 8L },
            actualSetAsideKes = 0
        )
        assertEquals(landedOnly.personalIncomeKes, summary.personalIncomeKes)
        assertEquals(landedOnly.companyIncomeKes, summary.companyIncomeKes)
    }
}
