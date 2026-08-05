package app.cleared.ui.sync

import app.cleared.data.db.entity.FxRateEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.derive.RecordState
import app.cleared.data.model.Stage
import app.cleared.data.model.SyncOpState
import app.cleared.ui.components.stageLabel
import app.cleared.ui.format.DateFormat
import app.cleared.ui.format.MoneyFormat
import java.time.Duration
import java.time.Instant

data class QueuedOpUi(
    val id: Long,
    val ordinal: String,
    val label: String,
    val subLine: String,
    val stateLabel: String,
    val isRetrying: Boolean
)

data class ConflictSideUi(val who: String, val at: String, val stage: Stage)

data class ConflictUi(
    val opId: Long,
    val title: String,
    val mine: ConflictSideUi,
    val theirs: ConflictSideUi,
    val explanation: String
)

data class RateRowUi(val currency: String, val rate: String)

data class SyncUiState(
    val online: Boolean = true,
    val statusTitle: String = "",
    val statusBody: String = "",
    val queuedCount: Int = 0,
    val conflictCount: Int = 0,
    val bytesToSend: String = "0 B",
    val conflicts: List<ConflictUi> = emptyList(),
    val queued: List<QueuedOpUi> = emptyList(),
    val rates: List<RateRowUi> = emptyList(),
    val ratesNote: String = "",
    val loading: Boolean = true
)

/** Frame `2a`. The queue, what it is waiting for, and what disagrees. */
object SyncMapper {

    fun build(
        online: Boolean,
        ops: List<SyncOpEntity>,
        states: List<RecordState>,
        platforms: List<PlatformEntity>,
        rates: List<FxRateEntity>,
        lastSyncedAt: Instant?,
        now: Instant
    ): SyncUiState {
        val pending = ops.filter { it.state == SyncOpState.WAITING || it.state == SyncOpState.RETRYING }
            .sortedBy { it.id }
        val conflicts = ops.filter { it.state == SyncOpState.CONFLICT }.sortedBy { it.id }
        val bytes = pending.sumOf { it.sizeBytes }

        return SyncUiState(
            online = online,
            statusTitle = if (online) {
                lastSyncedAt?.let { "Synced ${DateFormat.time(it)}" } ?: "Online"
            } else {
                lastSyncedAt?.let { "Offline since ${DateFormat.time(it)}" } ?: "Offline"
            },
            statusBody = if (online) {
                "Everything you write is sent as you write it."
            } else {
                "Everything still works. What you write is queued here and replays in order when " +
                    "you are back."
            },
            queuedCount = pending.size,
            conflictCount = conflicts.size,
            bytesToSend = formatBytes(bytes),
            conflicts = conflicts.map { conflict(it, states, platforms) },
            queued = pending.mapIndexed { index, op -> queuedRow(op, index + 1, now) },
            rates = rates.map { RateRowUi(it.currency.name, MoneyFormat.rate(it.midToKes)) },
            ratesNote = ratesNote(rates, now),
            loading = false
        )
    }

    private fun queuedRow(op: SyncOpEntity, ordinal: Int, now: Instant): QueuedOpUi {
        val retrying = op.state == SyncOpState.RETRYING
        val nextIn = op.nextAttemptAt?.let { Duration.between(now, it) }?.takeIf { !it.isNegative }

        return QueuedOpUi(
            id = op.id,
            ordinal = ordinal.toString().padStart(2, '0'),
            label = op.label ?: "${op.entityType} ${op.entityId}",
            subLine = buildString {
                append(DateFormat.time(op.createdAt))
                if (retrying) {
                    append(" · retry ${op.attempts} of 5")
                    nextIn?.let { append(" · next in ${it.toMinutes().coerceAtLeast(1)} min") }
                }
            },
            stateLabel = if (retrying) "retrying" else "waiting",
            isRetrying = retrying
        )
    }

    /**
     * Both sides, with both timestamps. The explanatory line is the important part: taking the
     * platform's answer does not discard the hours already logged.
     */
    private fun conflict(
        op: SyncOpEntity,
        states: List<RecordState>,
        platforms: List<PlatformEntity>
    ): ConflictUi {
        val state = states.firstOrNull { it.record.id == op.entityId }
        val platform = platforms.firstOrNull { it.id == state?.record?.platformId }
        val amount = state?.let { MoneyFormat.formatMinor(it.record.currency, it.record.grossMinor) }
        val hours = state?.let { it.record.hoursWorked + it.record.hoursUnpaid } ?: 0.0
        val remoteStage = op.remoteStage ?: Stage.REJECTED

        return ConflictUi(
            opId = op.id,
            title = listOfNotNull(platform?.name, amount).joinToString(" · ") + " disagrees",
            mine = ConflictSideUi(
                who = "You",
                at = DateFormat.time(op.createdAt),
                stage = state?.displayStage ?: Stage.SUBMITTED
            ),
            theirs = ConflictSideUi(
                who = "Platform",
                at = op.remoteOccurredAt?.let { DateFormat.time(it) } ?: "—",
                stage = remoteStage
            ),
            explanation = "Taking theirs keeps your ${MoneyFormat.hours(hours)} logged. " +
                "The record moves to ${stageLabel(remoteStage)} and the hours stay against " +
                "${platform?.name ?: "the platform"}."
        )
    }

    /**
     * Anything older than about two hours is stale, and unlanded figures are estimates against it.
     * Landed records keep the rate they actually converted at and are unaffected.
     */
    private fun ratesNote(rates: List<FxRateEntity>, now: Instant): String {
        val fetchedAt = rates.minOfOrNull { it.fetchedAt } ?: return "No rates fetched yet."
        val age = Duration.between(fetchedAt, now)
        val ageText = when {
            age.toHours() >= 1 -> "${age.toHours()} h ${age.toMinutes() % 60} m old"
            else -> "${age.toMinutes()} m old"
        }
        return "Fetched ${DateFormat.time(fetchedAt)}, $ageText. Landed records keep the rate they " +
            "actually converted at — only unlanded estimates move when this refreshes."
    }

    /** The user is on metered mobile data, so this is stated in the units that matter to them. */
    fun formatBytes(bytes: Int): String {
        if (bytes < 1024) return "$bytes B"
        val kb = java.math.BigDecimal.valueOf(bytes.toLong())
            .divide(java.math.BigDecimal(1024), 1, java.math.RoundingMode.HALF_UP)
        return "${kb.toPlainString()} KB"
    }
}
