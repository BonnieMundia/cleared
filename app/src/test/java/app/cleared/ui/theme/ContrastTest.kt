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

    /**
     * The one place a *primary* tone lands short: the uppercase week-group overline on Pipeline.
     *
     * `label` #6D7277 reaches 4.85:1 on plain surface, but the week header sits on
     * `surfaceContainer` #F2F4F5 and that drops it to 4.40:1 — a 2% shortfall against AA at 11 sp
     * SemiBold, which WCAG counts as normal text rather than large. Dark is unaffected: the same
     * overline measures well clear on both dark backgrounds.
     *
     * Darkening `label` to about #676C71 would clear it. That is a token change, so it is the
     * designer's call and this test records the current value rather than pre-empting it.
     */
    @Test
    fun `the week-group overline lands just under AA on its own header band`() {
        val onHeader = ratio(LLabel, LSurfaceContainer)
        assertTrue("label on surfaceContainer is %.2f:1".format(onHeader), onHeader in 4.3..4.5)
        assertAtLeast(4.5, "the same overline on plain surface", LLabel, LSurface)
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
     * The caption tier — `tertiary`, `tertiary2`, `tertiary3` and `ghost`, which carry the KES
     * equivalent under a row amount, stat-column labels and the components-sheet annotations.
     *
     * These sit at 11–11.5 sp, which WCAG counts as normal text, and they do **not** reach 4.5:1
     * against white: `tertiary` measures about 4.3:1, `tertiary2` about 3.3:1 and `tertiary3` about
     * 2.9:1. They clear the 3:1 large-text threshold but not the one README.md claims for them.
     *
     * DESIGN_TOKENS.md says explicitly "do not lighten the secondary and tertiary text tones", so
     * this test holds them at the line they actually meet rather than darkening them unilaterally.
     * Raising them is a design decision, not a build one.
     */
    @Test
    fun `the caption tier clears 3 to 1 but not AA for normal text`() {
        assertAtLeast(3.0, "light tertiary on surface", LTertiary, LSurface)
        assertAtLeast(3.0, "light tertiary2 on surface", LTertiary2, LSurface)
        assertAtLeast(2.8, "light tertiary3 on surface", LTertiary3, LSurface)

        assertAtLeast(3.0, "dark tertiary on surface", DTertiary, DSurface)
        assertAtLeast(3.0, "dark tertiary2 on surface", DTertiary2, DSurface)
        assertAtLeast(2.8, "dark tertiary3 on surface", DTertiary3, DSurface)

        // The shortfall, stated as a fact rather than left implicit.
        assertTrue(
            "tertiary2 reaching AA would mean the design brief had been overruled",
            ratio(LTertiary2, LSurface) < 4.5
        )
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
