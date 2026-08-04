package app.cleared.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every colour in the app, as hex, from design/DESIGN_TOKENS.md.
 *
 * The values were authored in OKLCh and converted to sRGB there; these are those conversions
 * verbatim. Nothing here is generated from a seed colour and nothing is tinted at runtime — the
 * work/money split is the product, and a wallpaper must not repaint it.
 *
 * Contrast was checked against the surface each token sits on. Do not lighten the secondary and
 * tertiary text tones: this app is read outdoors.
 */

// ── Light ────────────────────────────────────────────────────────────────
val LBg                = Color(0xFFF7F9FA)
val LSurface           = Color(0xFFFFFFFF)
val LSurfaceLow        = Color(0xFFF4F6F8)
val LSurfaceContainer  = Color(0xFFF2F4F5)
val LSurfaceHigh       = Color(0xFFEFF0F2)
val LNavBar            = Color(0xFFEFF1F4)
val LOutlineVariant    = Color(0xFFE6E8EA)
val LDivider           = Color(0xFFE4E6E9)
val LOutlineCard       = Color(0xFFDFE1E4)
val LOutlineField      = Color(0xFFD2D4D7)
val LOutlineButton     = Color(0xFFC8CBCE)
val LOutlineDashed     = Color(0xFFB4B8BB)
val LOnSurface         = Color(0xFF1C2024)
val LOnSurfaceStrong   = Color(0xFF393E42)
val LOnSurfaceVariant  = Color(0xFF51565B)
val LOnSurfaceVariant2 = Color(0xFF5F6469)
val LOnSurfaceVariant3 = Color(0xFF65696F)
// Raised to WCAG AA — see the note at the foot of this file. Was #6D7277 / #777B7F / #83878B /
// #8F9397 / #A2A5A8, which measured 4.40 / 3.94 / 3.34 / 2.86 / 2.34 against the surfaces they
// actually sit on.
val LLabel             = Color(0xFF696E73)
val LTertiary          = Color(0xFF696E73)
val LTertiary2         = Color(0xFF696E73)
val LTertiary3         = Color(0xFF696E73)
val LGhost             = Color(0xFF888B8E)
val LAccent            = Color(0xFF1364B0)
val LAccentPressed     = Color(0xFF0052A1)
val LAccentContainer   = Color(0xFFCBE2FD)
val LOnAccentContainer = Color(0xFF003363)
val LAccentTintHover   = Color(0xFFE7F1FE)
val LSelectedRow       = Color(0xFFEAF4FF)
val LWork              = Color(0xFF8364C8)
val LWorkContainer     = Color(0xFFEEE9FF)
val LOnWorkContainer   = Color(0xFF503484)
val LWorkOutline       = Color(0xFFD2CDE3)
val LWorkMid           = Color(0xFFAF9FDE)
val LMoney             = Color(0xFF25855B)
val LMoneyContainer    = Color(0xFFD4F7E3)
val LOnMoneyContainer  = Color(0xFF004F2C)
val LMoneyOutline      = Color(0xFFBED8C9)
val LMoneyMid          = Color(0xFF64AA85)
val LMoneyPale         = Color(0xFFA9CFB9)
val LMoneyTint         = Color(0xFFF3FCF6)
val LOverdue           = Color(0xFF894800)
val LOverdueContainer  = Color(0xFFFFECC9)
val LOverdueBar        = Color(0xFFF3DAB2)
val LOverdueDot        = Color(0xFFBF7600)
val LOfflineStrip      = Color(0xFFFFEECB)
val LOnOfflineStrip    = Color(0xFF7F4400)
val LReject            = Color(0xFFDB4241)
val LRejectContainer   = Color(0xFFFFE9E7)
val LOnRejectContainer = Color(0xFFB02A2D)
val LRejectOutline     = Color(0xFFF0C5C1)
val LRejectTint        = Color(0xFFFFF8F7)

// ── Dark ─────────────────────────────────────────────────────────────────
val DBg                = Color(0xFF111315)
val DSurface           = Color(0xFF1C1E21)
val DSurfaceLow        = Color(0xFF15171A)
val DNavBar            = Color(0xFF171A1C)
val DSurfaceHigh       = Color(0xFF232629)
val DChipBg            = Color(0xFF282A2D)
val DOutlineVariant    = Color(0xFF292C2F)
val DOutline           = Color(0xFF2B2E32)
val DOutlineField      = Color(0xFF3A3D41)
val DOutlineButton     = Color(0xFF4A4D51)
val DOutlineDashed     = Color(0xFF5F6469)
val DOnSurface         = Color(0xFFF0F2F4)
val DOnSurfaceVariant  = Color(0xFFBABEC3)
val DOnSurfaceVariant2 = Color(0xFFADB1B6)
val DOnSurfaceVariant3 = Color(0xFFA1A5A9)
val DLabel             = Color(0xFF95999D)
// Raised to WCAG AA. Was #83878B / #7D8185 / #66696D, which measured 4.15 / 3.89 / 2.72 against
// the darkest surface each sits on. `label` needs no change in dark.
val DTertiary          = Color(0xFF8D9195)
val DTertiary2         = Color(0xFF8D9195)
val DGhost             = Color(0xFF707377)
val DAccent            = Color(0xFF71B5FF)
val DOnAccent          = Color(0xFF021429)
val DAccentPressed     = Color(0xFF89C9FF)
val DAccentContainer   = Color(0xFF17395D)
val DOnAccentContainer = Color(0xFFC2E1FF)
val DSelectedRow       = Color(0xFF1C2937)
val DWork              = Color(0xFFA086DF)
val DWorkContainer     = Color(0xFF342B4A)
val DOnWorkContainer   = Color(0xFFD9C9FF)
val DWorkOutline       = Color(0xFF4B3F69)
val DWorkLabel         = Color(0xFFC3B1F7)
val DMoney             = Color(0xFF53AD81)
val DMoneyContainer    = Color(0xFF143A28)
val DOnMoneyContainer  = Color(0xFF9FE7C0)
val DMoneyOutline      = Color(0xFF28523C)
val DMoneyLabel        = Color(0xFF82D2A8)
val DMoneyFigure       = Color(0xFF80DAAC)
val DOverdue           = Color(0xFFF7C97B)
val DOverdueContainer  = Color(0xFF483213)
val DOfflineStrip      = Color(0xFF3C2A13)
val DOnOfflineStrip    = Color(0xFFECD4AB)
val DOverdueDot        = Color(0xFFE8B45E)
val DReject            = Color(0xFFE45D58)
val DRejectContainer   = Color(0xFF4C2422)
val DOnRejectContainer = Color(0xFFFFA8A0)
val DRejectOutline     = Color(0xFF773733)
val DRejectTint        = Color(0xFF2D1B19)

// ── Dark values design/DESIGN_TOKENS.md does not give ─────────────────────
//
// The light tables carry seven tokens the dark tables do not, and every one of them is load-bearing
// for a screen that has to exist in both themes: the unpaid-hours track on the Platforms bar, the
// three segments of the cost-of-getting-paid bar, the "bigger saving available" card, the
// emphasised secondary figure tone, and the accent hover tint.
//
// These are derived by holding the light relationship — same hue, stepped toward the dark surface
// instead of toward white — and are the only colours in this file that are not transcribed.
// Flagged for the designer to overwrite.
val DOnSurfaceStrong   = Color(0xFFD5D9DE) // between DOnSurface and DOnSurfaceVariant
val DTertiary3         = Color(0xFF8D9195) // collapsed into the AA caption tone with tertiary/2
val DAccentTintHover   = Color(0xFF12243A) // pale accent tint, inverted
val DWorkMid           = Color(0xFF7A66A8) // between DWorkOutline and DWork
val DMoneyMid          = Color(0xFF3D7F5F) // second segment of the cost bar
val DMoneyPale         = Color(0xFF2C5A44) // third segment of the cost bar
val DMoneyTint         = Color(0xFF12241B) // "bigger saving available" card background
val DOverdueBar        = Color(0xFF5A431E) // unpaid-hours portion of the platform hour bar

// ── Why the supporting-text ramp is shorter than DESIGN_TOKENS.md's ───────
//
// The tables give eight steps of supporting text, from `onSurfaceVariant` down to `ghost`. They are
// used at 11–11.5 sp, which WCAG counts as normal text and therefore holds to 4.5:1. Measured
// against the surfaces they actually sit on, only the first four steps clear that bar: `label`
// reached 4.40:1, `tertiary` 3.94, `tertiary2` 3.34, `tertiary3` 2.86 and `ghost` 2.34.
//
// The band between "just legible" and "already used by onSurfaceVariant3" is about one step wide,
// so eight distinguishable AA-compliant greys do not exist to be had. DESIGN_TOKENS.md states the
// tie-breaker itself — "this app is read outdoors" — so legibility wins and the ramp loses steps:
// `label`, `tertiary`, `tertiary2` and `tertiary3` collapse onto one compliant tone per theme.
//
// The token names are all kept, so call sites and the design vocabulary are unchanged and a future
// re-spacing is a one-line edit here. The visible consequence is that captions and the KES
// equivalents under row amounts read a little stronger than the prototype shows them.
//
// `ghost` is the exception: it is the 40 sp zero-value hero in the empty state, which is large text
// and holds to 3:1 rather than 4.5:1. It is set to exactly that and must not be used for the 11 sp
// rank numerals on the Platforms cards — those take `tertiary`.
