package app.cleared.ui.money

import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import app.cleared.data.model.WalletProvider
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/** Frame `1c`. Money in a wallet is not money you can spend. */
class MoneyMapperTest {

    private val idleSince = SampleData.NOW.minus(Duration.ofDays(19))

    private val wallets = listOf(
        WalletBalanceEntity(WalletProvider.PAYPAL, Currency.USD, Money.minorOf("412.60"), SampleData.NOW, idleSince),
        WalletBalanceEntity(WalletProvider.PAYONEER, Currency.USD, Money.minorOf("268.00"), SampleData.NOW, idleSince),
        WalletBalanceEntity(WalletProvider.PAYONEER, Currency.EUR, Money.minorOf("340.00"), SampleData.NOW, idleSince)
    )

    private fun state(amount: String = "300", currency: Currency = Currency.USD) =
        MoneyMapper.build(
            wallets = wallets,
            routes = SampleData.routes,
            states = SampleData.states,
            rates = SampleData.RATES,
            amount = amount,
            currency = currency,
            year = 2026,
            now = SampleData.NOW
        )

    /** `wallets` and `walletTotalKes` from design/sample_data.json. */
    @Test
    fun `wallet balances convert at today's mid`() {
        val ui = state()
        val paypal = ui.wallets.first { it.provider == "Paypal" }
        val payoneer = ui.wallets.first { it.provider == "Payoneer" }

        assertEquals("USD 412.60", paypal.balances)
        assertEquals("KES 52,978", paypal.kes)

        assertEquals("USD 268.00 · EUR 340.00", payoneer.balances)
        assertEquals("KES 84,595", payoneer.kes)

        assertEquals("KES 137,573", ui.walletTotal)
    }

    /** Idle time is what turns a balance into a problem. */
    @Test
    fun `the total row says how long the money has sat there`() {
        assertEquals("Not yet withdrawn · idle 19 days", state().idleLabel)
    }

    /** CLAUDE.md: at USD 300 the best route is Payoneer → Equity Bank. */
    @Test
    fun `at USD 300 Payoneer to Equity Bank is cheapest`() {
        val routes = state("300").routes
        assertEquals("Payoneer → Equity Bank", routes.first().label)
        assertEquals("cheapest", routes.first().delta)
        assertEquals("KES 37,561", routes.first().net)
    }

    /** The losers show what they cost, as a negative. */
    @Test
    fun `losing routes show their deficit`() {
        val routes = state("300").routes
        assertEquals(listOf("cheapest", "−350", "−1,007", "−1,013"), routes.map { it.delta })
        assertTrue(routes.drop(1).none { it.isCheapest })
    }

    /**
     * The fee is flat, so the ranking genuinely changes with size. Nothing may hard-code a winner:
     * PayPal → M-Pesa beats PayPal → Equity Bank below about USD 291 and loses above it.
     */
    @Test
    fun `the ranking changes with the amount`() {
        val small = state("40").routes.map { it.label }
        val large = state("1000").routes.map { it.label }
        assertTrue(small.indexOf("PayPal → M-Pesa") < small.indexOf("PayPal → Equity Bank"))
        assertTrue(large.indexOf("PayPal → M-Pesa") > large.indexOf("PayPal → Equity Bank"))
    }

    @Test
    fun `the route sub-line states the fee, the spread and the timing`() {
        val route = state().routes.first { it.label == "Payoneer → Equity Bank" }
        assertEquals("USD 1.50 fee · 2% spread · USD account · 1–2 days", route.subLine)
    }

    @Test
    fun `an empty or zero amount ranks nothing`() {
        assertTrue(state("").routes.isEmpty())
        assertTrue(state("0").routes.isEmpty())
    }

    /** The cost bar is a ranking: the largest cost first, so it reads as one. */
    @Test
    fun `cost segments are ordered largest first and sum to the total`() {
        val ui = MoneyMapper.build(
            wallets = wallets,
            routes = SampleData.routes,
            states = listOf(app.cleared.data.derive.RecordState.of(SampleData.landedRecordDetailExample)),
            rates = SampleData.RATES,
            amount = "300",
            currency = Currency.USD,
            year = 2026,
            now = SampleData.NOW
        )
        val fractions = ui.costSegments.map { it.fraction }
        assertEquals(fractions.sortedDescending(), fractions)
        assertEquals(1.0f, fractions.sum(), 1e-3f)
    }
}
