package app.cleared.data.derive

import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.model.Phase
import java.time.Duration
import java.time.Instant

/** One row of the record-detail history: an event and how long it took to arrive. */
data class TimelineEntry(
    val event: StageEventEntity,
    val deltaDays: Long,
    val isLast: Boolean
) {
    val stage get() = event.stage
    val occurredAt: Instant get() = event.occurredAt
}

/** A phase-labelled block of the timeline, with the time the record spent in that phase. */
data class TimelinePhase(
    val phase: Phase,
    val entries: List<TimelineEntry>,
    val durationDays: Long
)

/**
 * The record-detail history — frame `1e`.
 *
 * Render whatever events exist and never a placeholder for a stage that was skipped. Halo Data
 * reports approval directly, so no `IN_REVIEW` event is ever written for it and the timeline draws
 * `Submitted → Approved`. That is correct: the log is the history.
 */
object Timeline {

    fun of(detail: RecordDetail): List<TimelinePhase> {
        val events = StageResolver.recordEvents(detail)
            .sortedWith(compareBy({ it.occurredAt }, { it.stageOrder }, { it.id }))
        if (events.isEmpty()) return emptyList()

        val entries = events.mapIndexed { index, event ->
            TimelineEntry(
                event = event,
                deltaDays = if (index == 0) 0
                else CalendarDays.between(events[index - 1].occurredAt, event.occurredAt),
                isLast = index == events.lastIndex
            )
        }

        // Group into contiguous phase blocks, so a record that re-entered a phase after an undo
        // reads as two blocks rather than one impossible interleaving.
        //
        // A terminal event joins the block it ended rather than opening one of its own. Frame `4a`
        // is explicit: the money phase runs green through Received and *then* breaks, with the
        // dashed connector and the hollow ring inside that block. A rejection ends the work phase
        // the same way — it leaves the work phase without ever entering the money phase.
        val blocks = mutableListOf<MutableList<TimelineEntry>>()
        for (entry in entries) {
            val last = blocks.lastOrNull()
            val joinsPrevious = last != null &&
                (last.first().stage.phase == entry.stage.phase || entry.stage.phase == Phase.TERMINAL)
            if (joinsPrevious) last!! += entry else blocks += mutableListOf(entry)
        }

        return blocks.mapIndexed { index, block ->
            val start = block.first().occurredAt
            // A phase lasts until the next phase begins; the final one until its own last event.
            val end = blocks.getOrNull(index + 1)?.first()?.occurredAt ?: block.last().occurredAt
            TimelinePhase(
                phase = block.first().stage.phase,
                entries = block,
                durationDays = CalendarDays.between(start, end)
            )
        }
    }

    /** Submitted to landed, the figure the header strip calls "End to end". */
    fun endToEndDays(detail: RecordDetail): Long? {
        val events = StageResolver.recordEvents(detail)
        val first = events.minByOrNull { it.occurredAt } ?: return null
        val last = events.maxByOrNull { it.occurredAt } ?: return null
        return CalendarDays.between(first.occurredAt, last.occurredAt)
    }
}
