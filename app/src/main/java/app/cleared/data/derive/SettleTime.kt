package app.cleared.data.derive

import app.cleared.data.model.Stage
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

/**
 * Per-platform settle-time distribution — frame `2b`.
 *
 * Over records that reached LANDED, `landedAt − submittedAt`, then p50, p90, and the 90-day drift.
 * Also the mean dwell time per stage, which is what shows that nearly half the wait sits in
 * IN_REVIEW — a work-phase problem, not a payment problem.
 */
data class SettleTimeStats(
    val platformId: Long,
    val sampleDays: List<Long>,
    val p50Days: Int?,
    val p90Days: Int?,
    val meanDays: Double?,
    val driftDays90: Int?,
    val stageDwellDays: Map<Stage, Double>
) {
    val sampleCount: Int get() = sampleDays.size
}

/** One bar of the settle-time histogram on frame `2b`. */
data class SettleBucket(
    val fromDays: Int,
    val toDays: Int,
    val count: Int,
    /** True for the long tail past p90 — the bars the design renders amber. */
    val isTail: Boolean,
    val isOverflow: Boolean
) {
    val label: String get() = if (isOverflow) "${fromDays}d+" else "${fromDays}d"
}

object SettleTime {

    /** Twelve bars, as the design draws them. */
    const val HISTOGRAM_BARS = 12

    /**
     * Buckets the landed sample into [HISTOGRAM_BARS] bars.
     *
     * The axis runs to about 1.5 × p90 and everything beyond lands in a single overflow bar, so the
     * one record that took four months does not flatten the eleven that took a fortnight. That last
     * bar is the point of the screen as much as the shape of the rest: it is where the records that
     * flagged overdue actually live.
     */
    fun histogram(sortedDays: List<Long>, p90Days: Int?): List<SettleBucket> {
        if (sortedDays.isEmpty()) return emptyList()
        val p90 = p90Days ?: sortedDays.last().toInt()
        val bars = HISTOGRAM_BARS - 1
        val ceiling = maxOf(bars, kotlin.math.ceil(p90 * 1.5).toInt())
        val width = maxOf(1, kotlin.math.ceil(ceiling.toDouble() / bars).toInt())

        return (0 until HISTOGRAM_BARS).map { index ->
            val overflow = index == HISTOGRAM_BARS - 1
            val from = index * width
            val to = if (overflow) Int.MAX_VALUE else from + width
            SettleBucket(
                fromDays = from,
                toDays = if (overflow) from else to,
                count = sortedDays.count { it >= from && it < to },
                isTail = from >= p90,
                isOverflow = overflow
            )
        }
    }

    /**
     * Nearest-rank percentile on the sorted sample: the smallest value at or above the rank
     * `ceil(p × n)`. No interpolation — these are whole days and the figure is a threshold a record
     * is compared against, so a real observation is the honest answer.
     */
    fun percentile(sortedDays: List<Long>, p: Double): Int? {
        if (sortedDays.isEmpty()) return null
        val rank = ceil(p * sortedDays.size).toInt().coerceIn(1, sortedDays.size)
        return sortedDays[rank - 1].toInt()
    }

    fun of(platformId: Long, states: List<RecordState>, now: Instant): SettleTimeStats {
        val mine = states.filter { it.record.platformId == platformId }
        val landed = mine.filter { it.displayStage == Stage.LANDED }

        val days = landed.mapNotNull { endToEndDays(it) }.sorted()
        val recent = landed.filter { state ->
            landedAt(state)?.isAfter(now.minus(Duration.ofDays(90))) == true
        }.mapNotNull { endToEndDays(it) }.sorted()
        val older = landed.filter { state ->
            landedAt(state)?.isBefore(now.minus(Duration.ofDays(90))) == true
        }.mapNotNull { endToEndDays(it) }.sorted()

        val p50Now = percentile(recent, 0.50)
        val p50Then = percentile(older, 0.50)

        return SettleTimeStats(
            platformId = platformId,
            sampleDays = days,
            p50Days = percentile(days, 0.50),
            p90Days = percentile(days, 0.90),
            meanDays = if (days.isEmpty()) null else days.average(),
            driftDays90 = if (p50Now != null && p50Then != null) p50Now - p50Then else null,
            stageDwellDays = dwellByStage(mine)
        )
    }

    /** p90 per platform, recomputed from scratch — call it whenever a record lands. */
    fun p90ByPlatform(platformIds: Collection<Long>, states: List<RecordState>, now: Instant): Map<Long, Int> =
        platformIds.mapNotNull { id -> of(id, states, now).p90Days?.let { id to it } }.toMap()

    private fun submittedAt(state: RecordState): Instant? =
        StageResolver.recordEvents(state.detail)
            .filter { it.stage == Stage.SUBMITTED }
            .minByOrNull { it.occurredAt }?.occurredAt

    private fun landedAt(state: RecordState): Instant? =
        state.detail.events.filter { it.stage == Stage.LANDED }.maxByOrNull { it.occurredAt }?.occurredAt

    fun endToEndDays(state: RecordState): Long? {
        val from = submittedAt(state) ?: return null
        val to = landedAt(state) ?: return null
        return CalendarDays.between(from, to)
    }

    /** Mean days spent in each stage, over every record that left that stage. */
    private fun dwellByStage(states: List<RecordState>): Map<Stage, Double> {
        val totals = mutableMapOf<Stage, MutableList<Long>>()
        for (state in states) {
            val events = StageResolver.recordEvents(state.detail).sortedBy { it.occurredAt }
            events.zipWithNext { a, b ->
                totals.getOrPut(a.stage) { mutableListOf() }
                    .add(Duration.between(a.occurredAt, b.occurredAt).toHours())
            }
        }
        return totals.mapValues { (_, hours) -> hours.average() / 24.0 }
    }
}
