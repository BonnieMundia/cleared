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
    val recordCount: Int,
    /**
     * Work hours per one unit of this platform's pay currency, from history. Null until there is
     * any. Drives the hours a Discovery listing is estimated at — see [PlatformStatistics.hoursModel].
     */
    val hoursPerPayUnit: Double? = null,
    /**
     * What an unpaid assessment costs here, typically. A platform trait rather than a job trait:
     * Halo's calibration set is the same two hours whether the batch is large or small.
     */
    val typicalAssessmentHours: Double = 0.0,
    /** How many records the hours model was built from, so the UI can say. */
    val hoursSampleCount: Int = 0
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

        val model = hoursModel(platform, mine)

        return PlatformStats(
            platform = platform,
            totalPaidKes = Money.toKes(cleared),
            hoursTotal = hours,
            hoursUnpaid = unpaid,
            effectiveKesPerHour = rate,
            landedCount = mine.count { it.displayStage == Stage.LANDED },
            rejectedCount = mine.count { it.displayStage == Stage.REJECTED },
            reversedCount = mine.count { it.displayStage == Stage.REVERSED },
            recordCount = mine.size,
            hoursPerPayUnit = model.hoursPerPayUnit,
            typicalAssessmentHours = model.typicalAssessmentHours,
            hoursSampleCount = model.sampleCount
        )
    }

    private data class HoursModel(
        val hoursPerPayUnit: Double?,
        val typicalAssessmentHours: Double,
        val sampleCount: Int
    )

    /**
     * How long work on this platform takes, per unit of what it pays.
     *
     * Deliberately **not** derived from the effective rate. Estimating a listing's hours as
     * `netKes / effectiveRate` would make its projected rate come back as the platform's historical
     * rate every time, for every listing — an answer that cannot tell one job from another. This
     * divides hours by *stated pay* instead, which is a different quantity, and keeps the
     * assessment separate so it stays a fixed cost.
     *
     * That separation is what makes the estimate informative: a fixed two-hour calibration set
     * ruins a small job and barely dents a large one, and the projection now says so.
     *
     * Prospects are excluded — they carry pay with no work logged yet, and would drag the estimate
     * toward zero hours for every job on the platform.
     */
    private fun hoursModel(platform: PlatformEntity, records: List<RecordState>): HoursModel {
        val priced = records.filter {
            it.record.carriesHours &&
                it.record.currency == platform.payCurrency &&
                it.displayStage != Stage.PROSPECT &&
                it.record.hoursWorked > 0.0 &&
                it.record.grossMinor > 0
        }

        val grossUnits = priced.sumOf { Money.fromMinor(it.record.grossMinor).toDouble() }
        val workHours = priced.sumOf { it.record.hoursWorked }

        val withAssessment = records.filter { it.record.carriesHours && it.record.hoursUnpaid > 0.0 }

        return HoursModel(
            hoursPerPayUnit = if (grossUnits > 0.0 && workHours > 0.0) workHours / grossUnits else null,
            // The median, not the mean: one onboarding marathon should not become the expectation
            // for every later assessment.
            typicalAssessmentHours = withAssessment
                .map { it.record.hoursUnpaid }
                .sorted()
                .let { if (it.isEmpty()) 0.0 else it[it.size / 2] },
            sampleCount = priced.size
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
