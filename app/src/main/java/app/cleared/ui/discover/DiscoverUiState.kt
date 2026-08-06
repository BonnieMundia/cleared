package app.cleared.ui.discover

import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.derive.Discovery
import app.cleared.data.derive.ListingProjection
import app.cleared.data.derive.PlatformStatistics
import app.cleared.data.derive.PlatformStats
import app.cleared.data.derive.ProjectionLine
import app.cleared.data.derive.RecordState
import app.cleared.data.discovery.ScanResult
import app.cleared.data.model.Currency
import app.cleared.data.model.PayoutDestination
import app.cleared.data.model.WalletProvider
import app.cleared.ui.format.DateFormat
import app.cleared.ui.format.MoneyFormat
import java.math.BigDecimal
import java.math.MathContext
import java.time.Duration
import java.time.Instant

enum class DiscoverFilter(val label: String) {
    All("All"),
    AboveMedian("Above my median"),
    NoAssessment("No assessment"),
    Writing("Writing")
}

data class ListingRowUi(
    val id: Long,
    val title: String,
    val subLine: String,
    val rate: String,
    val vsMedian: String,
    val isBelowMedian: Boolean,
    val pays: String,
    val hours: String,
    val hasAssessment: Boolean,
    val then: String,
    val adjusted: String,
    val warning: String?,
    /** False when nobody has said how long this would take, so there is no rate to show. */
    val isPriced: Boolean = true
)

data class DiscoverUiState(
    val bestRate: String = "—",
    val bestCaption: String = "",
    val scanCaption: String = "",
    val filter: DiscoverFilter = DiscoverFilter.All,
    val listings: List<ListingRowUi> = emptyList(),
    val loading: Boolean = true
)

data class ListingDetailUiState(
    val listingId: Long = 0,
    val title: String = "",
    val subLine: String = "",
    val rate: String = "",
    val comparison: String = "",
    val isBelowMedian: Boolean = false,
    val isPriced: Boolean = false,
    /** The hours the user has set, or zero while unestimated. */
    val estHours: Double = 0.0,
    val assessmentHours: Double = 0.0,
    val hoursEstimatedByUser: Boolean = false,
    val breakdown: List<ProjectionLine> = emptyList(),
    val riskNote: String = "",
    val platformName: String = "",
    val platformStats: List<Pair<String, String>> = emptyList(),
    val platformStatsNote: String = "",
    val platformId: Long? = null,
    val prospectNote: String = "",
    val loading: Boolean = true
)

/**
 * Frames `3a` and `3b`.
 *
 * Both use the **net** projection from design/DATA_MODEL.md — stated pay less commission, less the
 * cost of the route the money would actually take. design/sample_data.json's own
 * `projectedKesPerHour` figures are the *gross* rate, stated pay converted at mid over total hours,
 * which is why `3a` shows 1,651 for the same Halo listing that `3b` works out at 1,520.
 *
 * One number, computed once, and it is the smaller one. A projection that ignores the commission
 * and the withdrawal fee flatters every listing on the screen, and this screen exists to stop the
 * user taking bad work.
 */
object DiscoverMapper {

    fun build(
        scan: ScanResult,
        platforms: List<PlatformEntity>,
        routes: List<WithdrawalRouteEntity>,
        states: List<RecordState>,
        rates: Map<Currency, BigDecimal>,
        filter: DiscoverFilter,
        now: Instant
    ): DiscoverUiState {
        val stats = PlatformStatistics.all(platforms, states)
        val median = PlatformStatistics.medianRate(stats)

        // Priced listings rank by rate; unpriced ones sort after them rather than below the worst
        // job on the board, because "we do not know" is not the same as "nearly nothing".
        val projected = scan.listings
            .map { it to project(it, platforms, routes, stats, rates) }
            .sortedWith(
                compareByDescending<Pair<ListingEntity, ListingProjection>> { it.second.isPriced }
                    .thenByDescending { it.second.projectedKesPerHour ?: Long.MIN_VALUE }
            )

        val visible = projected.filter { (listing, projection) ->
            when (filter) {
                DiscoverFilter.All -> true
                // An unpriced listing cannot be claimed to beat the median.
                DiscoverFilter.AboveMedian ->
                    projection.projectedKesPerHour?.let { it > median } == true
                DiscoverFilter.NoAssessment -> (listing.assessmentHours ?: 0.0) == 0.0
                DiscoverFilter.Writing -> listing.kind.equals("Writing", ignoreCase = true)
            }
        }

        val best = projected.firstOrNull { it.second.isPriced }?.second
        val multiple = if (median > 0 && best?.projectedKesPerHour != null) {
            BigDecimal.valueOf(best.projectedKesPerHour!!)
                .divide(BigDecimal.valueOf(median), MathContext.DECIMAL64)
        } else null

        return DiscoverUiState(
            bestRate = best?.projectedKesPerHour?.let { MoneyFormat.kes(it) } ?: "—",
            bestCaption = multiple?.let {
                val times = it.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()
                "$times× your median of ${MoneyFormat.kes(median)}/h"
            } ?: "",
            scanCaption = "Scanned ${scan.boardCount} platform boards and ${scan.feedCount} " +
                "community feeds at ${DateFormat.time(scan.scannedAt)} · " +
                "${visible.size} of ${projected.size} shown",
            filter = filter,
            listings = visible.map { (listing, projection) ->
                row(listing, projection, platforms, stats, median, now)
            },
            loading = false
        )
    }

    fun detail(
        listingId: Long,
        scan: ScanResult,
        platforms: List<PlatformEntity>,
        routes: List<WithdrawalRouteEntity>,
        states: List<RecordState>,
        rates: Map<Currency, BigDecimal>
    ): ListingDetailUiState {
        val listing = scan.listings.firstOrNull { it.id == listingId }
            ?: return ListingDetailUiState(loading = false)
        val stats = PlatformStatistics.all(platforms, states)
        val median = PlatformStatistics.medianRate(stats)
        val platform = platforms.firstOrNull { it.name == listing.platformName }
        val stat = stats.firstOrNull { it.platform.id == platform?.id }
        val route = usualRoute(platform, routes)
        val projection = project(listing, platforms, routes, stats, rates)

        val settle = platform?.let { p ->
            app.cleared.data.derive.SettleTime.of(p.id, states, Instant.now())
        }

        return ListingDetailUiState(
            listingId = listing.id,
            title = listing.title,
            subLine = "${listing.platformName} · ${listing.kind} · " +
                "${listing.sourceLabel} · ${relative(listing.seenAt, Instant.now())}",
            rate = projection.projectedKesPerHour?.let { MoneyFormat.kes(it) } ?: "Not priced yet",
            comparison = projection.projectedKesPerHour?.let { comparison(it, median) }
                ?: "Set the hours below and this prices itself.",
            isBelowMedian = projection.projectedKesPerHour?.let { it < median } == true,
            isPriced = projection.isPriced,
            estHours = listing.estHours ?: 0.0,
            assessmentHours = listing.assessmentHours ?: 0.0,
            hoursEstimatedByUser = listing.hoursEstimatedByUser,
            breakdown = Discovery.breakdown(
                listing = listing,
                platform = platform,
                usualRoute = route,
                rates = rates,
                projection = projection,
                formatMoney = { currency, amount -> MoneyFormat.format(currency, amount) },
                formatKes = { MoneyFormat.kes(it) },
                formatHours = { MoneyFormat.hours(it) },
                formatPercent = { MoneyFormat.percent(it) }
            ),
            riskNote = riskNote(listing, stat, projection),
            platformName = listing.platformName,
            platformId = platform?.id,
            platformStats = buildList {
                stat?.approvalPct?.let { add("Approval" to "$it%") }
                settle?.p50Days?.let { add("Median to land" to "$it d") }
                settle?.p90Days?.let { add("p90" to "$it d") }
            },
            platformStatsNote = if (stat == null || stat.recordCount == 0) {
                "You have never worked with this platform, so none of this is your data."
            } else {
                "From your own ${stat.recordCount} records, not from the listing."
            },
            prospectNote = "Tracking starts the clock on unpaid hours. If the assessment takes " +
                "twice as long as advertised, you will see it in ${listing.platformName}'s " +
                "effective rate whether or not this ever becomes a submission.",
            loading = false
        )
    }

    /** The route the money would actually take, given where this platform pays. */
    private fun usualRoute(
        platform: PlatformEntity?,
        routes: List<WithdrawalRouteEntity>
    ): WithdrawalRouteEntity? {
        val provider = when (platform?.payoutDestination) {
            PayoutDestination.PAYPAL -> WalletProvider.PAYPAL
            PayoutDestination.PAYONEER -> WalletProvider.PAYONEER
            null -> null
        }
        val candidates = if (provider == null) routes else routes.filter { it.provider == provider }
        // The cheapest of them, since that is the one a rational user would pick.
        return candidates.minByOrNull { it.spreadPct }
    }

    private fun project(
        listing: ListingEntity,
        platforms: List<PlatformEntity>,
        routes: List<WithdrawalRouteEntity>,
        stats: List<PlatformStats>,
        rates: Map<Currency, BigDecimal>
    ): ListingProjection {
        val platform = platforms.firstOrNull { it.name == listing.platformName }
        val stat = stats.firstOrNull { it.platform.id == platform?.id }
        return Discovery.project(
            listing = listing,
            platform = platform,
            usualRoute = usualRoute(platform, routes),
            rates = rates,
            approvalRate = stat?.approvalRate
        )
    }

    private fun row(
        listing: ListingEntity,
        projection: ListingProjection,
        platforms: List<PlatformEntity>,
        stats: List<PlatformStats>,
        median: Long,
        now: Instant
    ): ListingRowUi {
        val platform = platforms.firstOrNull { it.name == listing.platformName }
        val stat = stats.firstOrNull { it.platform.id == platform?.id }
        val rate = projection.projectedKesPerHour
        val below = rate != null && rate < median
        val assessment = listing.assessmentHours ?: 0.0

        return ListingRowUi(
            id = listing.id,
            title = listing.title,
            subLine = "${listing.platformName} · ${listing.kind} · ${listing.sourceLabel} · " +
                relative(listing.seenAt, now),
            rate = rate?.let { MoneyFormat.kes(it) } ?: "Not priced yet",
            vsMedian = if (rate == null) "tap to estimate the hours" else comparison(rate, median),
            isBelowMedian = below,
            pays = MoneyFormat.formatMinor(listing.currency, listing.statedPayMinor),
            hours = when {
                listing.estHours == null -> "not stated — you decide"
                assessment > 0 ->
                    "${MoneyFormat.hours(listing.estHours!!)} est + " +
                        "${MoneyFormat.hours(assessment)} unpaid assessment"
                else -> "${MoneyFormat.hours(listing.estHours!!)} est"
            },
            hasAssessment = assessment > 0,
            isPriced = projection.isPriced,
            then = stat?.let { s ->
                s.approvalPct?.let { "$it% approved" } ?: "no approval history"
            } ?: "no history with this platform",
            adjusted = projection.riskAdjustedKesPerHour?.let { adjusted ->
                "${MoneyFormat.kes(adjusted)} risk-adjusted at ${stat?.approvalPct ?: 0}%"
            } ?: "no approval history",
            warning = if (below) warning(listing, stat) else null
        )
    }

    /**
     * A listing on a platform that rejects 39% of submissions is shown at 61% of its headline
     * rate. That is the reason this feature lives inside this app rather than in a browser tab.
     */
    private fun riskNote(
        listing: ListingEntity,
        stat: PlatformStats?,
        projection: ListingProjection
    ): String {
        if (!projection.isPriced) {
            return "No board states how long a job will take. Set the hours and this prices " +
                "itself, risk-adjusted by ${listing.platformName}'s own approval rate."
        }
        val adjusted = projection.riskAdjustedKesPerHour
            ?: return "You have no approval history with ${listing.platformName}, so there is " +
                "nothing to risk-adjust this against."
        return "At ${listing.platformName}'s ${stat?.approvalPct ?: 0}% approval rate the " +
            "risk-adjusted figure is ${MoneyFormat.kes(adjusted)}/h."
    }

    private fun warning(listing: ListingEntity, stat: PlatformStats?): String {
        val note = listing.note
        if (!note.isNullOrBlank()) return note
        val approval = stat?.approvalPct ?: return "Below your median rate."
        return "Below your median rate, and $approval% of what you submit here is approved."
    }

    private fun comparison(rate: Long, median: Long): String {
        if (median <= 0) return ""
        val delta = (rate - median).toDouble() / median * 100
        val sign = if (delta >= 0) "+" else MoneyFormat.MINUS
        return "$sign${MoneyFormat.percent(kotlin.math.abs(delta), decimals = 0)} vs your median"
    }

    private fun relative(at: Instant, now: Instant): String {
        val gap = Duration.between(at, now)
        return when {
            gap.toMinutes() < 60 -> "${gap.toMinutes().coerceAtLeast(1)} min ago"
            gap.toHours() < 24 -> "${gap.toHours()} h ago"
            else -> "${gap.toDays()} d ago"
        }
    }
}
