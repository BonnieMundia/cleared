# Handoff: Cleared — income pipeline tracker (Android / Jetpack Compose)

## Overview

Cleared tracks the gap between **work done** and **money landed** for a remote contractor in Kenya who
earns from several AI-training and freelance platforms at once plus a small business he owns. Money
arrives in USD and EUR through PayPal and Payoneer, converts to KES, and lands at Equity Bank or
M-Pesa.

The thing that makes it different from every other income tracker: it starts **before** the invoice.
It counts the unpaid hours — assessments, calibration sets, onboarding — that never turn into income,
and divides them into everything a platform has ever paid, producing a single honest number:
**effective KES per hour**.

Target: Android, Material 3, Jetpack Compose. Designed at 390 × 844 dp. Light and dark for every screen.

## About the design files

`Cleared.dc.html` in this bundle is a **design reference**, not production code. It is an HTML/CSS
prototype that shows intended layout, colour, type, and behaviour. **Do not port the HTML.** The task
is to rebuild these screens natively in Jetpack Compose using Material 3 components and the token
values documented here.

Where the prototype and this README disagree, this README wins — it has been converted to the units
and colour space the app will actually use.

Open `Cleared.dc.html` in any browser. It is a canvas of labelled frames; scroll or pan around it.
Frames are numbered `1a`…`3b`; those ids are used throughout this document.

## Fidelity

**High fidelity.** Colours, type scale, spacing, and copy are final. Rebuild pixel-accurately.
Sample data is realistic but illustrative — replace with real records.

## Companion documents

| File | Contents |
| --- | --- |
| `DESIGN_TOKENS.md` | Every colour as hex, ready to paste into `Color.kt`; type scale; spacing, radius, sizing |
| `DATA_MODEL.md` | Entities, the append-only stage log, every derived figure and how to compute it |
| `SCREENS.md` | Per-screen layout, component-by-component, with exact copy |
| `sample_data.json` | Seed data matching the prototype exactly |
| `Cleared.dc.html`, `support.js` | The design reference itself |

---

## The stage model — read this first

Everything in the app hangs off one idea: a record moves through two phases, and **the boundary
between effort and money must be visible at a glance**.

```
PROSPECT ──► SUBMITTED ──► IN_REVIEW ──► APPROVED ──► PAYOUT_ISSUED ──► RECEIVED ──► LANDED
 (pre)      └──────────── WORK PHASE ────────────┘   └────────── MONEY PHASE ──────────┘
                              │
                              └──► REJECTED  (terminal — leaves work phase, never becomes money)
```

- **Work phase is violet.** Effort that has not become money.
- **Money phase is green.** A payout exists somewhere in the chain.
- **Rejected is red.** Terminal. **The logged hours are kept** and still drag the platform's effective
  rate down. This is the point of the app; do not "clean up" rejected records.
- **Prospect** is a pre-stage introduced by the Discovery feature (frame `3b`). Rendered as a
  *dashed outline* chip, never filled — it is not yet work. Hours start counting the moment a
  prospect is tracked.

One accent colour (blue) is used **only** for the primary figure on a screen and for primary actions.
Violet, green, amber and red are semantic and never decorative.

---

## Screens

Nine destinations-worth of UI. Full specs in `SCREENS.md`; summary here.

### Bottom navigation

Four destinations in the base design (`1a`–`1d`): **Pipeline · Platforms · Money · Tax**.
Discovery (turn 3) adds a fifth: **Pipeline · Platforms · Discover · Money · Tax**.
M3 permits 3–5. If the product decides to keep four, the destination to fold is **Tax** — it is
consulted monthly, not daily, and sits naturally as a tab inside Money. Build the nav data-driven so
this is a one-line change.

Record detail, Add record, Sync, Settle-time, Withdraw advisor and Listing detail are **pushed or
modal**, never tabs.

| Frame | Screen | Purpose |
| --- | --- | --- |
| `1a` | **Pipeline** (home) | Hero figure of total owed in KES with USD/EUR components; records grouped by expected arrival week; tap a row to advance its stage |
| `1b` | **Platforms** | One card per platform; effective KES/hour as the headline; sortable; a bad platform is obvious at a glance |
| `1c` | **Money** | Idle wallet balances, year-to-date cost of getting paid, withdrawal-route calculator |
| `1d` | **Tax** | Deliberately plain. Personal and company income kept apart, running set-aside, CSV export |
| `1e` | **Record detail** | Every stage transition with timestamp, the rate applied, each fee, final KES cleared |
| `1f` | **Add record** | Bottom sheet. Platform, amount, currency, hours, stage — under ten seconds |
| `1g` | **Empty state** | First run. Teaches the stage model once, instead of an onboarding carousel |
| `1h` | **Components sheet** | Record row in all seven stage variants, stage chips, age pill, offline strip, figure scale |
| `2a` | **Sync** | Offline queue, conflict resolution, retry state, rate staleness, bytes to send |
| `2b` | **Settle-time model** | Per-platform distribution; overdue is p90, not an average; time spent per stage |
| `2c` | **Withdraw advisor** | Cost by withdrawal size, break-even alerts, FX exposure, payout-destination advice |
| `2d` | **Bulk triage** | Multi-select rows and advance them together; swipe affordance; undo, not confirm |
| `3a` | **Discover** | Open work found on public boards, priced in projected effective KES/hour |
| `3b` | **Listing detail** | The projection shown as arithmetic; "Track as prospect" |

### States that must exist

- **Empty** — first-time user with no records (`1g`)
- **Overdue** — a record past its platform's p90 settle time; amber age pill (`1a`, rows 5 and 6)
- **Offline** — a persistent condition, not an error. Slim amber strip under the app bar with a
  queued-writes count, tappable through to Sync (`1a` dark, `2a`)
- **Rejected** — exits the work phase without becoming money; struck-through amount, red rail (`1a`, last row)
- **Conflict** — local edit disagrees with platform state (`2a`)

---

## Interactions & behaviour

| Interaction | Behaviour |
| --- | --- |
| Tap a pipeline row | Advances one stage. Hero figure, phase-split bar and week subtotals all recompute. A record reaching `LANDED` drops out of "owed". Rejected and landed rows do not advance. |
| Long-press a pipeline row | Enters selection mode → contextual app bar in `accentContainer`, checkboxes on rows, action bar at the bottom (`2d`) |
| Swipe a row right | Advance one stage. Swipe left opens the record. |
| Any destructive or state-changing action | **Undo snackbar for 8 seconds. Never a confirmation dialog.** |
| Sort chips on Platforms | Effective rate (default, desc) · Total paid · Approval · Days to land |
| Amount field on Money | Recomputes all four withdrawal routes live and re-ranks them; the best one gets the green container and a "cheapest" label, the rest show a negative delta |
| Filter chips on Discover | All · Above my median · No assessment · Writing |
| FAB on Pipeline | Opens the Add-record bottom sheet |
| Offline strip | Tappable → Sync screen |

**Motion.** Restrained: this is a tool used several times a day, not a consumer finance app. No
celebration animations, no confetti, no count-up on figures. The one moment worth animating is a
record **crossing the phase boundary** (Approved → Payout issued): cross-fade the rail and chip from
violet to green over ~200 ms, standard M3 easing. Everything else is the default Compose
`animateContentSize` / ripple.

**Reachability.** Primary actions sit within one-handed thumb reach: FAB bottom-right above the nav
bar; the calculator input low on the Money screen; sheet actions pinned to the bottom of the sheet.
Minimum touch target 48 dp everywhere.

---

## State management

Recommended: a single-Activity Compose app with per-screen ViewModels exposing immutable UI state,
backed by a Room database and a `WorkManager` sync queue.

| State | Owner | Notes |
| --- | --- | --- |
| Records + their stage events | Room, exposed as `Flow` | Current stage is **derived** from the event log, never stored (see `DATA_MODEL.md`) |
| Platform statistics | Derived query / materialised view | Effective rate, approval rate, p50/p90 settle time, per-stage dwell time |
| Wallet balances | Room, refreshed on sync | Each with an `observedAt`; show staleness |
| FX rates | Room `ConversionSnapshot` + a "current mid" row | Landed records keep the rate they actually converted at; only unlanded estimates move on refresh |
| Sync queue | Room `SyncOp` table | Append-only, replayed in `createdAt` order, exponential backoff, max 5 attempts |
| Connectivity | `ConnectivityManager` callback | Drives the offline strip; the app is fully usable offline |
| Selection mode | ViewModel, transient | Set of selected record ids |
| Calculator inputs | ViewModel, transient | Amount + currency |

**Data fetching.** Only two things need the network: FX rates and Discovery listings. Everything else
is local. Discovery reads public platform boards and community feeds — it **never** signs in as the
user and never applies on his behalf.

---

## Design tokens

Full table with hex values in `DESIGN_TOKENS.md`. Headlines:

**Typeface.** IBM Plex Sans for UI, **IBM Plex Mono for every figure** — money, dates, durations,
percentages, ids. This is a deliberate departure from stock Roboto: the brief requires tabular
figures so columns align, and Plex Mono gives that without feature-flag gymnastics. Bundle both from
Google Fonts. If you must use Roboto for the UI face, keep Plex Mono for figures and enable `tnum`.

**Currency.** Always a three-letter code, never a symbol — `KES 247,119`, `USD 184.00`, `EUR 640.00`.
Three currencies are in play and `$` is ambiguous. Thousands separated with commas; KES shown to
zero decimals, USD and EUR to two.

**Surfaces.** Flat. **No gradients, no drop shadows, no elevation overlays.** Separation comes from
1 dp hairline dividers and 8 dp spacer bands in the surface-container tone. Cards get a 1 dp outline,
not a shadow.

**Contrast.** Body text and figures meet WCAG AA against their surface in both themes; the app is
used outdoors in daylight. Do not lighten the secondary text tones.

---

## Assets

**None.** No images, no logos, no brand marks — platforms are referenced by plain text name only, and
that is a product requirement, not an oversight. All iconography in the prototype is drawn from
primitives (rectangles, circles, rules) and should be replaced with **Material Symbols** in the app:

| Prototype mark | Material Symbol |
| --- | --- |
| Three stacked bars (Pipeline) | `list_alt` |
| 2×2 grid (Platforms) | `grid_view` |
| Concentric circles (Discover) | `explore` or `radar` |
| Circle outline (Money) | `account_balance_wallet` |
| Bordered rect (Tax) | `receipt_long` |
| Circle (search), three dots (overflow), chevron (back), plus (FAB), check (selection) | `search`, `more_vert`, `arrow_back`, `add`, `check` |

Charts (the settle-time histogram in `2b`, the cost curve in `2c`, the stacked cost bar in `1c`) are
plain rectangles — build them with Compose `Canvas` or `Row`s of `Box`es. No charting library needed.

---

## Suggested build order

1. Room schema + the append-only stage log + all derived queries (`DATA_MODEL.md`). Everything else
   depends on this being right.
2. Theme: `Color.kt`, `Type.kt`, `Theme.kt` from `DESIGN_TOKENS.md`. Light and dark from day one —
   do not retrofit dark.
3. The **record row** composable in all seven stage variants (`1h`). It is the atom of the app;
   Pipeline, bulk triage and the components sheet all reuse it.
4. Pipeline (`1a`) including the hero figure, phase-split bar, week grouping and subtotals.
5. Add-record sheet (`1f`), then Record detail (`1e`).
6. Platforms (`1b`) and the statistics behind it.
7. Money (`1c`) and Tax (`1d`).
8. Offline: connectivity strip, `SyncOp` queue, conflict UI (`2a`).
9. Settle-time percentiles (`2b`), bulk triage (`2d`), withdraw advisor (`2c`).
10. Discovery (`3a`, `3b`) last — it is the only feature that needs a backend.

## Acceptance checks

- Advancing a record to `LANDED` removes it from the "owed" hero figure and its week subtotal, with
  no manual refresh.
- A rejected record still appears in the platform's hour total and drags its effective rate down.
- Killing the network mid-session leaves every screen except Discover fully usable, and edits made
  offline replay in order on reconnect.
- Overdue flags fire on the platform's own p90, recomputed when a record lands — not on a global
  constant and not on a mean.
- Every figure in the app is monospaced and right-aligned in its column.
- Light and dark both pass AA for body text and figures.

## Contact points left open

- Discovery's sources are unspecified in the design (it says "6 platform boards and 2 community
  feeds"). That needs a real answer before `3a` is buildable.
- A **payout reversed / bounced** state is not yet designed — a record that reaches `PAYOUT_ISSUED`
  and then fails at the bank. Model it as a terminal money-phase stage now so the schema does not
  need migrating later.
- Partial payments (a platform paying 60% on approval, the rest later) are not yet designed. The
  append-only log supports splitting a record; the UI does not show it.
