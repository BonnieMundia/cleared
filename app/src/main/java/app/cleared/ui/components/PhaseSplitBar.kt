package app.cleared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/** The 7 dp square that labels a segment of the split bar. */
val SwatchSize = 7.dp

/**
 * The phase-split bar under the Pipeline hero. 6 dp tall, 3 dp radius, violet sized to
 * `workKes / owedKes` with green filling the rest, over a track in `divider`.
 *
 * Two solid segments, not a gradient: the boundary *is* the information — it is the answer to "how
 * much of what I am owed is still only effort".
 */
@Composable
fun PhaseSplitBar(
    workFraction: Float,
    modifier: Modifier = Modifier
) {
    val fraction = workFraction.coerceIn(0f, 1f)
    val semantics = Cleared.semantics

    Row(
        modifier
            .fillMaxWidth()
            .height(Dimens.splitBar)
            .clip(ClearedShape.progressBar)
            .background(Cleared.tones.divider)
    ) {
        if (fraction > 0f) {
            Box(Modifier.weight(fraction).fillMaxHeight().background(semantics.work))
        }
        if (fraction < 1f) {
            Box(Modifier.weight(1f - fraction).fillMaxHeight().background(semantics.money))
        }
    }
}

@Composable
fun LegendSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(SwatchSize).background(color, ClearedShape.phaseRail))
}
