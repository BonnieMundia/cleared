package app.cleared.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cleared.ui.components.StageChip
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `2a` — the offline queue, what it is waiting on, and what disagrees.
 *
 * Reached by tapping the offline strip. Nothing here blocks anything: the app works while this
 * screen has a backlog on it.
 */
@Composable
fun SyncScreen(
    state: SyncUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onResolve: (Long, Boolean) -> Unit = { _, _ -> },
    onRetryNow: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.topAppBarWithBack)
                    .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(Dimens.minTouchTarget)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text("Sync", style = Cleared.type.pushedTitle, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item { StatusHeader(state) }

            if (state.conflicts.isNotEmpty()) {
                item { SectionLabel("Needs you") }
                items(state.conflicts, key = { it.opId }) { Conflict(it, onResolve) }
            }

            if (state.queued.isNotEmpty()) {
                item { SectionLabel("Queued · replays in order") }
                items(state.queued, key = { it.id }) { QueuedRow(it) }
            }

            item { SectionLabel("Rates in use") }
            item { Rates(state, onRetryNow) }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun StatusHeader(state: SyncUiState) {
    Column(Modifier.padding(horizontal = Dimens.screenGutter).padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (state.online) Cleared.semantics.money else Cleared.semantics.overdueDot,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = state.statusTitle,
                style = Cleared.type.cardTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(state.statusBody, style = Cleared.type.body, color = Cleared.tones.onSurfaceVariant2)

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCell("Queued", state.queuedCount.toString(), Modifier.weight(1f))
            StatCell(
                label = "Conflicts",
                value = state.conflictCount.toString(),
                modifier = Modifier.weight(1f),
                emphasised = state.conflictCount > 0
            )
            StatCell("To send", state.bytesToSend, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier, emphasised: Boolean = false) {
    Column(modifier) {
        Text(label, style = Cleared.type.caption, color = Cleared.tones.tertiary)
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = Cleared.type.rowFigure,
            color = if (emphasised) Cleared.semantics.onRejectContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = Cleared.type.sectionOverline,
        color = Cleared.tones.label,
        modifier = Modifier
            .padding(horizontal = Dimens.screenGutter)
            .padding(top = Dimens.sectionSpacing, bottom = 8.dp)
    )
}

@Composable
private fun Conflict(conflict: ConflictUi, onResolve: (Long, Boolean) -> Unit) {
    Column(
        Modifier
            .padding(horizontal = Dimens.screenGutter)
            .fillMaxWidth()
            .background(Cleared.tones.rejectTint, ClearedShape.card)
            .border(Dimens.hairline, Cleared.tones.rejectOutline, ClearedShape.card)
            .padding(Dimens.cardPadding)
    ) {
        Text(
            text = conflict.title,
            style = Cleared.type.rowPrimary,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Side(conflict.mine, Modifier.weight(1f))
            Side(conflict.theirs, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            // Outlined for mine, filled for theirs — the platform is usually right, and the design
            // makes the safer answer the easier one without hiding the other.
            Box(
                Modifier
                    .weight(1f)
                    .height(Dimens.outlinedButton)
                    .border(Dimens.hairline, Cleared.tones.outlineButton, ClearedShape.pill)
                    .clip(ClearedShape.pill)
                    .clickable { onResolve(conflict.opId, false) },
                contentAlignment = Alignment.Center
            ) {
                Text("Keep mine", style = Cleared.type.tableRow, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(Dimens.outlinedButton)
                    .background(MaterialTheme.colorScheme.primary, ClearedShape.pill)
                    .clip(ClearedShape.pill)
                    .clickable { onResolve(conflict.opId, true) },
                contentAlignment = Alignment.Center
            ) {
                Text("Take theirs", style = Cleared.type.tableRow, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(conflict.explanation, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
    }
}

@Composable
private fun Side(side: ConflictSideUi, modifier: Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, ClearedShape.smallTile)
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.smallTile)
            .padding(10.dp)
    ) {
        Text(
            text = "${side.who}, ${side.at}",
            style = Cleared.type.microAnnotation,
            color = Cleared.tones.tertiary
        )
        Spacer(Modifier.height(7.dp))
        StageChip(stage = side.stage)
    }
}

@Composable
private fun QueuedRow(op: QueuedOpUi) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(op.ordinal, style = Cleared.type.rowSubFigure, color = Cleared.tones.tertiary3)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(op.label, style = Cleared.type.tableRow, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(op.subLine, style = Cleared.type.microAnnotation, color = Cleared.tones.tertiary)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .background(
                    if (op.isRetrying) Cleared.semantics.overdueContainer else Cleared.tones.chipBg,
                    ClearedShape.agePill
                )
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = op.stateLabel,
                style = Cleared.type.agePill,
                color = if (op.isRetrying) Cleared.semantics.overdue else Cleared.tones.onSurfaceVariant3,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun Rates(state: SyncUiState, onRetryNow: () -> Unit) {
    Column(Modifier.padding(horizontal = Dimens.screenGutter)) {
        state.rates.forEach { rate ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(rate.currency, style = Cleared.type.tableRow, color = Cleared.tones.onSurfaceVariant2)
                Spacer(Modifier.weight(1f))
                Text(
                    text = rate.rate,
                    style = Cleared.type.captionFigure,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(state.ratesNote, style = Cleared.type.caption, color = Cleared.tones.tertiary2)

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(Dimens.filledButton)
                .background(MaterialTheme.colorScheme.primary, ClearedShape.pill)
                .clip(ClearedShape.pill)
                .clickable(onClick = onRetryNow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Retry now",
                style = Cleared.type.rowPrimary.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
