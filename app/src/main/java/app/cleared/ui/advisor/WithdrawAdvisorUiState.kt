package app.cleared.ui.advisor

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.derive.Ledger
import app.cleared.data.derive.Pipeline
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.Withdrawal
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import app.cleared.data.model.PayoutDestination
import app.cleared.data.model.WalletProvider
import app.cleared.ui.format.MoneyFormat
import java.math.BigDecimal
import java.math.RoundingMode

data class CostBarUi(
    val amountLabel: String,
    val pctLabel: String,
    val fraction: Float,
    val isCurrent: Boolean,
    /** True when this size costs more than the balance being advised on. */
    val isWorse: Boolean
)

data class DestinationAdviceUi(
    val title: String,
    val fromPct: String,
    val toPct: String,
    val annualSaving: String
)

data class WithdrawAdvisorUiState(
    val title: String = "",
    val costPct: String = "—",
    val costKes: String = "",
    val explanation: String = "",
    val bars: List<CostBarUi> = emptyList(),
    val splitNote: String = "",
    val advice: DestinationAdviceUi? = null,
    val breakEven: String = "",
    val fxExposure: String = "",
    val fxCaption: String = "",
    val loading: Boolean = true
)

/**
 * Frame `2c` — what this withdrawal costs, and what a cheaper one would look like.
 *
 * Every figure here is a function of the flat fee. That is the whole insight: a fixed cost is a
 * percentage that falls as the amount rises, so *when* you withdraw matters as much as *where*.
 */
object WithdrawAdvisorMapper {

    private const val BREAK_EVEN_TARGET_PCT = 4.5
    private const val FX_SWING_PCT = 3.0

    fun build(
        provider: WalletProvider,
        wallets: List<WalletBalanceEntity>,
        routes: List<WithdrawalRouteEntity>,
        platforms: List<PlatformEntity>,
        states: List<RecordState>,
        rates: Map<Currency, BigDecimal>
    ): WithdrawAdvisorUiState {
        val held = wallets.filter { it.provider == provider }
        val primary = held.maxByOrNull { it.amountMinor } ?: return WithdrawAdvisorUiState(loading = false)
        val currency = primary.currency
        val balance = Money.fromMinor(primary.amountMinor)

        val available = routes.filter { it.provider == provider }
        val route = available.maxByOrNull { Withdrawal.netKes(it, balance, Pipeline.rateFor(currency, rates)) }
            ?: return WithdrawAdvisorUiState(loading = false)

        val currentPct = Withdrawal.costPct(route, balance, currency, rates)
        val midRate = Pipeline.rateFor(currency, rates)
        val costKes = balance.multiply(midRate) - Withdrawal.netKes(route, balance, midRate)

        return WithdrawAdvisorUiState(
            title = "${provider.name.lowercase().replaceFirstChar { it.uppercase() }} · " +
                MoneyFormat.format(currency, balance),
            costPct = "${MoneyFormat.percent(currentPct.toDouble(), decimals = 2)}",
            costKes = MoneyFormat.kes(Money.toKes(costKes)),
            explanation = explanation(route, balance, currency, rates),
            bars = bars(route, balance, currency, rates),
            splitNote = splitNote(route, balance, currency, rates),
            advice = advice(provider, routes, platforms, states, balance, currency, rates),
            breakEven = breakEven(route, currency, rates),
            fxExposure = "± " + MoneyFormat.kes(
                Withdrawal.fxExposureKes(
                    balances = wallets.groupBy { it.currency }
                        .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { acc, r -> acc + Money.fromMinor(r.amountMinor) } },
                    rates = rates,
                    swingPct = FX_SWING_PCT
                )
            ),
            fxCaption = "on a ${MoneyFormat.percent(FX_SWING_PCT, decimals = 0)} move",
            loading = false
        )
    }

    /** States the flat fee as the share of *this* withdrawal that it is. */
    private fun explanation(
        route: WithdrawalRouteEntity,
        balance: BigDecimal,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): String {
        if (balance.signum() <= 0) return ""
        val fee = Money.fromMinor(route.flatFeeMinor)
        val feeShare = fee.divide(balance, java.math.MathContext.DECIMAL64).multiply(BigDecimal(100))
        return "The ${MoneyFormat.format(route.feeCurrency, fee)} flat fee is " +
            "${MoneyFormat.percent(feeShare.toDouble())} at this size. It does not change when the " +
            "amount does, which is the whole reason waiting is worth something."
    }

    /**
     * The cost curve. The bar at the current balance is the accent one; the rest read as better or
     * worse than where the user is standing.
     */
    private fun bars(
        route: WithdrawalRouteEntity,
        balance: BigDecimal,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): List<CostBarUi> {
        val current = balance.setScale(0, RoundingMode.HALF_UP)
        val sizes = (listOf(100, 200, 300, 600, 800).map { BigDecimal(it) } + current)
            .distinct()
            .sorted()

        val curve = Withdrawal.costCurve(route, sizes, currency, rates)
        val worst = curve.maxOfOrNull { it.second } ?: BigDecimal.ONE
        val currentPct = Withdrawal.costPct(route, balance, currency, rates)

        return curve.map { (size, pct) ->
            CostBarUi(
                amountLabel = size.toPlainString(),
                pctLabel = MoneyFormat.percent(pct.toDouble()),
                fraction = if (worst.signum() == 0) 0f
                else pct.divide(worst, java.math.MathContext.DECIMAL64).toFloat().coerceIn(0.05f, 1f),
                isCurrent = size.compareTo(current) == 0,
                isWorse = pct > currentPct
            )
        }
    }

    /** Splitting is never cheaper, and the note says by how much. */
    private fun splitNote(
        route: WithdrawalRouteEntity,
        balance: BigDecimal,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): String {
        val penalty = Withdrawal.splitPenaltyKes(route, balance, parts = 2, currency, rates)
        return "Two withdrawals of ${MoneyFormat.format(currency, balance.divide(BigDecimal(2), 2, RoundingMode.HALF_UP))} " +
            "cost ${MoneyFormat.kes(penalty)} more than one of ${MoneyFormat.format(currency, balance)}. " +
            "Splitting is never cheaper."
    }

    /**
     * The bigger lever: not a cheaper withdrawal, but a platform paying into a cheaper wallet.
     *
     * Only offered when a platform actually pays into this one and another destination beats it.
     */
    private fun advice(
        provider: WalletProvider,
        routes: List<WithdrawalRouteEntity>,
        platforms: List<PlatformEntity>,
        states: List<RecordState>,
        balance: BigDecimal,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): DestinationAdviceUi? {
        val here = routes.filter { it.provider == provider }
        val elsewhere = routes.filter { it.provider != provider }
        if (here.isEmpty() || elsewhere.isEmpty()) return null

        val midRate = Pipeline.rateFor(currency, rates)
        val best = here.maxByOrNull { Withdrawal.netKes(it, balance, midRate) } ?: return null
        val better = elsewhere.maxByOrNull { Withdrawal.netKes(it, balance, midRate) } ?: return null
        if (Withdrawal.netKes(better, balance, midRate) <= Withdrawal.netKes(best, balance, midRate)) return null

        // The platform with the most volume paying into this wallet is the one worth moving.
        val payoutHere = if (provider == WalletProvider.PAYPAL) PayoutDestination.PAYPAL else PayoutDestination.PAYONEER
        val candidates = platforms.filter { it.payoutDestination == payoutHere }
        val volumeByPlatform = candidates.associateWith { platform ->
            states.filter { it.record.platformId == platform.id }
                .fold(BigDecimal.ZERO) { acc, state -> acc + Ledger.finalKesCleared(state.detail) }
        }
        val (platform, volume) = volumeByPlatform.maxByOrNull { it.value } ?: return null
        if (volume.signum() <= 0) return null

        return DestinationAdviceUi(
            title = "Have ${platform.name} pay to ${better.provider.name.lowercase()
                .replaceFirstChar { it.uppercase() }} instead",
            fromPct = MoneyFormat.percent(Withdrawal.costPct(best, balance, currency, rates).toDouble(), decimals = 2),
            toPct = MoneyFormat.percent(Withdrawal.costPct(better, balance, currency, rates).toDouble(), decimals = 2),
            annualSaving = MoneyFormat.kes(
                Withdrawal.destinationSaving(
                    current = best,
                    better = better,
                    annualVolume = volume.divide(Pipeline.rateFor(currency, rates), java.math.MathContext.DECIMAL64),
                    typicalWithdrawal = balance,
                    currency = currency,
                    rates = rates
                )
            )
        )
    }

    private fun breakEven(
        route: WithdrawalRouteEntity,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): String {
        val amount = Withdrawal.breakEvenAmount(route, BREAK_EVEN_TARGET_PCT, currency, rates)
            ?: return "Never falls to ${MoneyFormat.percent(BREAK_EVEN_TARGET_PCT)} on this route"
        return "at ${MoneyFormat.format(currency, amount)}"
    }
}
