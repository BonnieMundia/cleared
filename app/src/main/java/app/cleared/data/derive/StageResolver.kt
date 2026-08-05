package app.cleared.data.derive

import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.db.entity.SettlementEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import java.time.Duration
import java.time.Instant

/**
 * Current stage, derived. Nothing here reads a stored stage column, because there isn't one.
 *
 *     currentStage(record) = the StageEvent with the greatest occurredAt,
 *                            ties broken by the greater Stage.order
 *
 * Insertion id is the final tie-break so the result is deterministic when two events share both an
 * instant and a stage.
 */
object StageResolver {

    private val byRecency = compareBy<StageEventEntity>({ it.occurredAt }, { it.stageOrder }, { it.id })

    fun latest(events: List<StageEventEntity>): StageEventEntity? = events.maxWithOrNull(byRecency)

    /** Record-scoped events only. Work-phase events always live here. */
    fun recordEvents(detail: RecordDetail): List<StageEventEntity> =
        detail.events.filter { it.settlementId == null }

    fun recordStage(detail: RecordDetail): Stage? = latest(recordEvents(detail))?.stage

    /**
     * A settlement inherits the record's work phase until it gets a money-phase event of its own —
     * work-phase events are never written against a settlement.
     */
    fun settlementStage(detail: RecordDetail, settlementId: Long): Stage? =
        latest(detail.events.filter { it.settlementId == settlementId })?.stage ?: recordStage(detail)

    /**
     * The first event of the phase the record is currently in. Age is measured from here, not from
     * record creation — a record that sat in review for a month and then moved to Approved
     * yesterday is one day into the work phase's last stage but 31 days into the phase.
     *
     * "First event **in** the current phase" is meant literally: the earliest event belonging to
     * that phase anywhere in the log, not the start of the latest unbroken run of it.
     *
     * The difference is not academic. Undo appends the previous stage as a new event, so undoing a
     * tap that crossed the phase boundary puts a fresh work-phase event at the top of a log whose
     * earlier work-phase events are days old. Reading the run would restart the age at zero and the
     * pill would tell the user their two-day-old record was logged this second.
     */
    fun currentPhaseStartedAt(detail: RecordDetail): Instant? {
        val events = recordEvents(detail)
        val current = latest(events) ?: return null
        return events
            .filter { it.stage.phase == current.stage.phase }
            .minOfOrNull { it.occurredAt }
    }

    /** Calendar days, so `31d` on the age pill means the date moved 31 times. */
    fun daysInCurrentPhase(detail: RecordDetail, now: Instant): Long {
        val from = currentPhaseStartedAt(detail) ?: return 0
        return CalendarDays.between(from, now)
    }
}

/** One settlement's derived position. */
data class SettlementState(
    val settlement: SettlementEntity,
    val stage: Stage,
    val isLanded: Boolean
) {
    val phase: Phase get() = stage.phase
}

/**
 * Everything the rest of the app needs to know about where a record stands.
 *
 * [displayStage] is what the row's chip reads. [isPartPaid] is carried separately because
 * "Part paid" is not a stage — a split record has no single current stage, so the chip is a
 * rendering of the settlement set, not of the event log.
 */
data class RecordState(
    val detail: RecordDetail,
    val displayStage: Stage,
    val isPartPaid: Boolean,
    val settlementStates: List<SettlementState>,
    /** Unlanded value in the record's own currency, minor units. Zero for landed and terminal. */
    val owedMinorByPhase: Map<Phase, Long>
) {
    val record get() = detail.record
    val owedMinor: Long get() = owedMinorByPhase.values.sum()
    val isOwed: Boolean get() = owedMinor != 0L
    val isSplit: Boolean get() = settlementStates.isNotEmpty()

    /** The fraction of the record's money that has landed. Drives the split rail in frame `4c`. */
    val clearedFraction: Double
        get() {
            if (!isSplit) return if (displayStage == Stage.LANDED) 1.0 else 0.0
            val total = settlementStates.sumOf { it.settlement.amountMinor }
            if (total == 0L) return 0.0
            return settlementStates.filter { it.isLanded }.sumOf { it.settlement.amountMinor }.toDouble() / total
        }

    companion object {

        fun of(detail: RecordDetail): RecordState {
            val recordStage = StageResolver.recordStage(detail) ?: Stage.PROSPECT
            val settlements = detail.settlements.sortedBy { it.sequence }

            if (settlements.isEmpty()) {
                val owed = if (recordStage.phase == Phase.WORK || recordStage.phase == Phase.MONEY) {
                    // LANDED is money-phase but nothing is still in flight.
                    if (recordStage == Stage.LANDED) 0L else detail.record.grossMinor
                } else 0L
                return RecordState(
                    detail = detail,
                    displayStage = recordStage,
                    isPartPaid = false,
                    settlementStates = emptyList(),
                    owedMinorByPhase = if (owed == 0L) emptyMap() else mapOf(recordStage.phase to owed)
                )
            }

            val states = settlements.map { settlement ->
                val stage = StageResolver.settlementStage(detail, settlement.id) ?: recordStage
                SettlementState(settlement, stage, stage == Stage.LANDED)
            }

            val landed = states.count { it.isLanded }
            val display = when {
                landed == states.size -> Stage.LANDED
                // "some landed, some not" reads Part paid; "none landed" reads the earliest
                // unlanded settlement's stage. Both point at the same settlement for the chip.
                else -> states.first { !it.isLanded }.stage
            }

            val owed = mutableMapOf<Phase, Long>()
            for (state in states) {
                if (state.isLanded) continue
                if (state.phase != Phase.WORK && state.phase != Phase.MONEY) continue
                owed[state.phase] = (owed[state.phase] ?: 0L) + state.settlement.amountMinor
            }

            return RecordState(
                detail = detail,
                displayStage = display,
                isPartPaid = landed in 1 until states.size,
                settlementStates = states,
                owedMinorByPhase = owed
            )
        }
    }
}

/**
 * The hours a record puts against its platform.
 *
 * Rejected and reversed records keep theirs — that is the point of the app. A re-issue carries
 * none, because the predecessor already counted them.
 */
fun RecordState.billableHours(): Double =
    if (record.carriesHours) record.hoursWorked + record.hoursUnpaid else 0.0

fun RecordState.unpaidHours(): Double = if (record.carriesHours) record.hoursUnpaid else 0.0
