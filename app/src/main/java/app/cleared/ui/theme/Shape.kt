package app.cleared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** The radius scale from design/DESIGN_TOKENS.md §5, by the thing it is the radius of. */
object ClearedShape {
    val stageChip = RoundedCornerShape(5.dp)
    val agePill = RoundedCornerShape(5.dp)
    val filterChip = RoundedCornerShape(9.dp)
    val smallTile = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(12.dp)
    val field = RoundedCornerShape(12.dp)
    val fab = RoundedCornerShape(18.dp)
    val checkbox = RoundedCornerShape(4.dp)
    val phaseRail = RoundedCornerShape(2.dp)
    val progressBar = RoundedCornerShape(3.dp)
    val bottomSheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    /** Buttons and FAB-adjacent pills are fully rounded — height ÷ 2. */
    val pill = RoundedCornerShape(percent = 50)
}

val ClearedShapes = Shapes(
    extraSmall = ClearedShape.stageChip,
    small = ClearedShape.filterChip,
    medium = ClearedShape.card,
    large = ClearedShape.fab,
    extraLarge = RoundedCornerShape(26.dp)
)
