package app.cleared.ui.settletime

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.derive.Pipeline
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.SettleBucket
import app.cleared.data.derive.SettleTime
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import app.cleared.ui.components.stageLabel
import app.cleared.ui.format.MoneyFormat
import java.time.Instant

data class StageDwellUi(
    val stage: Stage,
    val days: Double,
    val fraction: Float,
    val label: String
) {
    val phase: Phase get() = stage.phase
}

data class SettleTimeUiState(
    val platformName: String = "",
    val medianDays: String = "—",
    val sampleCaption: String = "",
    val buckets: List<SettleBucket> = emptyList(),
    val maxBucketCount: Int = 1,
    val axisLabels: List<String> = emptyList(),
    val p50: String = "—",
    val p90: String = "—",
    val drift: String = "—",
    val meanNote: String = "",
    val dwell: List<StageDwellUi> = emptyList(),
    val dwellNote: String = "",
    val overdueCount: Int = 0,
    val loading: Boolean = true
)

/**
 * Frame `2b` — why overdue is p90 and not a mean, shown rather than argued.
 */
object SettleTimeMapper {

    fun build(
        platform: PlatformEntity,
        states: List<RecordState>,
        now: Instant
    ): SettleTimeUiState {
        val stats = SettleTime.of(platform.id, states, now)
        val buckets = SettleTime.histogram(stats.sampleDays, stats.p90Days)
        val dwell = dwell(stats.stageDwellDays)

        val p90 = stats.p90Days
        val overdue = states.count { state ->
            state.record.platformId == platform.id &&
                Pipeline.isOverdue(
                    state,
                    now,
                    p90?.let { mapOf(platform.id to it) } ?: emptyMap(),
                    mapOf(platform.id to platform.graceDays)
                )
        }

        return SettleTimeUiState(
            platformName = platform.name,
            medianDays = stats.p50Days?.let { "$it d" } ?: "—",
            sampleCaption = "median · ${stats.sampleCount} records",
            buckets = buckets,
            maxBucketCount = buckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1,
            axisLabels = axisLabels(buckets),
            p50 = stats.p50Days?.let { "p50 $it d" } ?: "p50 —",
            p90 = p90?.let { "p90 · overdue at $it d" } ?: "p90 —",
            drift = stats.driftDays90?.let { "Drift · 90 d ${if (it >= 0) "+" else ""}$it d" }
                ?: "Drift · not enough history",
            meanNote = meanNote(stats.meanDays, p90, stats.sampleDays),
            dwell = dwell,
            dwellNote = dwellNote(dwell),
            overdueCount = overdue,
            loading = false
        )
    }

    /** Four labels across twelve bars, as the design draws them. */
    private fun axisLabels(buckets: List<SettleBucket>): List<String> {
        if (buckets.isEmpty()) return emptyList()
        return listOf(
            buckets.first().label,
            buckets[buckets.size / 3].label,
            buckets[2 * buckets.size / 3].label,
            buckets.last().label
        )
    }

    /**
     * The argument the screen exists to make: the mean is dragged up by a handful of records that
     * took far longer than the rest, so flagging on it would miss every one of them while nagging
     * about the normal ones.
     */
    private fun meanNote(meanDays: Double?, p90: Int?, sample: List<Long>): String {
        if (meanDays == null || p90 == null) return ""
        val tail = sample.count { it > p90 }
        val tailText = when (tail) {
            0 -> "no records"
            1 -> "one record that took far longer"
            else -> "$tail records that took far longer"
        }
        return "The mean here is ${MoneyFormat.percent(meanDays).removeSuffix("%")} d — dragged up " +
            "by $tailText. Flagging on the mean would have missed them while nagging you about " +
            "normal ones. Cleared flags at p90 and recomputes it every time a record lands."
    }

    private fun dwell(byStage: Map<Stage, Double>): List<StageDwellUi> {
        val total = byStage.values.sum()
        if (total <= 0.0) return emptyList()
        return byStage.entries
            .sortedBy { it.key.order }
            .map { (stage, days) ->
                StageDwellUi(
                    stage = stage,
                    days = days,
                    fraction = (days / total).toFloat(),
                    label = stageLabel(stage)
                )
            }
    }

    /** Names where the wait actually is, which is usually not where the user assumes. */
    private fun dwellNote(dwell: List<StageDwellUi>): String {
        val worst = dwell.maxByOrNull { it.days } ?: return ""
        val share = (worst.fraction * 100).toInt()
        val phase = if (worst.phase == Phase.WORK) "a work-phase problem, not a payment problem"
        else "a payment problem, not a work-phase one"
        return "$share% of the wait is ${worst.label} — $phase."
    }
}
