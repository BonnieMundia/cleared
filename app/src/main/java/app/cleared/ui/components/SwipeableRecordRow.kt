package app.cleared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cleared.data.model.Stage
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.Dimens

/** Frame `2d`: the green panel revealed behind a row swiped right. */
private val AdvancePanelWidth = 96.dp

/**
 * A record row you can swipe.
 *
 * Swipe right advances one stage; swipe left opens the record. Both are undoable from the snackbar
 * for eight seconds — there is no confirmation dialog anywhere in this app, so the swipe has to be
 * cheap to get wrong.
 *
 * A row that cannot advance — landed, rejected, reversed — does not offer the right swipe at all,
 * because a gesture that silently does nothing is worse than one that is not there.
 */
@Composable
fun SwipeableRecordRow(
    row: RecordRowUi,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onAdvance: () -> Unit = {},
    onOpen: () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val canAdvance = row.stage.isAdvanceable

    // Selection mode owns the horizontal gesture: dragging to select and dragging to advance in the
    // same space would make both unreliable.
    if (selectionMode) {
        RecordRow(
            row = row,
            modifier = modifier,
            showDivider = showDivider,
            selectionMode = true,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick
        )
        return
    }

    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> canAdvance
                SwipeToDismissBoxValue.EndToStart -> true
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    // The row springs back either way: a swipe is a command, not a deletion, and the row stays.
    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onAdvance()
                state.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onOpen()
                state.reset()
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = canAdvance,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeBackground(state.dismissDirection, row.stage) }
    ) {
        RecordRow(
            row = row,
            showDivider = showDivider,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, stage: Stage) {
    val advancing = direction == SwipeToDismissBoxValue.StartToEnd
    Box(
        Modifier
            .fillMaxSize()
            .background(
                if (advancing) Cleared.semantics.moneyContainer else Cleared.tones.surfaceHigh
            ),
        contentAlignment = if (advancing) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Box(
            Modifier.width(AdvancePanelWidth).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (advancing) "Advance" else "Open",
                style = Cleared.type.stageChip,
                color = if (advancing) Cleared.semantics.onMoneyContainer
                else Cleared.tones.onSurfaceVariant2,
                modifier = Modifier.padding(horizontal = Dimens.rowInternalGap)
            )
        }
    }
}

