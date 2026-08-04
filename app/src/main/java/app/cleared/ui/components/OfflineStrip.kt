package app.cleared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedTheme
import app.cleared.ui.theme.Dimens

/**
 * Offline is a condition, not an error.
 *
 * A slim amber band under the app bar with a queued-writes count, tappable through to Sync. No
 * blocking dialog, no spinner over content, nothing disabled — every screen except Discover works
 * with the network off, so the strip states a fact and gets out of the way.
 */
@Composable
fun OfflineStrip(
    lastSyncedLabel: String?,
    queuedWrites: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.offlineStrip)
            .background(Cleared.semantics.offlineStrip)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.screenGutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(7.dp).background(Cleared.semantics.overdueDot, CircleShape))
        Text(
            text = stripText(lastSyncedLabel, queuedWrites),
            style = Cleared.type.captionFigure,
            color = Cleared.semantics.onOfflineStrip
        )
    }
}

/** `Offline · last synced 09:14 · 2 changes queued`. */
fun stripText(lastSyncedLabel: String?, queuedWrites: Int): String = buildString {
    append("Offline")
    if (lastSyncedLabel != null) append(" · last synced $lastSyncedLabel")
    if (queuedWrites > 0) {
        append(" · ")
        append(if (queuedWrites == 1) "1 change queued" else "$queuedWrites changes queued")
    }
}

@Preview(name = "Offline strip · light", showBackground = true, widthDp = 390)
@Composable
private fun OfflineStripLight() = ClearedTheme(darkTheme = false) {
    Surface(color = MaterialTheme.colorScheme.background) {
        OfflineStrip(lastSyncedLabel = "09:14", queuedWrites = 2)
    }
}

@Preview(name = "Offline strip · dark", showBackground = true, widthDp = 390)
@Composable
private fun OfflineStripDark() = ClearedTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        OfflineStrip(lastSyncedLabel = "09:14", queuedWrites = 2)
    }
}
