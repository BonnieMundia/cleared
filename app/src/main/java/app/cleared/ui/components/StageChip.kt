package app.cleared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.ClearedTheme

/**
 * The stage chip. 10.5 sp SemiBold, 3 × 7 dp padding, 5 dp radius, `*Container` background with
 * `on*Container` text — one per phase family and never mixed.
 *
 * Prospect is the exception: a **dashed outline**, never filled. It is not yet work, and a filled
 * chip would claim it is.
 */
@Composable
fun StageChip(
    stage: Stage,
    modifier: Modifier = Modifier,
    label: String = stageLabel(stage)
) {
    if (stage == Stage.PROSPECT) {
        ProspectChip(label = label, modifier = modifier)
        return
    }

    val (targetContainer, targetOnContainer) = chipColors(stage)

    // Cross-fades with the rail when a record crosses the phase boundary. See PhaseCrossfadeMillis.
    val container by animateColorAsState(
        targetContainer, tween(PhaseCrossfadeMillis), label = "stageChipContainer"
    )
    val onContainer by animateColorAsState(
        targetOnContainer, tween(PhaseCrossfadeMillis), label = "stageChipContent"
    )

    Box(
        modifier = modifier
            .background(container, ClearedShape.stageChip)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = label, style = Cleared.type.stageChip, color = onContainer)
    }
}

/** Dashed 1 dp outline, no fill. Introduced by Discovery in frame `3b`. */
@Composable
fun ProspectChip(
    label: String = "Prospect",
    modifier: Modifier = Modifier
) {
    val outline = Cleared.tones.outlineDashed
    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    brush = SolidColor(outline),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 2.dp.toPx()), 0f
                        )
                    )
                )
            }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = label, style = Cleared.type.stageChip, color = Cleared.tones.tertiary)
    }
}

/**
 * Container and text colour for a stage, by phase family.
 *
 * Work is violet, money is green, both terminal stages are red. Nothing else may borrow these.
 */
@Composable
fun chipColors(stage: Stage): Pair<Color, Color> {
    val semantics = Cleared.semantics
    return when (stage.phase) {
        Phase.WORK -> semantics.workContainer to semantics.onWorkContainer
        Phase.MONEY -> semantics.moneyContainer to semantics.onMoneyContainer
        Phase.TERMINAL -> semantics.rejectContainer to semantics.onRejectContainer
        Phase.PRE -> Color.Transparent to Cleared.tones.tertiary
    }
}

/** Copy from design/SCREENS.md. The wording is designed, not filler. */
fun stageLabel(stage: Stage): String = when (stage) {
    Stage.PROSPECT -> "Prospect"
    Stage.SUBMITTED -> "Submitted"
    Stage.IN_REVIEW -> "In review"
    Stage.APPROVED -> "Approved"
    Stage.PAYOUT_ISSUED -> "Payout issued"
    Stage.RECEIVED -> "Received"
    Stage.LANDED -> "Landed"
    Stage.REJECTED -> "Rejected"
    Stage.REVERSED -> "Reversed"
}

@Preview(name = "Stage chips · light", showBackground = true, widthDp = 390)
@Composable
private fun StageChipsLight() = ClearedTheme(darkTheme = false) { ChipSheet() }

@Preview(name = "Stage chips · dark", showBackground = true, widthDp = 390)
@Composable
private fun StageChipsDark() = ClearedTheme(darkTheme = true) { ChipSheet() }

/** All nine stages plus the split-record chip, which is a rendering of settlements, not a stage. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSheet() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Stage.entries.forEach { StageChip(it) }
            StageChip(Stage.PAYOUT_ISSUED, label = "Part paid · 40% left")
        }
    }
}
