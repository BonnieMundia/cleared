package app.cleared.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.cleared.data.model.Stage
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Everything a record row draws. Deliberately a presentation model rather than a domain type: the
 * row is the atom reused by Pipeline, bulk triage and the components sheet, and none of them should
 * have to agree on a repository.
 */
data class RecordRowUi(
    val id: Long,
    val platformName: String,
    val stage: Stage,
    /** `EUR 640.00`. */
    val grossText: String,
    /** `KES 94,464`, or the phrase the stage calls for — see [kesToneFor]. */
    val kesText: String,
    /** `3d` · `31d · 7 over` · `closed` · `17d stalled`. */
    val ageText: String,
    val overdue: Boolean = false,
    /** Non-null only for a split record: the fraction of its money that has landed. */
    val clearedFraction: Float? = null,
    val chipLabel: String = stageLabel(stage)
)

/**
 * The record row — the atom of the app.
 *
 * ```
 * │ ▌  Lumen Writers                         EUR 640.00 │
 * │ ▌  [In review]  [31d · 7 over]           KES 94,464 │
 * ```
 *
 * ~68 dp tall, so it clears the 48 dp minimum touch target on its own. The two figures sit in their
 * own right-aligned column: that is what makes a list of them line up, and it is why they are a
 * trailing `Column` rather than trailing elements of each line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordRow(
    row: RecordRowUi,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val rejected = row.stage == Stage.REJECTED
    val background = when {
        selected -> Cleared.tones.selectedRow
        pressed -> Cleared.tones.surfaceLow
        else -> MaterialTheme.colorScheme.surface
    }

    Column(modifier.background(background)) {
        if (showDivider) {
            HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .defaultMinSize(minHeight = 68.dp)
                .padding(
                    horizontal = Dimens.rowHorizontalPadding,
                    vertical = Dimens.rowVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                SelectionBox(selected = selected)
                Spacer(Modifier.width(Dimens.rowInternalGap))
            }

            PhaseRail(stage = row.stage, clearedFraction = row.clearedFraction)

            Spacer(Modifier.width(Dimens.rowInternalGap))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = row.platformName,
                    style = Cleared.type.rowPrimary,
                    // A rejected record's name recedes; the money never existed.
                    color = if (rejected) Cleared.tones.tertiary2 else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StageChip(stage = row.stage, label = row.chipLabel)
                    AgePill(text = row.ageText, overdue = row.overdue)
                }
            }

            Spacer(Modifier.width(Dimens.rowInternalGap))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = row.grossText,
                    style = Cleared.type.rowFigure,
                    color = Cleared.semantics.figure,
                    textAlign = TextAlign.End,
                    // Struck through means the money never existed — which is REJECTED, and only
                    // REJECTED. A reversed payout did exist, so it is never struck.
                    textDecoration = if (rejected) TextDecoration.LineThrough else null
                )
                Text(
                    text = row.kesText,
                    style = Cleared.type.rowSubFigure,
                    color = kesToneFor(row.stage),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/** A 20 dp box ahead of the phase rail, filled with the accent and a white check when selected. */
@Composable
private fun SelectionBox(selected: Boolean) {
    Box(
        Modifier
            .size(20.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ClearedShape.checkbox
            )
            .border(
                Dimens.hairline,
                if (selected) MaterialTheme.colorScheme.primary else Cleared.tones.outlineButton,
                ClearedShape.checkbox
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * The colour of the second figure, which carries the stage's outcome as much as its value:
 * green when it cleared, red when there was no payout or the payout came back, neutral while it is
 * still in flight.
 */
@Composable
fun kesToneFor(stage: Stage): Color = when (stage) {
    Stage.LANDED -> Cleared.semantics.onMoneyContainer
    Stage.REJECTED, Stage.REVERSED -> Cleared.semantics.onRejectContainer
    else -> Cleared.tones.tertiary2
}
