package app.cleared.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * design/README.md: "Body text and figures meet WCAG AA against their surface in both themes; the
 * app is used outdoors in daylight."
 *
 * This checks that claim against the tokens rather than trusting it, and pins the tiers where the
 * palette lands short so the shortfall is visible rather than discovered on a phone in the sun.
 */
class ContrastTest {

    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun ratio(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        val light = maxOf(a, b)
        val dark = minOf(a, b)
        return (light + 0.05) / (dark + 0.05)
    }

    private fun assertAtLeast(min: Double, label: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue("$label is %.2f:1, under the required %.1f:1".format(r, min), r >= min)
    }

    /** AA for normal text. Everything a screen sets long-form copy or a row figure in. */
    @Test
    fun `body text and figures pass AA in light`() {
        assertAtLeast(4.5, "onSurface on surface", LOnSurface, LSurface)
        assertAtLeast(4.5, "onSurface on bg", LOnSurface, LBg)
        assertAtLeast(4.5, "onSurfaceStrong on surface", LOnSurfaceStrong, LSurface)
        assertAtLeast(4.5, "onSurfaceVariant on surface", LOnSurfaceVariant, LSurface)
        assertAtLeast(4.5, "onSurfaceVariant2 on navBar", LOnSurfaceVariant2, LNavBar)
        assertAtLeast(4.5, "onSurfaceVariant3 on surface", LOnSurfaceVariant3, LSurface)
        assertAtLeast(4.5, "label on surface", LLabel, LSurface)
        assertAtLeast(4.5, "accent hero on bg", LAccent, LBg)
    }

    @Test
    fun `body text and figures pass AA in dark`() {
        assertAtLeast(4.5, "onSurface on surface", DOnSurface, DSurface)
        assertAtLeast(4.5, "onSurface on bg", DOnSurface, DBg)
        assertAtLeast(4.5, "onSurfaceStrong on surface", DOnSurfaceStrong, DSurface)
        assertAtLeast(4.5, "onSurfaceVariant on surface", DOnSurfaceVariant, DSurface)
        assertAtLeast(4.5, "onSurfaceVariant2 on navBar", DOnSurfaceVariant2, DNavBar)
        assertAtLeast(4.5, "onSurfaceVariant3 on surface", DOnSurfaceVariant3, DSurface)
        assertAtLeast(4.5, "label on surface", DLabel, DSurface)
        assertAtLeast(4.5, "label on surfaceLow", DLabel, DSurfaceLow)
        assertAtLeast(4.5, "accent hero on bg", DAccent, DBg)
    }

    /** Every stage chip: its text against its own container, in both themes. */
    @Test
    fun `stage chips pass AA on their containers`() {
        assertAtLeast(4.5, "light work chip", LOnWorkContainer, LWorkContainer)
        assertAtLeast(4.5, "light money chip", LOnMoneyContainer, LMoneyContainer)
        assertAtLeast(4.5, "light overdue pill", LOverdue, LOverdueContainer)
        assertAtLeast(4.5, "light reject chip", LOnRejectContainer, LRejectContainer)
        assertAtLeast(4.5, "light offline strip", LOnOfflineStrip, LOfflineStrip)

        assertAtLeast(4.5, "dark work chip", DOnWorkContainer, DWorkContainer)
        assertAtLeast(4.5, "dark money chip", DOnMoneyContainer, DMoneyContainer)
        assertAtLeast(4.5, "dark overdue pill", DOverdue, DOverdueContainer)
        assertAtLeast(4.5, "dark reject chip", DOnRejectContainer, DRejectContainer)
        assertAtLeast(4.5, "dark offline strip", DOnOfflineStrip, DOfflineStrip)
    }

    /**
     * The supporting-text tier — `label`, `tertiary`, `tertiary2`, `tertiary3` — against **every**
     * surface it can land on, not just the flattering one.
     *
     * These carry the week-group overline, the KES equivalent under a row amount, the stat-column
     * labels and the components-sheet annotations, all at 11–11.5 sp. WCAG counts that as normal
     * text, so the bar is 4.5:1 and the worst surface is the one that decides: the 8 dp spacer band
     * in light, the chip track in dark.
     *
     * The four tones share one value per theme. See the note at the foot of Color.kt: eight
     * distinguishable AA-compliant greys do not fit in the band between legible and already-taken.
     */
    @Test
    fun `the supporting-text tier passes AA on every surface it sits on`() {
        val lightSurfaces = mapOf(
            "surface" to LSurface, "surfaceLow" to LSurfaceLow, "surfaceContainer" to LSurfaceContainer,
            "surfaceHigh" to LSurfaceHigh, "navBar" to LNavBar, "bg" to LBg
        )
        val darkSurfaces = mapOf(
            "surface" to DSurface, "surfaceLow" to DSurfaceLow, "surfaceHigh" to DSurfaceHigh,
            "chipBg" to DChipBg, "navBar" to DNavBar, "bg" to DBg
        )

        for ((name, bg) in lightSurfaces) {
            assertAtLeast(4.5, "light label on $name", LLabel, bg)
            assertAtLeast(4.5, "light tertiary on $name", LTertiary, bg)
            assertAtLeast(4.5, "light tertiary2 on $name", LTertiary2, bg)
            assertAtLeast(4.5, "light tertiary3 on $name", LTertiary3, bg)
        }
        for ((name, bg) in darkSurfaces) {
            assertAtLeast(4.5, "dark label on $name", DLabel, bg)
            assertAtLeast(4.5, "dark tertiary on $name", DTertiary, bg)
            assertAtLeast(4.5, "dark tertiary2 on $name", DTertiary2, bg)
            assertAtLeast(4.5, "dark tertiary3 on $name", DTertiary3, bg)
        }
    }

    /**
     * `ghost` is the one tone held to 3:1 rather than 4.5:1, because its only job is the 40 sp
     * zero-value hero in the empty state and that is large text.
     *
     * The test also pins that it does *not* reach AA, so a call site needing 4.5:1 cannot quietly
     * reach for it — the 11 sp rank numerals on the Platforms cards take `tertiary` instead.
     */
    @Test
    fun `ghost meets the large-text threshold and only that`() {
        assertAtLeast(3.0, "light ghost on bg", LGhost, LBg)
        assertAtLeast(3.0, "light ghost on surface", LGhost, LSurface)
        assertAtLeast(3.0, "dark ghost on bg", DGhost, DBg)
        assertAtLeast(3.0, "dark ghost on surface", DGhost, DSurface)

        assertTrue("ghost is a large-text tone; small text takes tertiary", ratio(LGhost, LBg) < 4.5)
        assertTrue("ghost is a large-text tone; small text takes tertiary", ratio(DGhost, DBg) < 4.5)
    }

    /** A rail has to be distinguishable from the surface it sits on, in both themes. */
    @Test
    fun `phase rails are visible against their row`() {
        assertAtLeast(3.0, "light work rail", LWork, LSurface)
        assertAtLeast(3.0, "light money rail", LMoney, LSurface)
        assertAtLeast(3.0, "light reject rail", LReject, LSurface)
        assertAtLeast(3.0, "dark work rail", DWork, DSurface)
        assertAtLeast(3.0, "dark money rail", DMoney, DSurface)
        assertAtLeast(3.0, "dark reject rail", DReject, DSurface)
    }
}
