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
/** Where a listing's hours came from, which decides how much the rate above them can be trusted. */
enum class HoursSource {
    /** The listing itself stated them. Rare. */
    Stated,

    /** The user said. The best number available. */
    User,

    /** Estimated from what work on this platform has taken before. */
    PlatformHistory,

    /** Nobody knows and there is no history to guess from. */
    Unknown
}

/**
 * What a listing would pay. [projectedKesPerHour] is null only when the hours are unknown *and*
 * there is no history to estimate them from — see [Discovery.project].
 */
data class ListingProjection(
    val listing: ListingEntity,
    /** What it lands as in KES. Known even without hours, because it does not divide by them. */
    val netKes: Long,
    val projectedKesPerHour: Long?,
    val riskAdjustedKesPerHour: Long?,
    val totalHours: Double?,
    val hoursSource: HoursSource = HoursSource.Unknown,
    /** Records the estimate was built from, when [hoursSource] is [HoursSource.PlatformHistory]. */
    val hoursSampleCount: Int = 0
) {
    val isPriced: Boolean get() = projectedKesPerHour != null

    /** True when the rate rests on an estimate rather than on a stated or confirmed hour count. */
    val isEstimated: Boolean get() = hoursSource == HoursSource.PlatformHistory
}

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

        // Without hours the table stops here and says so, rather than showing a division it cannot
        // do. The lines above are all still true — only the last two depend on the estimate.
        val hours = projection.totalHours
        if (hours == null) {
            rows += ProjectionLine(
                label = "Divided by hours",
                value = "—",
                subLabel = "no estimate yet"
            )
            return rows
        }

        rows += ProjectionLine(
            label = "Divided by hours",
            value = formatHours(hours),
            subLabel = hoursProvenance(listing, projection, formatHours)
        )
        rows += ProjectionLine(
            label = if (projection.isEstimated) "Projected effective, estimated"
            else "Projected effective",
            value = projection.projectedKesPerHour?.let(formatKes) ?: "—",
            isTotal = true
        )

        return rows
    }

    /**
     * Says where the divisor came from. An estimate and a stated figure produce the same number and
     * deserve very different confidence, so the line that carries them says which it is.
     */
    private fun hoursProvenance(
        listing: ListingEntity,
        projection: ListingProjection,
        formatHours: (Double) -> String
    ): String? {
        val assessment = listing.assessmentHours
        return when (projection.hoursSource) {
            HoursSource.PlatformHistory ->
                "estimated from ${projection.hoursSampleCount} records on this platform"

            HoursSource.User, HoursSource.Stated ->
                if (assessment != null && assessment > 0) {
                    "${formatHours(listing.estHours ?: 0.0)} of work + " +
                        "${formatHours(assessment)} unpaid assessment"
                } else null

            HoursSource.Unknown -> null
        }
    }

    /**
     * @param stats the platform's own statistics, used to estimate hours the listing does not state
     *        and the user has not supplied. Null, or a platform with no history, leaves the listing
     *        unpriced rather than guessed at.
     */
    fun project(
        listing: ListingEntity,
        platform: PlatformEntity?,
        usualRoute: WithdrawalRouteEntity?,
        rates: Map<Currency, BigDecimal>,
        approvalRate: Double?,
        stats: PlatformStats? = null
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

        val (hours, source, sample) = hoursFor(listing, stats)

        // Unknown hours and no history to estimate from: no rate. Dividing by an assumed count
        // would put a confident figure on the card, and an unknown flooring to zero would make the
        // listing read as the worst work available rather than as the unpriced one.
        if (hours == null || hours <= 0.0) {
            return ListingProjection(
                listing = listing,
                netKes = Money.toKes(net),
                projectedKesPerHour = null,
                riskAdjustedKesPerHour = null,
                totalHours = null,
                hoursSource = HoursSource.Unknown
            )
        }

        val perHour = net.divide(BigDecimal.valueOf(hours), MathContext.DECIMAL64)

        return ListingProjection(
            listing = listing,
            netKes = Money.toKes(net),
            projectedKesPerHour = Money.toKes(perHour),
            riskAdjustedKesPerHour = approvalRate?.let {
                Money.toKes(perHour.multiply(BigDecimal.valueOf(it)))
            },
            totalHours = hours,
            hoursSource = source,
            hoursSampleCount = sample
        )
    }

    /**
     * The hours to divide by, and where they came from.
     *
     * Preference order is stated, then the user's own estimate, then the platform's history. The
     * work component scales with what the listing pays; the assessment does not, because it is a
     * fixed toll for turning up.
     */
    private fun hoursFor(
        listing: ListingEntity,
        stats: PlatformStats?
    ): Triple<Double?, HoursSource, Int> {
        listing.totalHours?.let { stated ->
            if (stated > 0.0) {
                val source = if (listing.hoursEstimatedByUser) HoursSource.User else HoursSource.Stated
                return Triple(stated, source, 0)
            }
        }

        val perUnit = stats?.hoursPerPayUnit ?: return Triple(null, HoursSource.Unknown, 0)
        val pay = Money.fromMinor(listing.statedPayMinor).toDouble()
        val work = pay * perUnit
        val assessment = listing.assessmentHours ?: stats.typicalAssessmentHours

        val total = work + assessment
        if (total <= 0.0) return Triple(null, HoursSource.Unknown, 0)
        return Triple(total, HoursSource.PlatformHistory, stats.hoursSampleCount)
    }
}
