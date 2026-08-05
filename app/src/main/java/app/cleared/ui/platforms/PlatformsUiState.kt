package app.cleared.ui.platforms

import app.cleared.data.derive.PlatformStatistics
import app.cleared.data.derive.PlatformStats
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.SettleTime
import app.cleared.data.model.PlatformKind
import app.cleared.ui.format.MoneyFormat
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToInt

/** The four sorts from frame `1b`. Effective rate, descending, is the default. */
enum class PlatformSort(val label: String) {
    EffectiveRate("Effective rate"),
    TotalPaid("Total paid"),
    Approval("Approval"),
    DaysToLand("Days to land")
}

data class PlatformCardUi(
    val id: Long,
    val rank: String,
    val name: String,
    val subLine: String,
    val effectiveRate: String,
    val vsMedian: String,
    val isPoor: Boolean,
    val hoursLogged: String,
    val hoursUnpaid: String,
    /** The paid share of the hours bar. The track behind it is the unpaid remainder. */
    val paidFraction: Float,
    val hasUnpaidHours: Boolean,
    val approval: String,
    val daysToLand: String,
    val totalPaid: String,
    val reversedCount: Int,
    val warning: String?
)

data class PlatformsUiState(
    val cards: List<PlatformCardUi> = emptyList(),
    val sort: PlatformSort = PlatformSort.EffectiveRate,
    val medianRate: Long = 0,
    val loading: Boolean = true
)

/**
 * Frame `1b` — make a bad platform obvious at a glance.
 *
 * The headline is effective KES per hour: everything a platform has ever paid, divided by every
 * hour it was ever given, including the assessments and onboarding it never paid for. Nothing here
 * filters rejected work out, and that asymmetry is the whole point of the screen.
 */
object PlatformsMapper {

    fun build(
        states: List<RecordState>,
        platforms: List<app.cleared.data.db.entity.PlatformEntity>,
        sort: PlatformSort,
        now: Instant
    ): PlatformsUiState {
        val stats = PlatformStatistics.all(platforms, states)
        val median = PlatformStatistics.medianRate(stats)

        val sorted = when (sort) {
            PlatformSort.EffectiveRate -> stats.sortedByDescending { it.effectiveKesPerHour }
            PlatformSort.TotalPaid -> stats.sortedByDescending { it.totalPaidKes }
            PlatformSort.Approval -> stats.sortedByDescending { it.approvalRate ?: -1.0 }
            // Fewest days first: waiting less is better, so this one ascends.
            PlatformSort.DaysToLand -> stats.sortedBy {
                SettleTime.of(it.platform.id, states, now).p50Days ?: Int.MAX_VALUE
            }
        }

        return PlatformsUiState(
            cards = sorted.mapIndexed { index, stat ->
                card(stat, index + 1, median, SettleTime.of(stat.platform.id, states, now).p50Days)
            },
            sort = sort,
            medianRate = median,
            loading = false
        )
    }

    private fun card(stat: PlatformStats, rank: Int, median: Long, p50Days: Int?): PlatformCardUi {
        val poor = PlatformStatistics.isPoor(stat, median)
        val delta = stat.effectiveKesPerHour - median
        val unpaidPct = stat.unpaidSharePct

        return PlatformCardUi(
            id = stat.platform.id,
            rank = rank.toString().padStart(2, '0'),
            name = stat.platform.name,
            subLine = "${kindLabel(stat.platform.kind)} · pays in ${stat.platform.payCurrency.name}",
            effectiveRate = MoneyFormat.kes(stat.effectiveKesPerHour),
            vsMedian = when {
                delta == 0L -> "at the median"
                delta > 0 -> "+${MoneyFormat.digits(app.cleared.data.model.Currency.KES, java.math.BigDecimal.valueOf(delta))} vs median"
                else -> "${MoneyFormat.MINUS}${MoneyFormat.digits(app.cleared.data.model.Currency.KES, java.math.BigDecimal.valueOf(abs(delta)))} vs median"
            },
            isPoor = poor,
            hoursLogged = "${MoneyFormat.hours(stat.hoursTotal)} logged",
            hoursUnpaid = "${MoneyFormat.hours(stat.hoursUnpaid)} unpaid ($unpaidPct%)",
            paidFraction = if (stat.hoursTotal == 0.0) 1f
            else ((stat.hoursTotal - stat.hoursUnpaid) / stat.hoursTotal).toFloat().coerceIn(0f, 1f),
            hasUnpaidHours = stat.hoursUnpaid > 0.0,
            approval = stat.approvalPct?.let { "$it% approved" } ?: "no history",
            daysToLand = p50Days?.let { "$it d to land" } ?: "—",
            totalPaid = MoneyFormat.kes(stat.totalPaidKes),
            // A platform that bounces payouts is a different problem from one that rejects work.
            // Conflating them hides both, so the count is carried separately.
            reversedCount = stat.reversedCount,
            warning = if (poor) warning(stat) else null
        )
    }

    /** `Lowest rate · 32% of your hours here were never paid`. */
    private fun warning(stat: PlatformStats): String {
        val pct = if (stat.hoursTotal == 0.0) 0
        else (stat.hoursUnpaid / stat.hoursTotal * 100).roundToInt()
        return "Lowest rate · $pct% of your hours here were never paid"
    }

    /**
     * design/SCREENS.md gives one of these — `AI training tasks` — and leaves the other three to
     * be written. Flagged for the designer.
     */
    private fun kindLabel(kind: PlatformKind) = when (kind) {
        PlatformKind.AI_TRAINING -> "AI training tasks"
        PlatformKind.WRITING -> "Writing work"
        PlatformKind.MARKETPLACE -> "Marketplace work"
        PlatformKind.OWN_COMPANY -> "Your own company"
    }
}
