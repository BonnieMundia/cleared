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

object Discovery {

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
