package app.cleared.data.derive

import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import java.math.BigDecimal

/**
 * A route's net, and the ranking of all four — frame `1c`.
 *
 *     netKes(route, amount) = (amount − route.flatFee) × midRate × (1 − route.spreadPct)
 *
 * Because the fee is flat, the ranking genuinely changes with size: small amounts favour the
 * cheap-fee route, large amounts favour the low-spread route. Nothing here hard-codes a winner.
 */
data class RouteQuote(
    val route: WithdrawalRouteEntity,
    val netKes: Long,
    /** Zero on the winner; negative on every other route. */
    val deltaKes: Long,
    val isCheapest: Boolean
)

object Withdrawal {

    fun netKes(route: WithdrawalRouteEntity, amount: BigDecimal, midRate: BigDecimal): BigDecimal =
        (amount - Money.fromMinor(route.flatFeeMinor))
            .multiply(midRate)
            .multiply(Money.remainderOf(route.spreadPct))

    /** Ranked best first. The best route gets the green container; the rest show −(best − net). */
    fun rank(
        routes: List<WithdrawalRouteEntity>,
        amount: BigDecimal,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): List<RouteQuote> {
        val midRate = Pipeline.rateFor(currency, rates)
        val nets = routes.map { it to netKes(it, amount, midRate) }.sortedByDescending { it.second }
        val best = nets.firstOrNull()?.second ?: return emptyList()
        return nets.mapIndexed { index, (route, net) ->
            RouteQuote(
                route = route,
                netKes = Money.toKes(net),
                deltaKes = Money.toKes(net) - Money.toKes(best),
                isCheapest = index == 0
            )
        }
    }

    /**
     * What the withdrawal costs as a percentage of mid-market value — the figure at the top of the
     * withdraw advisor (`2c`).
     */
    fun costPct(
        route: WithdrawalRouteEntity,
        amount: BigDecimal,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): BigDecimal {
        val midRate = Pipeline.rateFor(currency, rates)
        val gross = amount.multiply(midRate)
        if (gross.signum() == 0) return BigDecimal.ZERO
        val net = netKes(route, amount, midRate)
        return (gross - net).divide(gross, java.math.MathContext.DECIMAL64).multiply(BigDecimal(100))
    }

    /**
     * The cost curve on frame `2c` — what a withdrawal of each size costs as a percentage.
     *
     * It falls monotonically because the fee is flat: the same USD 4.99 is 5% of a hundred dollars
     * and 0.6% of eight hundred. Seeing the curve is what tells you to wait.
     */
    fun costCurve(
        route: WithdrawalRouteEntity,
        sizes: List<BigDecimal>,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): List<Pair<BigDecimal, BigDecimal>> =
        sizes.map { it to costPct(route, it, currency, rates) }

    /**
     * The size at which a withdrawal's cost falls to [targetPct] — the break-even the user can be
     * notified at. Null when the fee is already below it, or never reaches it.
     */
    fun breakEvenAmount(
        route: WithdrawalRouteEntity,
        targetPct: Double,
        currency: Currency,
        rates: Map<Currency, BigDecimal>,
        maxAmount: BigDecimal = BigDecimal(5_000)
    ): BigDecimal? {
        val target = BigDecimal.valueOf(targetPct)
        // The curve is monotonic in amount, so a bisection is exact enough and cannot get stuck.
        var low = BigDecimal.ONE
        var high = maxAmount
        if (costPct(route, high, currency, rates) > target) return null
        if (costPct(route, low, currency, rates) <= target) return low

        repeat(40) {
            val mid = (low + high).divide(BigDecimal(2), java.math.MathContext.DECIMAL64)
            if (costPct(route, mid, currency, rates) > target) low = mid else high = mid
        }
        return high.setScale(0, java.math.RoundingMode.CEILING)
    }

    /**
     * What a move to a different payout destination would be worth over a year.
     *
     * design/SCREENS.md `2c`: "At last year's Halo Data volume that difference is KES 12,400."
     */
    fun destinationSaving(
        current: WithdrawalRouteEntity,
        better: WithdrawalRouteEntity,
        annualVolume: BigDecimal,
        typicalWithdrawal: BigDecimal,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): Long {
        if (typicalWithdrawal.signum() <= 0) return 0
        val withdrawals = annualVolume.divide(typicalWithdrawal, java.math.MathContext.DECIMAL64)
        val midRate = Pipeline.rateFor(currency, rates)
        val currentNet = netKes(current, typicalWithdrawal, midRate)
        val betterNet = netKes(better, typicalWithdrawal, midRate)
        return Money.toKes((betterNet - currentNet).multiply(withdrawals))
    }

    /**
     * What a move in the exchange rate would do to money still sitting in a wallet.
     *
     * The balances are unlanded, so they are exposed to the rate until they are withdrawn — which
     * is the other half of the argument against leaving them there.
     */
    fun fxExposureKes(
        balances: Map<Currency, BigDecimal>,
        rates: Map<Currency, BigDecimal>,
        swingPct: Double
    ): Long {
        val total = balances.entries.fold(BigDecimal.ZERO) { acc, (currency, amount) ->
            acc + amount.multiply(Pipeline.rateFor(currency, rates))
        }
        return Money.toKes(total.multiply(Money.pct(swingPct / 100.0)))
    }

    /**
     * Splitting is never cheaper: the flat fee is paid twice. Returns the KES penalty for taking
     * [parts] withdrawals of `amount / parts` instead of one of [amount].
     */
    fun splitPenaltyKes(
        route: WithdrawalRouteEntity,
        amount: BigDecimal,
        parts: Int,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): Long {
        require(parts >= 1) { "parts must be at least 1" }
        val midRate = Pipeline.rateFor(currency, rates)
        val whole = netKes(route, amount, midRate)
        val slice = amount.divide(BigDecimal(parts), java.math.MathContext.DECIMAL64)
        val split = netKes(route, slice, midRate).multiply(BigDecimal(parts))
        return Money.toKes(whole - split)
    }
}
