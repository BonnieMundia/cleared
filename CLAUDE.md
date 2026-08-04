# CLAUDE.md — Cleared

Persistent instructions for Claude Code working in this repository.
Place this file at the **root of the Android project** (next to `settings.gradle.kts`).

---

## What this project is

**Cleared** is an Android app that tracks the gap between work done and money landed for a remote
contractor in Kenya earning from several AI-training and freelance platforms plus his own small
business. Money arrives in USD and EUR through PayPal and Payoneer, converts to KES, and lands at
Equity Bank or M-Pesa.

It is not an invoicing app. It starts **before** the invoice and counts the unpaid hours —
assessments, calibration sets, onboarding — that never turn into income.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Single Activity, Compose Navigation
- Room for persistence, `WorkManager` for the sync queue
- Min SDK 26, target latest stable
- No backend for v1 except an FX-rate fetch. Discovery is stubbed until sources are decided.

## The design source of truth

Everything lives in `design/`:

| File | Read it for |
| --- | --- |
| `design/README.md` | Overview, stage model, screen list, build order, acceptance checks |
| `design/DESIGN_TOKENS.md` | Every colour as hex, a paste-ready `Color.kt`, the full type and spacing scale |
| `design/DATA_MODEL.md` | Entities, the append-only stage log, every derived figure with its formula |
| `design/SCREENS.md` | Component-by-component specs with exact copy |
| `design/sample_data.json` | Seed data with pre-computed totals to assert against in tests |
| `design/Cleared.dc.html` | The visual reference. Open it in a browser. **Never port this HTML.** |

Read `DATA_MODEL.md` before writing any code. If this file and `design/` disagree, `design/` wins.

---

## Hard rules — do not violate without asking

1. **A record's stage is derived from an append-only `StageEvent` log. Never store a `currentStage`
   column.** Corrections and conflict resolutions append new events; nothing is ever updated or
   deleted. This is what makes offline replay, conflict detection and free settle-time percentiles
   possible.

2. **Rejected records keep their logged hours.** They stay in the denominator of a platform's
   effective rate and contribute nothing to the numerator. Do not filter them out anywhere.

3. **Work phase is violet, money phase is green.** Never mix the families, never use either
   decoratively. Rejected is red, overdue is amber, and the blue accent appears **only** on the one
   primary figure per screen and on primary actions.

4. **Flat surfaces.** `shadowElevation = 0.dp` and `tonalElevation = 0.dp` on every `Surface`,
   `Card` and `NavigationBar`. Cards get a 1 dp border instead. No gradients anywhere.

5. **`dynamicColor = false`.** The work/money split is the product; a wallpaper must not repaint it.

6. **Every figure is IBM Plex Mono, right-aligned in its column.** Money always carries a
   three-letter currency code and never a symbol — `KES 247,119`, not `Ksh247119`. KES to zero
   decimals, USD and EUR to two. Negatives use a minus sign, never parentheses.

7. **Money is `BigDecimal` stored as scaled `Long` minor units. Never `Double`.**

8. **Overdue fires at the platform's own p90**, recomputed whenever a record lands — not a global
   constant and not a mean. Settle times are long-tailed; a mean flags nothing and hides everything.

9. **Landed records keep the FX rate they actually converted at, forever.** Only unlanded estimates
   move when the mid rate refreshes. Nothing is ever silently revalued.

10. **Offline is a condition, not an error.** Every screen except Discover works with no network.
    No blocking dialogs, no spinners over content, no disabled screens — a slim amber strip under the
    app bar with a queued-writes count, tappable through to the Sync screen.

11. **Undo, never confirm.** State-changing actions show an 8-second undo snackbar. No "are you
    sure?" dialogs.

12. **No images, no logos, no brand marks.** Platforms are referenced by plain text name — a product
    requirement. Icons come from Material Symbols only.

---

## Build order

Follow it. Each step depends on the one before.

1. Room schema, the `StageEvent` log, and every derived query in `DATA_MODEL.md` — with unit tests
   asserting against the figures in `sample_data.json`.
2. `Color.kt`, `Type.kt`, `Theme.kt` from `DESIGN_TOKENS.md`. **Light and dark from day one.**
   Semantic families (work / money / overdue / reject) go in a `CompositionLocal`, not `ColorScheme`.
3. The `RecordRow` composable in all seven stage variants. It is the atom of the app — Pipeline,
   bulk triage and the components sheet all reuse it. Write a Compose preview showing all seven in
   both themes before moving on.
4. Pipeline (`1a`): hero figure, phase-split bar, week grouping and subtotals, tap-to-advance.
5. Add-record bottom sheet (`1f`), then Record detail (`1e`).
6. Platforms (`1b`) and the statistics behind it.
7. Money (`1c`), then Tax (`1d`).
8. Connectivity strip, `SyncOp` queue, conflict UI (`2a`).
9. Settle-time percentiles (`2b`), bulk triage (`2d`), withdraw advisor (`2c`).
10. Discovery (`3a`, `3b`) last — it is the only feature needing a backend.

## Definition of done for any screen

- Compose previews exist for **light and dark**.
- Every figure is Mono and column-aligned.
- Body text and figures pass WCAG AA against their surface in both themes.
- All touch targets ≥ 48 dp.
- The screen is usable with the network off.
- Copy matches `SCREENS.md` **exactly** — the wording is designed, not filler.

## Testing

- Unit-test every derived figure against `design/sample_data.json`. Key assertions: `owedKes` is
  `247119`; work/money split is `188183` / `58936`; the three week subtotals are `64393`, `151730`,
  `30996`; Vector Annotate's effective rate is `402` and it is the only flagged platform; at USD 300
  the best withdrawal route is Payoneer → Equity Bank at `37560`.
- Test that advancing a record to `LANDED` removes it from `owedKes` and its week subtotal.
- Test that a rejected record still appears in its platform's hour total.
- Test that offline mutations replay in `SyncOp` id order and are idempotent under double-replay.
- Test that a `REVERSED` record contributes its hours to the platform's denominator, contributes zero
  to the numerator, and is excluded from `owedKes`.
- Test that a re-issue (`supersedesRecordId` set, `carriesHours = false`) adds money once and hours
  never.
- Test that a part-paid record counts only its **unlanded** settlements as owed, and that its week
  subtotal matches the remainder rather than the record total.

## Designed and ready to build

- **Payout reversed / bounced.** Terminal stage `REVERSED`. Schema in the *Reversal* section of
  `DATA_MODEL.md`; UI in `4a` and `4c` of `SCREENS.md`. Recovery is a new record linked by
  `supersedesRecordId`, never a return to an earlier stage.
- **Partial payments.** `Settlement` table hanging off the record — entity and derivation rules in
  `DATA_MODEL.md`; UI in `4b` and `4c` of `SCREENS.md`. Hours never split; the effective rate stays on
  the record.

## Not yet designed — ask before inventing

- **Discovery sources.** The design says "6 platform boards and 2 community feeds" without naming
  them. Stub Discovery behind a repository interface and seed it from `sample_data.json`.

## Working style

- Small, reviewable commits with a clear message.
- Do not add dependencies without asking — the charts are plain rectangles and need no library.
- Do not "improve" the design. If something looks wrong, say so and ask; the constraints are
  deliberate and most of them are load-bearing.
