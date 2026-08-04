package app.cleared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.ClearedTheme

/**
 * The age pill. Mono 10.5 sp Medium, 3 × 6 dp padding, 5 dp radius.
 *
 * Neutral it is `surfaceContainerHigh` on `onSurfaceVariant3`; overdue it turns amber. Amber here
 * means one specific thing — this record is past its platform's own p90 — and is never decorative.
 */
@Composable
fun AgePill(
    text: String,
    overdue: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (overdue) Cleared.semantics.overdueContainer else Cleared.tones.chipBg
    val content = if (overdue) Cleared.semantics.overdue else Cleared.tones.onSurfaceVariant3
    Box(
        modifier = modifier
            .background(background, ClearedShape.agePill)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text = text, style = Cleared.type.agePill, color = content)
    }
}

/**
 * The pill's copy, from design/SCREENS.md: `3d` normally, `31d · 7 over` when overdue, `closed`
 * when rejected, `17d stalled` when a payout reversed and the money is sitting in the wrong place.
 */
fun agePillText(days: Long, daysOver: Long? = null, closed: Boolean = false, stalled: Boolean = false): String =
    when {
        closed -> "closed"
        stalled -> "${days}d stalled"
        daysOver != null -> "${days}d · $daysOver over"
        else -> "${days}d"
    }

@Preview(name = "Age pills · light", showBackground = true)
@Composable
private fun AgePillsLight() = ClearedTheme(darkTheme = false) { PillRow() }

@Preview(name = "Age pills · dark", showBackground = true)
@Composable
private fun AgePillsDark() = ClearedTheme(darkTheme = true) { PillRow() }

@Composable
private fun PillRow() {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, shadowElevation = 0.dp) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AgePill(agePillText(3), overdue = false)
            AgePill(agePillText(31, daysOver = 7), overdue = true)
            AgePill(agePillText(17, stalled = true), overdue = true)
            AgePill(agePillText(9, closed = true), overdue = false)
        }
    }
}
