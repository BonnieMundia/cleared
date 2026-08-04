package app.cleared.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and sizing from design/DESIGN_TOKENS.md §5.
 *
 * The scale is 2 · 4 · 6 · 7 · 8 · 9 · 10 · 11 · 12 · 14 · 16 · 18 · 20 · 22 · 26 · 28 — the odd
 * values are deliberate and come from the prototype, so they are named here rather than rounded to
 * a tidier 4 dp grid.
 */
object Dimens {
    val screenGutter = 20.dp
    val cardPadding = 15.dp
    val rowVerticalPadding = 12.dp
    val rowHorizontalPadding = 20.dp
    val rowInternalGap = 11.dp
    val cardGap = 12.dp
    val sectionSpacing = 22.dp
    /** 8 dp band in surfaceContainerHigh, with a 1 dp rule top and bottom. */
    val spacerBand = 8.dp

    val hairline = 1.dp
    val topAppBar = 48.dp
    val topAppBarWithBack = 56.dp
    val offlineStrip = 32.dp
    val bottomNav = 64.dp
    val gestureArea = 22.dp

    val railWidth = 3.dp
    val railHeight = 36.dp
    val splitBar = 6.dp
    val progressBar = 5.dp

    val fab = 60.dp
    val fabEndMargin = 16.dp
    val fabBottomMargin = 104.dp

    val filledButton = 52.dp
    val outlinedButton = 48.dp

    /** Nothing tappable is ever smaller than this. */
    val minTouchTarget = 48.dp
}
