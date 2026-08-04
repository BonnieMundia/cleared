package app.cleared.ui.pipeline

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cleared.ui.components.LegendSwatch
import app.cleared.ui.components.OfflineStrip
import app.cleared.ui.components.PhaseSplitBar
import app.cleared.ui.components.RecordRow
import app.cleared.ui.components.RecordRowUi
import app.cleared.ui.components.stageLabel
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens
import kotlinx.coroutines.withTimeoutOrNull

/** CLAUDE.md rule 11: state-changing actions show an 8-second undo snackbar. */
const val UndoWindowMillis = 8_000L

/**
 * Frame `1a` — the home screen.
 *
 * Answers "how much is owed to me, and what is stuck" in under three seconds: one accent figure,
 * one bar showing how much of it is still only effort, and the records grouped by the week they are
 * expected to arrive.
 */
@Composable
fun PipelineScreen(
    state: PipelineUiState,
    pendingUndo: UndoableAdvance?,
    modifier: Modifier = Modifier,
    onAdvance: (RecordRowUi) -> Unit = {},
    onOpenRecord: (RecordRowUi) -> Unit = {},
    onLongPressRecord: (RecordRowUi) -> Unit = {},
    onUndo: (UndoableAdvance) -> Unit = {},
    onUndoHandled: () -> Unit = {},
    onAddRecord: () -> Unit = {},
    onOpenSync: () -> Unit = {}
) {
    val snackbars = remember { SnackbarHostState() }

    // Undo, never confirm. Exactly eight seconds — M3's own Long is ten, so the snackbar is shown
    // indefinitely and this cancels it on time. No dialog anywhere in this app asks "are you sure?".
    LaunchedEffect(pendingUndo) {
        val undo = pendingUndo ?: return@LaunchedEffect
        val result = withTimeoutOrNull(UndoWindowMillis) {
            snackbars.showSnackbar(
                message = "${undo.platformName} → ${stageLabel(undo.to)}",
                actionLabel = "Undo",
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite
            )
        }
        if (result == SnackbarResult.ActionPerformed) onUndo(undo) else onUndoHandled()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        snackbarHost = { SnackbarHost(snackbars) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRecord,
                modifier = Modifier.size(Dimens.fab),
                shape = ClearedShape.fab,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Log work")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.offline) {
                OfflineStrip(
                    lastSyncedLabel = state.lastSyncedLabel,
                    queuedWrites = state.queuedWrites,
                    onClick = onOpenSync
                )
            }

            LazyColumn(Modifier.fillMaxSize()) {
                item { HeroBlock(state) }

                state.groups.forEach { group ->
                    item(key = "header-${group.title}") { GroupHeader(group) }
                    items(group.rows, key = { it.id }) { row ->
                        RecordRow(
                            row = row,
                            showDivider = true,
                            onClick = { if (row.stage.isAdvanceable) onAdvance(row) else onOpenRecord(row) },
                            onLongClick = { onLongPressRecord(row) }
                        )
                    }
                }

                item { FooterNote(offline = state.offline) }
                // Clear of the FAB, which sits 104 dp from the bottom.
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

/** The one accent figure on the screen, and the split that explains it. */
@Composable
private fun HeroBlock(state: PipelineUiState) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = Dimens.screenGutter, end = Dimens.screenGutter, top = 6.dp, bottom = 20.dp)
    ) {
        Text(
            text = "Owed to you",
            style = Cleared.type.secondary,
            color = Cleared.tones.onSurfaceVariant3
        )
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "KES",
                style = Cleared.type.heroPrefix,
                color = Cleared.semantics.heroFigure,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.heroDigits,
                style = Cleared.type.heroFigure,
                color = Cleared.semantics.heroFigure,
                modifier = Modifier.alignByBaseline()
            )
        }

        if (state.components.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                state.components.forEach {
                    Text(it, style = Cleared.type.tableFigure, color = Cleared.tones.onSurfaceVariant2)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PhaseSplitBar(workFraction = state.workFraction)

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(Cleared.semantics.work)
            Spacer(Modifier.width(6.dp))
            Text(state.workLegend, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
            Spacer(Modifier.weight(1f))
            LegendSwatch(Cleared.semantics.money)
            Spacer(Modifier.width(6.dp))
            Text(state.moneyLegend, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
        }

        Spacer(Modifier.height(12.dp))
        Text(state.caption, style = Cleared.type.caption, color = Cleared.tones.tertiary2)
    }
}

/**
 * A week header, or the "Needs attention" band. Reversed records pin above the week groups because
 * they have no arrival week left to sit under — the money already arrived and came back.
 */
@Composable
private fun GroupHeader(group: RowGroup) {
    val background =
        if (group.isNeedsAttention) Cleared.semantics.overdueContainer else Cleared.tones.surfaceContainer
    val titleColor =
        if (group.isNeedsAttention) Cleared.semantics.overdue else Cleared.tones.label

    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .padding(start = Dimens.screenGutter, end = Dimens.screenGutter, top = 20.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(group.title, style = Cleared.type.sectionOverline, color = titleColor)
        Spacer(Modifier.weight(1f))
        group.subtitleFigure?.let {
            Text(it, style = Cleared.type.captionFigure, color = titleColor)
        }
    }
}

@Composable
private fun FooterNote(offline: Boolean) {
    Column {
        HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
        Text(
            text = if (offline) {
                "Unlanded amounts are valued at the last rate fetched before you went offline. " +
                    "What actually clears will differ by the fee and spread on the route you pick."
            } else {
                "Unlanded amounts are valued at today's mid rate. What actually clears will differ " +
                    "by the fee and spread on the route you pick."
            },
            style = Cleared.type.caption,
            color = Cleared.tones.tertiary2,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(
                start = Dimens.screenGutter,
                end = Dimens.screenGutter,
                top = 18.dp
            )
        )
    }
}

/** Empty box used while the first flow emission is in flight — never a spinner over content. */
@Composable
internal fun PipelineLoading() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
}
