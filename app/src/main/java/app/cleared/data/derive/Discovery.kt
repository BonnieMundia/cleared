package app.cleared.data.derive

import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import java.math.BigDecimal
import java.math.MathContext

/**
 * The Discovery projection — frames `3a` and `3b`.
 *
 *     netKes      = statedPay × (1 − platform.commissionPct)
 *                   × (1 − usualRoute.spreadPct) × midRate − flatFeeInKes
 *     projected/h = netKes ÷ (estHours + assessmentHours)
 *     riskAdjusted = projected/h × approvalRate(platform)   // null if no history
 *
 * A listing on a platform that rejects 39% of submissions is shown at 61% of its headline rate.
 * That is the reason this feature lives inside this app rather than in a browser tab.
 *
 * Discovery reads public boards only. It never signs in as the user and never applies on his behalf.
 */
data class ListingProjection(
    val listing: ListingEntity,
    val netKes: Long,
    val projectedKesPerHour: Long,
    val riskAdjustedKesPerHour: Long?,
    val totalHours: Double
)

/** One line of the "How that number is built" table on frame `3b`. */
data class ProjectionLine(
    val label: String,
    val value: String,
    val subLabel: String? = null,
    val isTotal: Boolean = false
)

object Discovery {

    /**
     * The projection written out as arithmetic, so `3b` can show its working.
     *
     * A number nobody can check is a number nobody should act on, and this one decides whether the
     * user spends a week on something.
     */
    fun breakdown(
        listing: ListingEntity,
        platform: PlatformEntity?,
        usualRoute: WithdrawalRouteEntity?,
        rates: Map<Currency, BigDecimal>,
        projection: ListingProjection,
        formatMoney: (Currency, BigDecimal) -> String,
        formatKes: (Long) -> String,
        formatHours: (Double) -> String,
        formatPercent: (Double) -> String
    ): List<ProjectionLine> {
        val currency = listing.currency
        val stated = Money.fromMinor(listing.statedPayMinor)
        val commissionPct = platform?.commissionPct ?: 0.0
        val commission = stated.multiply(Money.pct(commissionPct))
        val afterCommission = stated - commission

        val spreadPct = usualRoute?.spreadPct ?: 0.0
        val spreadCost = afterCommission.multiply(Money.pct(spreadPct))
        val flatFee = usualRoute?.let { Money.fromMinor(it.flatFeeMinor) } ?: BigDecimal.ZERO
        val routeCost = spreadCost + flatFee

        val rows = mutableListOf<ProjectionLine>()
        rows += ProjectionLine("Stated pay", formatMoney(currency, stated))

        if (commission.signum() > 0) {
            rows += ProjectionLine(
                label = "Platform commission ${formatPercent(commissionPct * 100)}",
                value = "−" + formatMoney(currency, commission)
            )
        }

        if (routeCost.signum() > 0 && usualRoute != null) {
            rows += ProjectionLine(
                label = "Withdrawal and FX, your usual route",
                value = "−" + formatMoney(usualRoute.feeCurrency, routeCost),
                subLabel = "${usualRoute.label}, ${formatPercent(spreadPct * 100)}"
            )
        }

        rows += ProjectionLine("Lands as", formatKes(projection.netKes))
        rows += ProjectionLine(
            label = "Divided by hours",
            value = formatHours(projection.totalHours),
            subLabel = if (listing.assessmentHours > 0) {
                "${formatHours(listing.estHours)} of work + " +
                    "${formatHours(listing.assessmentHours)} unpaid assessment"
            } else null
        )
        rows += ProjectionLine(
            label = "Projected effective",
            value = formatKes(projection.projectedKesPerHour),
            isTotal = true
        )

        return rows
    }

    fun project(
        listing: ListingEntity,
        platform: PlatformEntity?,
        usualRoute: WithdrawalRouteEntity?,
        rates: Map<Currency, BigDecimal>,
        approvalRate: Double?
    ): ListingProjection {
        val midRate = Pipeline.rateFor(listing.currency, rates)
        val commission = platform?.commissionPct ?: 0.0
        val spread = usualRoute?.spreadPct ?: 0.0

        val flatFeeKes = usualRoute?.let {
            Money.fromMinor(it.flatFeeMinor).multiply(Pipeline.rateFor(it.feeCurrency, rates))
        } ?: BigDecimal.ZERO

        val net = Money.fromMinor(listing.statedPayMinor)
            .multiply(Money.remainderOf(commission))
            .multiply(Money.remainderOf(spread))
            .multiply(midRate)
            .subtract(flatFeeKes)

        val hours = listing.estHours + listing.assessmentHours
        val perHour = if (hours == 0.0) BigDecimal.ZERO else
            net.divide(BigDecimal.valueOf(hours), MathContext.DECIMAL64)

        return ListingProjection(
            listing = listing,
            netKes = Money.toKes(net),
            projectedKesPerHour = Money.toKes(perHour),
            riskAdjustedKesPerHour = approvalRate?.let {
                Money.toKes(perHour.multiply(BigDecimal.valueOf(it)))
            },
            totalHours = hours
        )
    }
}
