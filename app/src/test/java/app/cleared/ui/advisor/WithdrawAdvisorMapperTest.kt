package app.cleared.ui.advisor

import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.derive.Withdrawal
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import app.cleared.data.model.WalletProvider
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** Frame `2c` — every figure on it is a consequence of the fee being flat. */
class WithdrawAdvisorMapperTest {

    private val wallets = listOf(
        WalletBalanceEntity(WalletProvider.PAYPAL, Currency.USD, Money.minorOf("412.60"), SampleData.NOW, SampleData.NOW),
        WalletBalanceEntity(WalletProvider.PAYONEER, Currency.USD, Money.minorOf("268.00"), SampleData.NOW, SampleData.NOW),
        WalletBalanceEntity(WalletProvider.PAYONEER, Currency.EUR, Money.minorOf("340.00"), SampleData.NOW, SampleData.NOW)
    )

    private fun state(provider: WalletProvider = WalletProvider.PAYPAL) =
        WithdrawAdvisorMapper.build(
            provider = provider,
            wallets = wallets,
            routes = SampleData.routes,
            platforms = SampleData.platforms,
            states = SampleData.states,
            rates = SampleData.RATES
        )

    @Test
    fun `the title names the wallet and what is in it`() {
        assertEquals("Paypal · USD 412.60", state().title)
    }

    /**
     * `2c` opens on PayPal's USD 412.60 over PayPal → Equity Bank, the better of the two PayPal
     * routes at this size.
     *
     * design/sample_data.json states 4.71% and KES 2,495 for this; the formula gives 4.67% and
     * KES 2,473. Same rounding-by-hand in the handoff figures as the withdrawal-route nets in
     * step 1 — the arithmetic here follows `netKes` exactly.
     */
    @Test
    fun `the headline is what this withdrawal costs`() {
        val ui = state()
        assertEquals("4.67%", ui.costPct)
        assertEquals("KES 2,473", ui.costKes)
    }

    @Test
    fun `the explanation states the flat fee as a share of this withdrawal`() {
        val note = state().explanation
        assertTrue(note, note.contains("USD 4.99"))
        assertTrue(note, note.contains("1.2%"))
    }

    /**
     * The curve only ever falls: the same flat fee is a smaller share of a bigger withdrawal. That
     * monotonicity is the entire argument for waiting.
     */
    @Test
    fun `the cost curve falls monotonically with size`() {
        val bars = state().bars
        assertTrue(bars.size >= 6)
        val pcts = bars.map { it.pctLabel.removeSuffix("%").toDouble() }
        assertEquals(pcts.sortedDescending(), pcts)
    }

    /** Exactly one bar is the current balance, and the ones above it are the cheaper ones. */
    @Test
    fun `the current balance is marked and the worse sizes identified`() {
        val bars = state().bars
        assertEquals(1, bars.count { it.isCurrent })
        val current = bars.indexOfFirst { it.isCurrent }
        assertTrue("smaller withdrawals cost more", bars.take(current).all { it.isWorse })
        assertTrue("larger ones cost less", bars.drop(current + 1).none { it.isWorse })
    }

    @Test
    fun `splitting is never cheaper and the note says by how much`() {
        val note = state().splitNote
        assertTrue(note, note.endsWith("Splitting is never cheaper."))
        assertTrue(note, note.contains("KES "))

        val route = SampleData.routes.first { it.label == "PayPal → Equity Bank" }
        val penalty = Withdrawal.splitPenaltyKes(
            route, BigDecimal("412.60"), 2, Currency.USD, SampleData.RATES
        )
        assertTrue("the penalty is a real cost", penalty > 0)
    }

    /** The bigger lever: a platform paying into a cheaper wallet, not a cheaper withdrawal. */
    @Test
    fun `the advice names a platform and what moving it would save`() {
        val advice = state().advice
        assertNotNull(advice)
        assertTrue(advice!!.title, advice.title.startsWith("Have "))
        assertTrue(advice.title, advice.title.endsWith("pay to Payoneer instead"))
        assertTrue(advice.fromPct.removeSuffix("%").toDouble() > advice.toPct.removeSuffix("%").toDouble())
    }

    /** Payoneer is already the cheapest destination, so there is nothing to advise. */
    @Test
    fun `no advice is offered when the wallet is already the best one`() {
        assertEquals(null, state(WalletProvider.PAYONEER).advice)
    }

    @Test
    fun `the break-even names the size at which the cost falls to the target`() {
        val text = state().breakEven
        assertTrue(text, text.startsWith("at USD "))
    }

    /**
     * The balances are unlanded, so they move with the rate until they are withdrawn — the other
     * half of the argument against leaving them sitting there.
     */
    @Test
    fun `currency exposure covers every wallet, not just this one`() {
        val ui = state()
        assertTrue(ui.fxExposure, ui.fxExposure.startsWith("± KES "))
        assertEquals("on a 3% move", ui.fxCaption)

        // USD 680.60 and EUR 340.00 at today's mid, 3% of the total.
        val expected = Withdrawal.fxExposureKes(
            balances = mapOf(
                Currency.USD to BigDecimal("680.60"),
                Currency.EUR to BigDecimal("340.00")
            ),
            rates = SampleData.RATES,
            swingPct = 3.0
        )
        assertEquals("± ${app.cleared.ui.format.MoneyFormat.kes(expected)}", ui.fxExposure)
    }

    @Test
    fun `an empty wallet degrades rather than failing`() {
        val ui = WithdrawAdvisorMapper.build(
            provider = WalletProvider.PAYPAL,
            wallets = emptyList(),
            routes = SampleData.routes,
            platforms = SampleData.platforms,
            states = SampleData.states,
            rates = SampleData.RATES
        )
        assertEquals("—", ui.costPct)
        assertTrue(ui.bars.isEmpty())
    }
}
