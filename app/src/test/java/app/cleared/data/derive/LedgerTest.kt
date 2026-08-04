package app.cleared.data.derive

import app.cleared.data.model.Money
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * "What happened to the money" — frame `1e`, against the `recordDetailExample` block of
 * design/sample_data.json.
 */
class LedgerTest {

    private val record = SampleData.landedRecordDetailExample

    @Test
    fun `gross less same-currency fees is EUR 606-50`() {
        assertEquals(BigDecimal("606.50"), Ledger.convertedAmount(record))
    }

    @Test
    fun `the record clears KES 88220`() {
        assertEquals(88_220L, Money.toKes(Ledger.finalKesCleared(record)))
    }

    @Test
    fun `mid-market value is KES 94464 and the cost of getting paid is KES 6244`() {
        assertEquals(94_464L, Money.toKes(Ledger.midMarketKes(record)))
        assertEquals(6_244L, Money.toKes(Ledger.totalCostKes(record)))
    }

    /** `totalCostPct` 6.6 and `keptPct` 93.4. */
    @Test
    fun `the cost is 6-6 percent and 93-4 percent is kept`() {
        val cost = Ledger.totalCostKes(record)
        val mid = Ledger.midMarketKes(record)
        val pct = cost.divide(mid, 6, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        assertEquals(BigDecimal("6.6"), pct.setScale(1, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("93.4"), (BigDecimal(100) - pct).setScale(1, RoundingMode.HALF_UP))
    }

    /** `conversion.spreadPct` 0.012 — mid 147.60 against an applied 145.82. */
    @Test
    fun `the conversion spread is 1-2 percent`() {
        val spread = CostOfGettingPaid.spreadPct(BigDecimal("147.60"), BigDecimal("145.82"))
        assertEquals(BigDecimal("1.2"), spread.setScale(1, RoundingMode.HALF_UP))
    }

    /** Only LANDED money counts. An in-flight record clears nothing yet. */
    @Test
    fun `an unlanded record has cleared nothing`() {
        assertEquals(0, Ledger.finalKesCleared(SampleData.pipelineById.getValue(1L)).signum())
    }
}
