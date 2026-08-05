package app.cleared.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cleared.data.derive.TimelinePhase
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import app.cleared.ui.components.stageLabel
import app.cleared.ui.format.DateFormat
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * The record's history as a phase-labelled timeline — frame `1e`.
 *
 * Whatever events exist, in order, and never a placeholder for a stage that was skipped. If a
 * platform reports approval directly and no `IN_REVIEW` event was ever written, the timeline draws
 * `Submitted → Approved` and that is the truth about the record.
 */
@Composable
fun PhaseTimeline(
    phases: List<TimelinePhase>,
    modifier: Modifier = Modifier,
    reversed: Boolean = false,
    reversalReason: String? = null,
    splitNote: String? = null
) {
    Column(modifier.fillMaxWidth()) {
        phases.forEachIndexed { phaseIndex, phase ->
            PhaseHeader(phase)
            phase.entries.forEachIndexed { index, entry ->
                val isFinalOverall = phaseIndex == phases.lastIndex && index == phase.entries.lastIndex
                // The step that did not complete: a hollow ring under a dashed red connector.
                val broken = reversed && entry.stage == Stage.REVERSED
                TimelineEntryRow(
                    stage = entry.stage,
                    label = if (broken) "Reversed by the bank" else entryLabel(entry.stage, entry.event.note),
                    timestamp = DateFormat.timestamp(entry.occurredAt),
                    delta = if (index == 0 && phaseIndex == 0) null else DateFormat.delta(entry.deltaDays),
                    isLast = isFinalOverall,
                    broken = broken,
                    connectorBroken = reversed && index == phase.entries.lastIndex - 1 &&
                        phase.entries.getOrNull(index + 1)?.stage == Stage.REVERSED
                )
                if (broken && reversalReason != null) {
                    ReasonBlock(reversalReason)
                }
            }
        }
        if (splitNote != null) {
            SplitNode(splitNote)
        }
    }
}

@Composable
private fun PhaseHeader(phase: TimelinePhase) {
    val semantics = Cleared.semantics
    val color = when (phase.phase) {
        Phase.WORK -> semantics.onWorkContainer
        Phase.MONEY -> semantics.onMoneyContainer
        else -> semantics.onRejectContainer
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(phaseName(phase.phase), style = Cleared.type.phaseOverline, color = color)
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = Dimens.hairline,
            color = Cleared.tones.divider
        )
        Spacer(Modifier.width(10.dp))
        Text(DateFormat.days(phase.durationDays), style = Cleared.type.agePill, color = color)
    }
}

private fun phaseName(phase: Phase) = when (phase) {
    Phase.PRE -> "BEFORE WORK"
    Phase.WORK -> "WORK PHASE"
    Phase.MONEY -> "MONEY PHASE"
    Phase.TERMINAL -> "ENDED"
}

/** `Received in Payoneer` reads better than `Received`, when the log says where. */
private fun entryLabel(stage: Stage, note: String?): String =
    if (note.isNullOrBlank()) stageLabel(stage) else "${stageLabel(stage)} in $note"

@Composable
private fun TimelineEntryRow(
    stage: Stage,
    label: String,
    timestamp: String,
    delta: String?,
    isLast: Boolean,
    broken: Boolean,
    connectorBroken: Boolean
) {
    val semantics = Cleared.semantics
    val dotColor = when {
        broken -> semantics.reject
        stage.phase == Phase.WORK -> semantics.work
        stage.phase == Phase.MONEY -> semantics.money
        else -> semantics.reject
    }
    val connectorColor = when {
        connectorBroken -> semantics.reject
        stage.phase == Phase.WORK -> semantics.workOutline
        stage.phase == Phase.MONEY -> semantics.moneyOutline
        else -> semantics.rejectContainer
    }
    val surface = MaterialTheme.colorScheme.surface

    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(10.dp)
                .height(46.dp)
                .drawBehind {
                    val centreX = size.width / 2f
                    val dotY = 6.dp.toPx()
                    if (!isLast) {
                        val stroke = 1.5.dp.toPx()
                        val effect = if (connectorBroken) {
                            PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
                        } else null
                        drawLine(
                            color = connectorColor,
                            start = Offset(centreX, dotY + 6.dp.toPx()),
                            end = Offset(centreX, size.height),
                            strokeWidth = stroke,
                            pathEffect = effect
                        )
                    }
                    if (broken) {
                        // Hollow ring, not a filled dot: the step did not complete.
                        drawCircle(color = surface, radius = 5.5.dp.toPx(), center = Offset(centreX, dotY))
                        drawCircle(
                            color = dotColor,
                            radius = 4.5.dp.toPx(),
                            center = Offset(centreX, dotY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    } else {
                        drawCircle(color = dotColor, radius = 4.5.dp.toPx(), center = Offset(centreX, dotY))
                    }
                }
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = Cleared.type.tableRow.copy(
                        fontWeight = if (isLast || broken) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (broken) semantics.onRejectContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                delta?.let {
                    Text(it, style = Cleared.type.rowSubFigure, color = Cleared.tones.tertiary)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(timestamp, style = Cleared.type.captionFigure, color = Cleared.tones.tertiary)
        }
    }
}

/** Why the bank sent it back, in its own block so it reads as the record's reason and not a note. */
@Composable
private fun ReasonBlock(reason: String) {
    Text(
        text = reason,
        style = Cleared.type.caption,
        color = Cleared.semantics.onRejectContainer,
        modifier = Modifier
            .padding(start = 22.dp, bottom = 14.dp)
            .fillMaxWidth()
            .background(Cleared.semantics.rejectContainer, ClearedShape.smallTile)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

/**
 * The last node of a split record's shared work phase. There is no per-settlement work phase — the
 * hours belong to the record and are never divided.
 */
@Composable
private fun SplitNode(note: String) {
    val moneyDot = Cleared.semantics.money
    Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Box(
            Modifier
                .width(10.dp)
                .height(16.dp)
                .drawBehind {
                    drawCircle(
                        color = moneyDot,
                        radius = 4.5.dp.toPx(),
                        center = Offset(size.width / 2f, 6.dp.toPx())
                    )
                }
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Split into 2 settlements",
                style = Cleared.type.tableRow.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = note,
                style = Cleared.type.caption,
                color = Cleared.tones.onSurfaceVariant2,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cleared.tones.surfaceContainer, ClearedShape.smallTile)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}
