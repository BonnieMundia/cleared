package app.cleared.data.derive

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.model.Money
import app.cleared.data.model.Stage
import java.math.BigDecimal
import java.math.MathContext

/**
 * One platform's numbers, as shown on frame `1b`.
 *
 * [hoursTotal] includes [hoursUnpaid]; the card renders the unpaid share as a portion of the bar
 * ("268 h logged" / "6 h unpaid (2%)"), not as an addition to it.
 */
data class PlatformStats(
    val platform: PlatformEntity,
    val totalPaidKes: Long,
    val hoursTotal: Double,
    val hoursUnpaid: Double,
    val effectiveKesPerHour: Long,
    val landedCount: Int,
    val rejectedCount: Int,
    val reversedCount: Int,
    val recordCount: Int
) {
    /** landedCount ÷ (landedCount + rejectedCount). Reversals are in neither term. */
    val approvalRate: Double?
        get() {
            val denominator = landedCount + rejectedCount
            return if (denominator == 0) null else landedCount.toDouble() / denominator
        }

    val approvalPct: Int? get() = approvalRate?.let { Math.round(it * 100).toInt() }

    val unpaidSharePct: Int
        get() = if (hoursTotal == 0.0) 0 else Math.round(hoursUnpaid / hoursTotal * 100).toInt()
}

object PlatformStatistics {

    /**
     *     effectiveRate(platform) =
     *           Σ finalKesCleared over ALL landed records for that platform
     *         ─────────────────────────────────────────────────────────────
     *           Σ (hoursWorked + hoursUnpaid) over ALL records for that platform,
     *             INCLUDING rejected ones
     *
     * Rejected work stays in the denominator and contributes nothing to the numerator. A reversed
     * payout behaves identically — only LANDED money counts. Its re-issue carries no hours, so when
     * that lands the money is counted once and the hours are not counted twice.
     */
    fun of(platform: PlatformEntity, states: List<RecordState>): PlatformStats {
        val mine = states.filter { it.record.platformId == platform.id }

        val cleared = mine.fold(BigDecimal.ZERO) { acc, state -> acc + Ledger.finalKesCleared(state.detail) }
        val hours = mine.sumOf { it.billableHours() }
        val unpaid = mine.sumOf { it.unpaidHours() }

        val rate = if (hours == 0.0) 0L else Money.toKes(
            cleared.divide(BigDecimal.valueOf(hours), MathContext.DECIMAL64)
        )

        return PlatformStats(
            platform = platform,
            totalPaidKes = Money.toKes(cleared),
            hoursTotal = hours,
            hoursUnpaid = unpaid,
            effectiveKesPerHour = rate,
            landedCount = mine.count { it.displayStage == Stage.LANDED },
            rejectedCount = mine.count { it.displayStage == Stage.REJECTED },
            reversedCount = mine.count { it.displayStage == Stage.REVERSED },
            recordCount = mine.size
        )
    }

    fun all(platforms: List<PlatformEntity>, states: List<RecordState>): List<PlatformStats> =
        platforms.map { of(it, states) }

    /** The median effective rate across platforms — the yardstick every card compares against. */
    fun medianRate(stats: List<PlatformStats>): Long {
        val rates = stats.map { it.effectiveKesPerHour }.sorted()
        if (rates.isEmpty()) return 0
        val mid = rates.size / 2
        return if (rates.size % 2 == 1) rates[mid] else (rates[mid - 1] + rates[mid]) / 2
    }

    /** Flag a platform as poor when its effective rate is under 0.6 × the median across platforms. */
    fun isPoor(stat: PlatformStats, medianRate: Long): Boolean =
        stat.effectiveKesPerHour < BigDecimal.valueOf(medianRate)
            .multiply(BigDecimal("0.6"))
            .toDouble()
}
