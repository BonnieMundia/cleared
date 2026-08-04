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
