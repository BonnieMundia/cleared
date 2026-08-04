package app.cleared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.cleared.R

/**
 * IBM Plex Sans for UI, IBM Plex Mono for every figure — money, dates, durations, percentages, ids.
 *
 * Both are bundled as `res/font` resources rather than downloadable fonts: the app has to render
 * correctly with no network, and every column of figures in it has to line up.
 */
val PlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold)
)

/**
 * Font padding off and line height trimmed to the first/last baseline, so a figure's box is its
 * glyphs. Without this a 40 sp hero and an 11 sp caption do not sit where the spec's dp offsets
 * say they should, and columns of figures drift.
 */
@Suppress("DEPRECATION")
private val Flush = PlatformTextStyle(includeFontPadding = false)

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun sans(
    size: Double,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Double = 1.4,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = PlexSans,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.em,
    platformStyle = Flush,
    lineHeightStyle = Trim
)

/**
 * Every figure style routes through here, so tabular figures are not something a call site can
 * forget. Plex Mono is monospaced already; `tnum` is belt and braces, and is what keeps columns
 * aligned if the UI face is ever swapped for Roboto as DESIGN_TOKENS.md allows.
 */
private fun mono(
    size: Double,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Double = 1.0,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = PlexMono,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = "tnum",
    platformStyle = Flush,
    lineHeightStyle = Trim
)

/**
 * The type scale from design/DESIGN_TOKENS.md §4, by the role it is named for rather than by an
 * M3 slot. The roles are the spec's own vocabulary — `heroFigure`, `agePill`, `phaseOverline` — so
 * a screen can be read against the document without translation.
 */
data class ClearedType(
    /** Top app bar, title only. */
    val screenTitle: TextStyle = sans(22.0, FontWeight.SemiBold, 1.2, -0.012),
    /** Pushed screen, with a back chevron. */
    val pushedTitle: TextStyle = sans(17.0, FontWeight.SemiBold, 1.2),

    /** The one accent figure on a screen. */
    val heroFigure: TextStyle = mono(40.0, FontWeight.SemiBold, 1.0, -0.02),
    /** `KES` ahead of the hero amount, baseline-aligned. */
    val heroPrefix: TextStyle = mono(15.0, FontWeight.Medium, 1.0, 0.02),
    val sectionFigureLarge: TextStyle = mono(34.0, FontWeight.SemiBold, 1.0, -0.022),
    val sectionFigure: TextStyle = mono(29.0, FontWeight.SemiBold, 1.0, -0.022),

    val cardTitle: TextStyle = sans(15.0, FontWeight.SemiBold, 1.2),
    val listingTitle: TextStyle = sans(14.5, FontWeight.SemiBold, 1.3),

    /** The record row's platform name, and its amount. */
    val rowPrimary: TextStyle = sans(14.5, FontWeight.Medium, 1.25),
    val rowFigure: TextStyle = mono(14.5, FontWeight.Medium, 1.25),

    val body: TextStyle = sans(13.0, FontWeight.Normal, 1.55),
    val tableRow: TextStyle = sans(13.0, FontWeight.Normal, 1.4),
    val tableFigure: TextStyle = mono(13.0, FontWeight.Normal, 1.4),
    val secondary: TextStyle = sans(12.5, FontWeight.Normal, 1.5),
    val caption: TextStyle = sans(11.5, FontWeight.Normal, 1.45),
    val captionFigure: TextStyle = mono(11.5, FontWeight.Medium, 1.45),
    /** The KES equivalent under a row's amount. */
    val rowSubFigure: TextStyle = mono(11.0, FontWeight.Normal, 1.2),

    /** UPPERCASE. Week group headers, section overlines. */
    val sectionOverline: TextStyle = sans(11.0, FontWeight.SemiBold, 1.0, 0.07),
    /** UPPERCASE. `WORK PHASE` / `MONEY PHASE`. */
    val phaseOverline: TextStyle = sans(10.0, FontWeight.SemiBold, 1.0, 0.08),

    val stageChip: TextStyle = sans(10.5, FontWeight.SemiBold, 1.0, 0.01),
    val agePill: TextStyle = mono(10.5, FontWeight.Medium, 1.0),
    val microAnnotation: TextStyle = mono(9.5, FontWeight.Normal, 1.2, 0.04),

    val navLabelActive: TextStyle = sans(11.0, FontWeight.SemiBold, 1.0),
    val navLabelInactive: TextStyle = sans(11.0, FontWeight.Normal, 1.0),
    /** Discovery adds a fifth destination and the labels step down half a point. */
    val navLabelActiveFive: TextStyle = sans(10.5, FontWeight.SemiBold, 1.0),
    val navLabelInactiveFive: TextStyle = sans(10.5, FontWeight.Normal, 1.0)
)

/**
 * The Material 3 slots, so stock components inherit Plex Sans rather than Roboto. Screens should
 * reach for [ClearedType] by role; this exists to stop the baseline face leaking through a
 * component nobody restyled.
 */
val ClearedTypography = Typography(
    displayLarge = sans(40.0, FontWeight.SemiBold, 1.1, -0.02),
    displayMedium = sans(34.0, FontWeight.SemiBold, 1.1, -0.02),
    displaySmall = sans(29.0, FontWeight.SemiBold, 1.1, -0.02),
    headlineLarge = sans(22.0, FontWeight.SemiBold, 1.2, -0.012),
    headlineMedium = sans(20.0, FontWeight.SemiBold, 1.2),
    headlineSmall = sans(17.0, FontWeight.SemiBold, 1.2),
    titleLarge = sans(17.0, FontWeight.SemiBold, 1.2),
    titleMedium = sans(15.0, FontWeight.SemiBold, 1.2),
    titleSmall = sans(14.5, FontWeight.Medium, 1.25),
    bodyLarge = sans(14.5, FontWeight.Normal, 1.4),
    bodyMedium = sans(13.0, FontWeight.Normal, 1.55),
    bodySmall = sans(12.5, FontWeight.Normal, 1.5),
    labelLarge = sans(13.5, FontWeight.Medium, 1.2),
    labelMedium = sans(11.5, FontWeight.Medium, 1.2),
    labelSmall = sans(10.5, FontWeight.SemiBold, 1.0, 0.01)
)
