# Design tokens — Cleared

All colours were authored in OKLCh and converted to sRGB hex here. Use the hex values.

Contrast was checked against the surface each token sits on; do not lighten the secondary and
tertiary text tones — this app is read outdoors.

---

## 1. Colour — light theme

### Surfaces and outlines

| Token | Hex | Used for |
| --- | --- | --- |
| `bg` | `#F7F9FA` | Screen background |
| `surface` | `#FFFFFF` | Cards, rows, fields |
| `surfaceContainerLow` | `#F4F6F8` | Row hover, subtotal row on the wallet card |
| `surfaceContainer` | `#F2F4F5` | Week group headers on Pipeline |
| `surfaceContainerHigh` | `#EFF0F2` | 8 dp spacer bands between sections, inline chip tracks |
| `navBar` | `#EFF1F4` | Bottom navigation bar and gesture area |
| `outlineVariant` | `#E6E8EA` | Table-row rules inside cards |
| `divider` | `#E4E6E9` | List row separators |
| `outlineCard` | `#DFE1E4` | Card and section borders (1 dp) |
| `outlineField` | `#D2D4D7` | Text-field and input borders |
| `outlineButton` | `#C8CBCE` | Outlined button borders |
| `outlineDashed` | `#B4B8BB` | Prospect chip and prospect card, **dashed** 1 dp |

### Text

| Token | Hex | Used for |
| --- | --- | --- |
| `onSurface` | `#1C2024` | Titles, row primary text, figures |
| `onSurfaceStrong` | `#393E42` | Emphasised secondary figures |
| `onSurfaceVariant` | `#51565B` | Table labels, body copy on cards |
| `onSurfaceVariant2` | `#5F6469` | Inactive nav labels, secondary body |
| `onSurfaceVariant3` | `#65696F` | Supporting copy under figures |
| `label` | `#6D7277` | Section overlines (uppercase) |
| `tertiary` | `#777B7F` | Small labels in stat columns |
| `tertiary2` | `#83878B` | Captions, KES equivalents under row amounts |
| `tertiary3` | `#8F9397` | Annotations on the components sheet |
| `ghost` | `#A2A5A8` | Zero-value hero figure in the empty state, rank numerals |

### Accent — primary figure and primary actions **only**

| Token | Hex |
| --- | --- |
| `accent` | `#1364B0` |
| `accentPressed` | `#0052A1` |
| `accentContainer` | `#CBE2FD` |
| `onAccentContainer` | `#003363` |
| `accentTintHover` | `#E7F1FE` |
| `selectedRow` | `#EAF4FF` |

### Work phase — violet

| Token | Hex | Used for |
| --- | --- | --- |
| `work` | `#8364C8` | 3 dp row rail, timeline dot, legend swatch |
| `workContainer` | `#EEE9FF` | Stage chip background |
| `onWorkContainer` | `#503484` | Stage chip text, phase overline |
| `workOutline` | `#D2CDE3` | Timeline rule, unselected work-stage chip border |
| `workMid` | `#AF9FDE` | Mid bars in the settle-time histogram |

### Money phase — green

| Token | Hex | Used for |
| --- | --- | --- |
| `money` | `#25855B` | 3 dp row rail, timeline dot, legend swatch |
| `moneyContainer` | `#D4F7E3` | Stage chip background, "cheapest route" card |
| `onMoneyContainer` | `#004F2C` | Stage chip text, cleared figure, phase overline |
| `moneyOutline` | `#BED8C9` | Timeline rule, unselected money-stage chip border |
| `moneyMid` | `#64AA85` | Second segment of the cost-breakdown bar |
| `moneyPale` | `#A9CFB9` | Third segment of the cost-breakdown bar |
| `moneyTint` | `#F3FCF6` | "Bigger saving available" card background |

### Overdue and offline — amber

| Token | Hex | Used for |
| --- | --- | --- |
| `overdue` | `#894800` | Overdue age-pill text, "short by" figure, p90 figure |
| `overdueContainer` | `#FFECC9` | Overdue age-pill background |
| `overdueBar` | `#F3DAB2` | Unpaid-hours portion of the platform hour bar |
| `overdueDot` | `#BF7600` | Offline strip dot |
| `offlineStrip` | `#FFEECB` | Offline strip background |
| `onOfflineStrip` | `#7F4400` | Offline strip text |

### Rejected — red

| Token | Hex | Used for |
| --- | --- | --- |
| `reject` | `#DB4241` | 3 dp row rail on a rejected record |
| `rejectContainer` | `#FFE9E7` | Rejected chip background, warning blocks |
| `onRejectContainer` | `#B02A2D` | Rejected chip text, "no payout", below-median rates |
| `rejectOutline` | `#F0C5C1` | Conflict card border |
| `rejectTint` | `#FFF8F7` | Conflict card background |

---

## 2. Colour — dark theme

### Surfaces and outlines

| Token | Hex | Used for |
| --- | --- | --- |
| `bg` | `#111315` | Screen background |
| `surface` | `#1C1E21` | Cards, rows, fields |
| `surfaceContainerLow` | `#15171A` | Week group headers, caption strips |
| `navBar` | `#171A1C` | Bottom navigation bar and gesture area |
| `surfaceContainerHigh` | `#232629` | Subtotal row on the wallet card |
| `chipBg` | `#282A2D` | Inline segmented-control track, neutral age pill |
| `outlineVariant` | `#292C2F` | Table-row rules inside cards |
| `outline` | `#2B2E32` | Card borders, list separators |
| `outlineField` | `#3A3D41` | Text-field borders |
| `outlineButton` | `#4A4D51` | Outlined button borders |
| `outlineDashed` | `#5F6469` | Prospect chip and card, **dashed** 1 dp |

### Text

| Token | Hex |
| --- | --- |
| `onSurface` | `#F0F2F4` |
| `onSurfaceVariant` | `#BABEC3` |
| `onSurfaceVariant2` | `#ADB1B6` |
| `onSurfaceVariant3` | `#A1A5A9` |
| `label` | `#95999D` |
| `tertiary` | `#83878B` |
| `tertiary2` | `#7D8185` |
| `ghost` | `#66696D` |

### Accent

| Token | Hex |
| --- | --- |
| `accent` | `#71B5FF` |
| `onAccent` | `#021429` |
| `accentPressed` | `#89C9FF` |
| `accentContainer` | `#17395D` |
| `onAccentContainer` | `#C2E1FF` |
| `selectedRow` | `#1C2937` |

### Work phase

| Token | Hex |
| --- | --- |
| `work` | `#A086DF` |
| `workContainer` | `#342B4A` |
| `onWorkContainer` | `#D9C9FF` |
| `workOutline` | `#4B3F69` |
| `workLabel` | `#C3B1F7` |

### Money phase

| Token | Hex |
| --- | --- |
| `money` | `#53AD81` |
| `moneyContainer` | `#143A28` |
| `onMoneyContainer` | `#9FE7C0` |
| `moneyOutline` | `#28523C` |
| `moneyLabel` | `#82D2A8` |
| `moneyFigure` | `#80DAAC` |

### Overdue and offline

| Token | Hex |
| --- | --- |
| `overdue` | `#F7C97B` |
| `overdueContainer` | `#483213` |
| `offlineStrip` | `#3C2A13` |
| `onOfflineStrip` | `#ECD4AB` |
| `overdueDot` | `#E8B45E` |

### Rejected

| Token | Hex |
| --- | --- |
| `reject` | `#E45D58` |
| `rejectContainer` | `#4C2422` |
| `onRejectContainer` | `#FFA8A0` |
| `rejectOutline` | `#773733` |
| `rejectTint` | `#2D1B19` |

---

## 3. `Color.kt` — paste-ready

```kotlin
package app.cleared.ui.theme

import androidx.compose.ui.graphics.Color

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
val LLabel             = Color(0xFF6D7277)
val LTertiary          = Color(0xFF777B7F)
val LTertiary2         = Color(0xFF83878B)
val LTertiary3         = Color(0xFF8F9397)
val LGhost             = Color(0xFFA2A5A8)
val LAccent            = Color(0xFF1364B0)
val LAccentPressed     = Color(0xFF0052A1)
val LAccentContainer   = Color(0xFFCBE2FD)
val LOnAccentContainer = Color(0xFF003363)
val LSelectedRow       = Color(0xFFEAF4FF)
val LWork              = Color(0xFF8364C8)
val LWorkContainer     = Color(0xFFEEE9FF)
val LOnWorkContainer   = Color(0xFF503484)
val LWorkOutline       = Color(0xFFD2CDE3)
val LMoney             = Color(0xFF25855B)
val LMoneyContainer    = Color(0xFFD4F7E3)
val LOnMoneyContainer  = Color(0xFF004F2C)
val LMoneyOutline      = Color(0xFFBED8C9)
val LOverdue           = Color(0xFF894800)
val LOverdueContainer  = Color(0xFFFFECC9)
val LOverdueBar        = Color(0xFFF3DAB2)
val LOverdueDot        = Color(0xFFBF7600)
val LOfflineStrip      = Color(0xFFFFEECB)
val LOnOfflineStrip    = Color(0xFF7F4400)
val LReject            = Color(0xFFDB4241)
val LRejectContainer   = Color(0xFFFFE9E7)
val LOnRejectContainer = Color(0xFFB02A2D)

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
val DTertiary          = Color(0xFF83878B)
val DTertiary2         = Color(0xFF7D8185)
val DGhost             = Color(0xFF66696D)
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
val DMoney             = Color(0xFF53AD81)
val DMoneyContainer    = Color(0xFF143A28)
val DOnMoneyContainer  = Color(0xFF9FE7C0)
val DMoneyOutline      = Color(0xFF28523C)
val DMoneyFigure       = Color(0xFF80DAAC)
val DOverdue           = Color(0xFFF7C97B)
val DOverdueContainer  = Color(0xFF483213)
val DOfflineStrip      = Color(0xFF3C2A13)
val DOnOfflineStrip    = Color(0xFFECD4AB)
val DOverdueDot        = Color(0xFFE8B45E)
val DReject            = Color(0xFFE45D58)
val DRejectContainer   = Color(0xFF4C2422)
val DOnRejectContainer = Color(0xFFFFA8A0)
```

The four semantic families (work, money, overdue, reject) are **not** part of `ColorScheme`. Expose
them through a `CompositionLocal`:

```kotlin
data class ClearedSemantics(
    val work: Color, val workContainer: Color, val onWorkContainer: Color, val workOutline: Color,
    val money: Color, val moneyContainer: Color, val onMoneyContainer: Color, val moneyOutline: Color,
    val overdue: Color, val overdueContainer: Color, val overdueBar: Color,
    val offlineStrip: Color, val onOfflineStrip: Color, val overdueDot: Color,
    val reject: Color, val rejectContainer: Color, val onRejectContainer: Color,
    val figure: Color,            // onSurface — every mono figure
    val heroFigure: Color         // accent — one per screen, no more
)

val LocalClearedSemantics = staticCompositionLocalOf<ClearedSemantics> { error("no semantics") }
```

**Disable dynamic colour.** `dynamicColor = false`. The work/money split is the product; a wallpaper
must not repaint it.

---

## 4. Typography

IBM Plex Sans (UI) + IBM Plex Mono (all figures). Bundle both; do not rely on downloadable fonts —
the app must work offline.

| Role | Family | Size | Weight | Tracking | Line height |
| --- | --- | --- | --- | --- | --- |
| Screen title (top app bar) | Sans | 22 sp | 600 | −0.012 em | 1.2 |
| Pushed-screen title | Sans | 17 sp | 600 | 0 | 1.2 |
| **Hero figure** | **Mono** | **40 sp** | **600** | **−0.02 em** | 1.0 |
| Hero currency prefix | Mono | 15 sp | 500 | +0.02 em | 1.0 |
| Section figure | Mono | 34 / 29 sp | 600 | −0.022 em | 1.0 |
| Card title | Sans | 15 sp | 600 | 0 | 1.2 |
| Listing title | Sans | 14.5–17 sp | 600 | 0 | 1.3 |
| Row primary | Sans | 14.5 sp | 500 | 0 | 1.25 |
| Row figure | Mono | 14.5 sp | 500 | 0 | 1.25 |
| Body | Sans | 13 sp | 400 | 0 | 1.55 |
| Table row | Sans | 13 sp | 400 | 0 | 1.4 |
| Secondary | Sans | 12.5 sp | 400 | 0 | 1.5 |
| Caption | Sans | 11.5 sp | 400 | 0 | 1.45 |
| Section overline | Sans | 11 sp | 600 | +0.07 em | 1.0 — UPPERCASE |
| Phase overline | Sans | 10 sp | 600 | +0.08 em | 1.0 — UPPERCASE |
| Stage chip | Sans | 10.5 sp | 600 | +0.01 em | 1.0 |
| Age pill | Mono | 10.5 sp | 500 | 0 | 1.0 |
| Micro annotation | Mono | 9.5–10 sp | 400 | +0.04 em | 1.2 |
| Nav label — 4 tabs | Sans | 11 sp | 600 active / 400 inactive | 0 | 1.0 |
| Nav label — 5 tabs | Sans | 10.5 sp | 600 active / 400 inactive | 0 | 1.0 |

**Figure rules**

- Every number is Mono, right-aligned in its column, and never truncated.
- Currency code precedes the amount with a single space: `KES 247,119`.
- KES: 0 decimals. USD / EUR: 2 decimals. Rates: 2 decimals. Percentages: 1–2 decimals.
- Negative amounts use a minus sign, not parentheses: `−EUR 32.00`.

---

## 5. Spacing, radius, sizing

**Spacing scale (dp):** 2 · 4 · 6 · 7 · 8 · 9 · 10 · 11 · 12 · 14 · 16 · 18 · 20 · 22 · 26 · 28

| Measure | Value |
| --- | --- |
| Screen horizontal gutter | 20 dp |
| Card padding | 14–16 dp (15 dp typical) |
| List-row vertical padding | 12 dp |
| Gap between cards | 12 dp |
| Section spacing | 22 dp |
| Spacer band between sections | 8 dp, in `surfaceContainerHigh`, with a 1 dp rule top and bottom |

| Radius | Value |
| --- | --- |
| Stage chip, age pill | 5 dp |
| Filter / selection chip, small tile | 8–9 dp |
| Card, field, sheet input | 12 dp |
| Bottom sheet top corners | 26 dp |
| Button, FAB-adjacent pill | fully rounded (height ÷ 2) |
| FAB | 18 dp (M3 large-FAB shape) |
| Checkbox | 4 dp |

| Element | Size |
| --- | --- |
| Frame | 390 × 844 dp |
| Status bar | 34 dp |
| Top app bar | 48 dp (title only) / 56 dp (with back) |
| Offline strip | 32 dp, full-bleed |
| Bottom navigation | 64 dp + 22 dp gesture area |
| Nav active indicator | 60 × 30 dp (4 tabs) / 52 × 30 dp (5 tabs) |
| FAB | 60 × 60 dp, 16 dp from the right, 104 dp from the bottom |
| Filled button | 52 dp tall, fully rounded |
| Outlined button | 44–48 dp tall |
| Row phase rail | 3 dp wide × 36 dp tall, 2 dp radius |
| Progress / split bars | 5–6 dp tall, 3 dp radius |
| Toggle | 44 × 26 dp, 18 dp knob |
| Minimum touch target | **48 dp** |

**Elevation: 0 everywhere.** No shadows, no tonal elevation overlays. Set
`shadowElevation = 0.dp` and `tonalElevation = 0.dp` on every `Surface`, `Card` and
`NavigationBar`, and give cards a 1 dp `BorderStroke` instead.
