package app.cleared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * The 3 × 36 dp phase rail down the left of a record row. Violet for work, green for money, red for
 * both terminal stages.
 *
 * A part-paid record's rail encodes the split: `money` down to the cleared fraction, `moneyContainer`
 * below it.
 *
 * design/SCREENS.md `4c` calls that split "a linear gradient". It is drawn here as two solid
 * segments meeting at a hard edge, because CLAUDE.md rule 4 forbids gradients outright and a rail
 * 3 dp wide reads a blend as mud. The information — where the boundary sits — is identical, and a
 * hard stop states it more precisely than a ramp. Worth confirming with the designer.
 */
@Composable
fun PhaseRail(
    stage: Stage,
    modifier: Modifier = Modifier,
    clearedFraction: Float? = null,
    height: Dp = Dimens.railHeight
) {
    val semantics = Cleared.semantics

    if (stage == Stage.PROSPECT) {
        // Not yet work, so the rail is an outline rather than a fill — the same rule as the chip.
        val outline = Cleared.tones.outlineDashed
        Box(
            modifier = modifier
                .size(Dimens.railWidth, height)
                .drawBehind {
                    drawRoundRect(
                        brush = SolidColor(outline),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(3.dp.toPx(), 2.dp.toPx()), 0f
                            )
                        )
                    )
                }
        )
        return
    }

    val solid = when (stage.phase) {
        Phase.WORK -> semantics.work
        Phase.MONEY -> semantics.money
        Phase.TERMINAL -> semantics.reject
        Phase.PRE -> semantics.work
    }

    if (clearedFraction == null) {
        Box(modifier.size(Dimens.railWidth, height).background(solid, ClearedShape.phaseRail))
        return
    }

    val cleared = clearedFraction.coerceIn(0f, 1f)
    Box(
        modifier
            .size(Dimens.railWidth, height)
            .clip(ClearedShape.phaseRail)
            .background(semantics.moneyContainer)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height * cleared)
                .background(semantics.money)
        )
    }
}
