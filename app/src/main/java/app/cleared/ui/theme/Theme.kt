package app.cleared.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The four semantic families, exactly as design/DESIGN_TOKENS.md specifies them.
 *
 * They are deliberately **not** part of `ColorScheme`. Work/money/overdue/reject are meanings, not
 * Material roles, and putting them in the scheme would invite a component to pick one up
 * decoratively. Violet and green are never decorative here.
 */
data class ClearedSemantics(
    val work: Color, val workContainer: Color, val onWorkContainer: Color, val workOutline: Color,
    val money: Color, val moneyContainer: Color, val onMoneyContainer: Color, val moneyOutline: Color,
    val overdue: Color, val overdueContainer: Color, val overdueBar: Color,
    val offlineStrip: Color, val onOfflineStrip: Color, val overdueDot: Color,
    val reject: Color, val rejectContainer: Color, val onRejectContainer: Color,
    /** onSurface — every mono figure. */
    val figure: Color,
    /** accent — one per screen, no more. */
    val heroFigure: Color
)

/**
 * The surface, outline and text tones the M3 scheme has no slot for. Chart segments live here too:
 * the settle-time histogram and the cost bar step through the work and money families by height,
 * which is a use of the family, not a decoration.
 */
data class ClearedTones(
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val navBar: Color,
    val divider: Color,
    val outlineCard: Color,
    val outlineField: Color,
    val outlineButton: Color,
    /** Prospect chip and prospect card. Dashed, 1 dp — never filled. */
    val outlineDashed: Color,
    val chipBg: Color,
    val onSurfaceStrong: Color,
    val onSurfaceVariant2: Color,
    val onSurfaceVariant3: Color,
    val label: Color,
    val tertiary: Color,
    val tertiary2: Color,
    val tertiary3: Color,
    /** Zero-value hero in the empty state, rank numerals. */
    val ghost: Color,
    val accentPressed: Color,
    val accentTintHover: Color,
    val selectedRow: Color,
    val workMid: Color,
    val moneyMid: Color,
    val moneyPale: Color,
    val moneyTint: Color,
    val rejectOutline: Color,
    val rejectTint: Color
)

val LocalClearedSemantics = staticCompositionLocalOf<ClearedSemantics> { error("no semantics") }
val LocalClearedTones = staticCompositionLocalOf<ClearedTones> { error("no tones") }
val LocalClearedType = staticCompositionLocalOf { ClearedType() }

private val LightSemantics = ClearedSemantics(
    work = LWork, workContainer = LWorkContainer, onWorkContainer = LOnWorkContainer, workOutline = LWorkOutline,
    money = LMoney, moneyContainer = LMoneyContainer, onMoneyContainer = LOnMoneyContainer, moneyOutline = LMoneyOutline,
    overdue = LOverdue, overdueContainer = LOverdueContainer, overdueBar = LOverdueBar,
    offlineStrip = LOfflineStrip, onOfflineStrip = LOnOfflineStrip, overdueDot = LOverdueDot,
    reject = LReject, rejectContainer = LRejectContainer, onRejectContainer = LOnRejectContainer,
    figure = LOnSurface,
    heroFigure = LAccent
)

private val DarkSemantics = ClearedSemantics(
    work = DWork, workContainer = DWorkContainer, onWorkContainer = DOnWorkContainer, workOutline = DWorkOutline,
    money = DMoney, moneyContainer = DMoneyContainer, onMoneyContainer = DOnMoneyContainer, moneyOutline = DMoneyOutline,
    overdue = DOverdue, overdueContainer = DOverdueContainer, overdueBar = DOverdueBar,
    offlineStrip = DOfflineStrip, onOfflineStrip = DOnOfflineStrip, overdueDot = DOverdueDot,
    reject = DReject, rejectContainer = DRejectContainer, onRejectContainer = DOnRejectContainer,
    figure = DOnSurface,
    heroFigure = DAccent
)

private val LightTones = ClearedTones(
    surfaceLow = LSurfaceLow, surfaceContainer = LSurfaceContainer, surfaceHigh = LSurfaceHigh, navBar = LNavBar,
    divider = LDivider, outlineCard = LOutlineCard, outlineField = LOutlineField, outlineButton = LOutlineButton,
    outlineDashed = LOutlineDashed, chipBg = LSurfaceHigh,
    onSurfaceStrong = LOnSurfaceStrong, onSurfaceVariant2 = LOnSurfaceVariant2, onSurfaceVariant3 = LOnSurfaceVariant3,
    label = LLabel, tertiary = LTertiary, tertiary2 = LTertiary2, tertiary3 = LTertiary3, ghost = LGhost,
    accentPressed = LAccentPressed, accentTintHover = LAccentTintHover, selectedRow = LSelectedRow,
    workMid = LWorkMid, moneyMid = LMoneyMid, moneyPale = LMoneyPale, moneyTint = LMoneyTint,
    rejectOutline = LRejectOutline, rejectTint = LRejectTint
)

private val DarkTones = ClearedTones(
    surfaceLow = DSurfaceLow, surfaceContainer = DSurfaceLow, surfaceHigh = DSurfaceHigh, navBar = DNavBar,
    divider = DOutline, outlineCard = DOutline, outlineField = DOutlineField, outlineButton = DOutlineButton,
    outlineDashed = DOutlineDashed, chipBg = DChipBg,
    onSurfaceStrong = DOnSurfaceStrong, onSurfaceVariant2 = DOnSurfaceVariant2, onSurfaceVariant3 = DOnSurfaceVariant3,
    label = DLabel, tertiary = DTertiary, tertiary2 = DTertiary2, tertiary3 = DTertiary3, ghost = DGhost,
    accentPressed = DAccentPressed, accentTintHover = DAccentTintHover, selectedRow = DSelectedRow,
    workMid = DWorkMid, moneyMid = DMoneyMid, moneyPale = DMoneyPale, moneyTint = DMoneyTint,
    rejectOutline = DRejectOutline, rejectTint = DRejectTint
)

/**
 * Every slot is set explicitly. An unset slot falls back to the M3 baseline purple, which would put
 * a violet that means nothing next to a violet that means "work".
 *
 * `surfaceTint` is transparent in both schemes: that single value is what actually disables tonal
 * elevation overlays, so a Surface given an elevation cannot quietly tint itself.
 */
private val LightScheme = lightColorScheme(
    primary = LAccent,
    onPrimary = Color.White,
    primaryContainer = LAccentContainer,
    onPrimaryContainer = LOnAccentContainer,
    inversePrimary = DAccent,

    secondary = LAccent,
    onSecondary = Color.White,
    secondaryContainer = LAccentContainer,
    onSecondaryContainer = LOnAccentContainer,

    tertiary = LOnSurfaceVariant,
    onTertiary = Color.White,
    tertiaryContainer = LSurfaceHigh,
    onTertiaryContainer = LOnSurface,

    background = LBg,
    onBackground = LOnSurface,
    surface = LSurface,
    onSurface = LOnSurface,
    surfaceVariant = LSurfaceHigh,
    onSurfaceVariant = LOnSurfaceVariant,
    surfaceTint = Color.Transparent,

    surfaceBright = LSurface,
    surfaceDim = LSurfaceContainer,
    surfaceContainerLowest = LSurface,
    surfaceContainerLow = LSurfaceLow,
    surfaceContainer = LSurfaceContainer,
    surfaceContainerHigh = LSurfaceHigh,
    surfaceContainerHighest = LNavBar,

    inverseSurface = LOnSurface,
    inverseOnSurface = LBg,

    error = LReject,
    onError = Color.White,
    errorContainer = LRejectContainer,
    onErrorContainer = LOnRejectContainer,

    outline = LOutlineField,
    outlineVariant = LOutlineVariant,
    scrim = Color.Black
)

private val DarkScheme = darkColorScheme(
    primary = DAccent,
    onPrimary = DOnAccent,
    primaryContainer = DAccentContainer,
    onPrimaryContainer = DOnAccentContainer,
    inversePrimary = LAccent,

    secondary = DAccent,
    onSecondary = DOnAccent,
    secondaryContainer = DAccentContainer,
    onSecondaryContainer = DOnAccentContainer,

    tertiary = DOnSurfaceVariant,
    onTertiary = DBg,
    tertiaryContainer = DSurfaceHigh,
    onTertiaryContainer = DOnSurface,

    background = DBg,
    onBackground = DOnSurface,
    surface = DSurface,
    onSurface = DOnSurface,
    surfaceVariant = DSurfaceHigh,
    onSurfaceVariant = DOnSurfaceVariant,
    surfaceTint = Color.Transparent,

    surfaceBright = DSurfaceHigh,
    surfaceDim = DBg,
    surfaceContainerLowest = DBg,
    surfaceContainerLow = DSurfaceLow,
    surfaceContainer = DSurface,
    surfaceContainerHigh = DSurfaceHigh,
    surfaceContainerHighest = DChipBg,

    inverseSurface = DOnSurface,
    inverseOnSurface = DBg,

    error = DReject,
    onError = DBg,
    errorContainer = DRejectContainer,
    onErrorContainer = DOnRejectContainer,

    outline = DOutlineField,
    outlineVariant = DOutlineVariant,
    scrim = Color.Black
)

/**
 * There is no `dynamicColor` parameter, and that is the point rather than an omission: the
 * work/money split *is* the product, so there is no supported configuration in which a wallpaper
 * repaints it.
 */
@Composable
fun ClearedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(
        LocalClearedSemantics provides if (darkTheme) DarkSemantics else LightSemantics,
        LocalClearedTones provides if (darkTheme) DarkTones else LightTones,
        LocalClearedType provides ClearedType()
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ClearedTypography,
            shapes = ClearedShapes,
            content = content
        )
    }
}

/**
 * Shorthand so screens read as `Cleared.semantics.work` rather than an unqualified local.
 */
object Cleared {
    val semantics: ClearedSemantics
        @Composable @ReadOnlyComposable get() = LocalClearedSemantics.current
    val tones: ClearedTones
        @Composable @ReadOnlyComposable get() = LocalClearedTones.current
    val type: ClearedType
        @Composable @ReadOnlyComposable get() = LocalClearedType.current
}

/**
 * Elevation is zero everywhere. Separation comes from 1 dp hairlines and 8 dp spacer bands, never
 * from a shadow or a tonal overlay, and cards get an outline instead.
 *
 * These defaults exist so that is one call rather than three properties a screen can forget.
 */
object ClearedElevation {
    val None: Dp = 0.dp
}

object ClearedCard {
    @Composable
    fun border(): BorderStroke = BorderStroke(1.dp, Cleared.tones.outlineCard)

    @Composable
    fun colors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    @Composable
    fun elevation(): CardElevation = CardDefaults.cardElevation(
        defaultElevation = ClearedElevation.None,
        pressedElevation = ClearedElevation.None,
        focusedElevation = ClearedElevation.None,
        hoveredElevation = ClearedElevation.None,
        draggedElevation = ClearedElevation.None,
        disabledElevation = ClearedElevation.None
    )
}
