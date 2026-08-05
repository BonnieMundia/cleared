package app.cleared.ui.money

import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.derive.CalendarDays
import app.cleared.data.derive.CostOfGettingPaid
import app.cleared.data.derive.Pipeline
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.Withdrawal
import app.cleared.data.model.Currency
import app.cleared.data.model.FeeKind
import app.cleared.data.model.Money
import app.cleared.ui.format.MoneyFormat
import java.math.BigDecimal
import java.time.Instant

data class WalletRowUi(
    val provider: String,
    /** The enum name, for the advisor route. */
    val providerKey: String,
    val balances: String,
    val kes: String
)

data class CostSegmentUi(
    val kind: FeeKind,
    val label: String,
    val amount: String,
    val fraction: Float
)

data class RouteQuoteUi(
    val id: Long,
    val label: String,
    val subLine: String,
    val net: String,
    val delta: String,
    val isCheapest: Boolean
)

data class MoneyUiState(
    val wallets: List<WalletRowUi> = emptyList(),
    val walletTotal: String = "KES 0",
    val idleLabel: String = "",
    val costTotal: String = "KES 0",
    val costCaption: String = "",
    val costSegments: List<CostSegmentUi> = emptyList(),
    val amount: String = "300",
    val currency: Currency = Currency.USD,
    val routes: List<RouteQuoteUi> = emptyList(),
    val loading: Boolean = true
)

/**
 * Frame `1c` — money that exists but is not yet yours to spend.
 *
 * `RECEIVED` means the money is sitting in PayPal or Payoneer; `LANDED` means it is in KES at the
 * bank. This screen is the gap between those two, priced.
 */
object MoneyMapper {

    fun build(
        wallets: List<WalletBalanceEntity>,
        routes: List<WithdrawalRouteEntity>,
        states: List<RecordState>,
        rates: Map<Currency, BigDecimal>,
        amount: String,
        currency: Currency,
        year: Int,
        now: Instant
    ): MoneyUiState {
        val byProvider = wallets.groupBy { it.provider }

        val walletRows = byProvider.map { (provider, balances) ->
            WalletRowUi(
                provider = provider.name.lowercase().replaceFirstChar { it.uppercase() },
                providerKey = provider.name,
                balances = balances.joinToString(" · ") {
                    MoneyFormat.formatMinor(it.currency, it.amountMinor)
                },
                kes = MoneyFormat.kes(Money.toKes(kesOf(balances, rates)))
            )
        }

        val total = wallets.fold(BigDecimal.ZERO) { acc, w ->
            acc + Money.fromMinor(w.amountMinor).multiply(Pipeline.rateFor(w.currency, rates))
        }

        // Idle time is what makes a wallet balance a problem rather than a fact.
        val idleSince = wallets.mapNotNull { it.idleSince }.minOrNull()
        val idleDays = idleSince?.let { CalendarDays.between(it, now) }

        val cost = CostOfGettingPaid.of(states, rates, year)
        val costTotal = cost.totalKes

        return MoneyUiState(
            wallets = walletRows,
            walletTotal = MoneyFormat.kes(Money.toKes(total)),
            idleLabel = idleDays?.let { "Not yet withdrawn · idle $it days" } ?: "Not yet withdrawn",
            costTotal = MoneyFormat.kes(costTotal),
            costCaption = if (cost.landedKes == 0L) "nothing has landed this year yet"
            else "${MoneyFormat.percent(cost.pctOfLanded)} of everything that landed this year",
            costSegments = segments(cost.byKind, costTotal),
            amount = amount,
            currency = currency,
            routes = quotes(routes, amount, currency, rates),
            loading = false
        )
    }

    private fun kesOf(balances: List<WalletBalanceEntity>, rates: Map<Currency, BigDecimal>) =
        balances.fold(BigDecimal.ZERO) { acc, b ->
            acc + Money.fromMinor(b.amountMinor).multiply(Pipeline.rateFor(b.currency, rates))
        }

    /** Largest first, so the bar reads as a ranking of what getting paid actually costs. */
    private fun segments(byKind: Map<FeeKind, Long>, total: Long): List<CostSegmentUi> =
        byKind.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (kind, amount) ->
                CostSegmentUi(
                    kind = kind,
                    label = kindLabel(kind),
                    amount = MoneyFormat.kes(amount),
                    fraction = if (total == 0L) 0f else (amount.toDouble() / total).toFloat()
                )
            }

    private fun kindLabel(kind: FeeKind) = when (kind) {
        FeeKind.FX_SPREAD -> "Currency conversion spread"
        FeeKind.PLATFORM_COMMISSION -> "Platform commission"
        FeeKind.WITHDRAWAL_FEE -> "Withdrawal fees"
        FeeKind.BANK_CREDIT_FEE -> "Bank credit fees"
        FeeKind.RETURN_HANDLING_FEE -> "Return handling fees"
    }

    /**
     * All four routes, re-ranked live. The fee is flat, so the winner genuinely changes with size
     * and nothing here may assume one.
     */
    private fun quotes(
        routes: List<WithdrawalRouteEntity>,
        amount: String,
        currency: Currency,
        rates: Map<Currency, BigDecimal>
    ): List<RouteQuoteUi> {
        val value = amount.toBigDecimalOrNull() ?: return emptyList()
        if (value.signum() <= 0) return emptyList()

        return Withdrawal.rank(routes, value, currency, rates).map { quote ->
            RouteQuoteUi(
                id = quote.route.id,
                label = quote.route.label,
                subLine = buildString {
                    append(MoneyFormat.formatMinor(quote.route.feeCurrency, quote.route.flatFeeMinor))
                    append(" fee · ")
                    append(MoneyFormat.percent(quote.route.spreadPct * 100))
                    append(" spread")
                    quote.route.note?.let { append(" · $it") }
                },
                net = MoneyFormat.kes(quote.netKes),
                delta = if (quote.isCheapest) "cheapest"
                else MoneyFormat.digits(Currency.KES, BigDecimal.valueOf(quote.deltaKes)),
                isCheapest = quote.isCheapest
            )
        }
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        trim().takeIf { it.isNotEmpty() }?.let { runCatching { BigDecimal(it) }.getOrNull() }
}
