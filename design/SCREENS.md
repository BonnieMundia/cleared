# Screens — Cleared

Frame ids match the labels in `Cleared.dc.html`. Every screen exists in light and dark.
Colour token names refer to `DESIGN_TOKENS.md`; type roles to its section 4.

Common chrome unless stated otherwise: status bar 34 dp → top app bar → scrolling content
(20 dp horizontal gutter) → bottom navigation 64 dp → gesture area 22 dp.

---

## `1a` Pipeline — home, default destination

**Purpose.** Answer "how much is owed to me, and what is stuck" in under three seconds.

### Hero block — 20 dp gutter, 6 dp top, 20 dp bottom

1. Overline `Owed to you` — 12 sp / 500 / `onSurfaceVariant3`
2. Figure row, baseline-aligned, 8 dp gap: `KES` at 15 sp Mono 500 in `accent`, then the amount at
   **40 sp Mono 600, −0.02 em, `accent`**. This is the one accent figure on the screen.
3. Components row, 14 dp gap, Mono 12.5 sp: `USD 947.50` · `EUR 850.00`
4. **Phase split bar** — 6 dp tall, 3 dp radius, 16 dp above. Violet segment sized to
   `workKes / owedKes`, green fills the rest. Track in `divider`.
5. Legend row, 8 dp below, 11.5 sp: a 7 dp violet square + `Work KES 188,183`, and a green square +
   `Money KES 58,936`, pushed to opposite ends.
6. Caption, 12 dp below, 11.5 sp `tertiary2`: `7 open records · 2 past their usual settle time`

All five recompute live when a row advances.

### Week groups

Header: 20 dp gutter, 20 dp top / 9 dp bottom, background `surfaceContainer`.
Left — 11 sp / 600 / +0.07 em / uppercase / `label`, e.g. `THIS WEEK · 3–9 AUG`.
Right — Mono 11.5 sp 500, the week subtotal in KES, excluding landed and rejected.

Three groups in the sample: `This week · 3–9 Aug`, `Next week · 10–16 Aug`, `Week of 17 Aug`.

### Record row — the atom of the app

Height ~68 dp. 12 dp vertical padding, 20 dp horizontal, 11 dp internal gaps, 1 dp `divider` on top,
`surface` background, `surfaceContainerLow` on press.

```
│ ▌  Lumen Writers                         EUR 640.00 │
│ ▌  [In review]  [31d · 7 over]           KES 94,464 │
```

| Part | Spec |
| --- | --- |
| Phase rail | 3 × 36 dp, 2 dp radius. `work` / `money` / `reject` |
| Platform name | Sans 14.5 sp 500, `onSurface` (`tertiary2` when rejected) |
| Stage chip | 10.5 sp 600, padding 3 × 7 dp, radius 5 dp, `*Container` bg + `on*Container` text |
| Age pill | Mono 10.5 sp 500, padding 3 × 6 dp, radius 5 dp. Neutral: `surfaceContainerHigh` / `onSurfaceVariant3`. Overdue: `overdueContainer` / `overdue` |
| Gross | Mono 14.5 sp 500, right-aligned. **Struck through when rejected** |
| KES equivalent | Mono 11 sp, `tertiary2`. `KES 88,220 cleared` when landed (green), `no payout` in `onRejectContainer` when rejected |

Age pill copy: `3d` normally · `31d · 7 over` when overdue · `closed` when rejected.

**Seven variants** — Submitted, In review, Approved (violet); Payout issued, Received, Landed (green);
Rejected (red). All seven are laid out side by side in `1h`.

**Tap** advances one stage. **Long-press** enters selection mode (`2d`).

### FAB

60 × 60 dp, radius 18 dp, `accent`, white plus. 16 dp from the right edge, 104 dp from the bottom —
clear of the nav bar and inside thumb reach. Opens the Add-record sheet.

### Footer note

18 dp top padding, 11.5 sp `tertiary2`:
"Unlanded amounts are valued at today's mid rate. What actually clears will differ by the fee and
spread on the route you pick."
Offline variant: "…valued at the last rate fetched before you went offline."

### Offline strip — shown on the dark frame

Full-bleed, 32 dp, `offlineStrip` background. 7 dp `overdueDot` circle + 11.5 sp 500
`onOfflineStrip`: `Offline · last synced 09:14 · 2 changes queued`. Tappable → Sync.

---

## `1b` Platforms

**Purpose.** Make a bad platform obvious at a glance.

Sort row under the app bar: label `Sort` then four chips — **Effective rate** (default, desc) ·
Total paid · Approval · Days to land. Selected: `accentContainer` / `onAccentContainer`, no border.
Unselected: transparent with a 1 dp `outlineField` border.

### Platform card — 12 dp radius, 1 dp `outlineCard`, 14/16 dp padding, 11 dp internal gaps

1. **Header** — name at 15 sp 600; below it 11.5 sp `tertiary2`: `AI training tasks · pays in USD`.
   Rank numeral (`01`…) top-right, Mono 11 sp `ghost`.
2. **Headline** — `KES 2,275` at **Mono 29 sp 600, −0.022 em**, then `per hour` at 12 sp.
   Right-aligned on the same baseline: `+1,109 vs median`, Mono 11 sp.
   A poor platform renders the headline in `onRejectContainer`.
3. **Hours bar** — 5 dp tall, 3 dp radius. Track is `overdueBar` (the unpaid portion); the paid
   portion overlays it in `money`, or `reject` for a poor platform. Below it, 11.5 sp:
   `268 h logged` left, `6 h unpaid (2%)` right — `tertiary2` when zero, amber otherwise, `reject`
   when the platform is poor.
4. **Stats row** — 1 dp `outlineVariant` above, 11 dp padding, 11.5 sp:
   `94% approved` · `11 d to land` · total paid in Mono, right-aligned.
5. **Warning block** (poor platforms only) — `rejectContainer`, 8 dp radius, 6 dp dot +
   `Lowest rate · 32% of your hours here were never paid`.

Footer, 11.5 sp `tertiary2`: "Effective rate divides everything a platform has ever paid you by every
hour you have given it, including assessments and onboarding that were never billable."

---

## `1c` Money

Three sections separated by 22 dp.

**Sitting in wallets** — one card, rows split by 1 dp rules. Each row: provider name 14.5 sp 500 with
the original-currency balance under it in Mono 11.5 sp; KES equivalent right-aligned in Mono 14.5 sp.
Final row on `surfaceContainerLow`: `Not yet withdrawn · idle 19 days` and the total at Mono 15 sp 600.

**Cost of getting paid · 2026 to date** — figure `KES 41,380` at Mono 30 sp 600; caption
`5.4% of everything that landed this year`; a three-segment stacked bar 6 dp tall
(`money` / `moneyMid` / `moneyPale`), then a legend of three rows, each a swatch + label + Mono amount.

**Which route for this amount** — an input row (12 dp radius, 1 dp `outlineField`) containing a
USD/EUR segmented control on the left and a right-aligned Mono 22 sp 600 numeric field. Below it,
four route cards, 9 dp apart:

| Part | Spec |
| --- | --- |
| Route name | 13.5 sp 500, e.g. `Payoneer → Equity Bank` |
| Sub-line | 11 sp `tertiary`, e.g. `USD 1.50 fee · 2% spread · USD account · 1–2 days` |
| Net | Mono 15 sp 600, right-aligned |
| Delta | Mono 11 sp — `cheapest` on the winner, `−349` on the rest |
| Winner styling | `moneyContainer` background, `money` border, `onMoneyContainer` figure |

Footer: "Fees are flat, so the ranking changes with size. Under USD 60 the M-Pesa routes win; above
that the flat Payoneer fee disappears into the spread."

---

## `1d` Tax — deliberately plain

No cards, no accent except the export button. Sections separated by 8 dp `surfaceContainerHigh`
bands with 1 dp rules top and bottom. Year tabs (`2026` / `2025` / `All`) under the title with a
2 dp `onSurface` underline on the active one.

Three blocks, each: overline → figure at Mono 28 sp 600 → 12 sp context line → a 1 dp rule → a
label/value row at 12.5 sp.

- **Personal income** `KES 742,180` · `Landed from 4 platforms · 96 records` · `Set aside at 25%` `KES 185,545`
- **Company income · Kibo Studio** `KES 418,600` · `Turnover · 22 records · below the 5M threshold` · `Turnover tax at 3%` `KES 12,558`
- **Running set-aside** `KES 164,000` with `of KES 198,103` right-aligned; a 6 dp progress bar in
  `onSurfaceStrong`; `Short by` `KES 34,103` in `overdue`; then `Held in Equity savings` / `last moved 28 Jul`

Export: full-width outlined button, 48 dp, fully rounded, 1 dp `accent` border and `accent` label —
`Export CSV · 2026`. Caption below, centred: "118 records with every fee, rate and timestamp.
Personal and company rows are tagged separately."

---

## `1e` Record detail

Top app bar with back chevron, title `Record`.

**Header** — platform name 16 sp 600 beside a `Landed` chip; sub-line
`Long-form writing · 3 briefs · ref LW-2264`; the cleared figure at **Mono 34 sp 600 in
`onMoneyContainer`**; `Cleared to Equity Bank · 4 Jul 2026`; then a three-column strip:
`Gross EUR 640.00` · `Hours logged 18.5 h` · `End to end 22 d`.

**History** — a phase-labelled timeline.

- Phase header: overline (`WORK PHASE` in `onWorkContainer`, `MONEY PHASE` in `onMoneyContainer`),
  a 1 dp rule filling the width, and the phase duration in Mono 10.5 sp on the right.
- Each entry: a 10 dp rail column containing a 9 dp dot in the phase colour and a 1.5 dp connector
  in the phase outline colour; then stage name 13.5 sp 500, delta (`+7 d`) Mono 11 sp right-aligned,
  and the timestamp `12 Jun 2026 · 09:14` in Mono 11.5 sp below.
- Six entries: Submitted, In review, Approved, Payout issued, Received in Payoneer, Landed in KES.
  The last is 600 weight and has no connector.

**What happened to the money** — a table of label/value rows, 10 dp padding, 1 dp rules:

```
Gross                                    EUR 640.00
Platform commission 5%                   −EUR 32.00
Payoneer withdrawal fee                  −EUR 1.50
Converted 606.50 at                          145.82
  mid was 147.60 · 1.2% spread
Bank credit fee                            −KES 220
Cleared                                  KES 88,220   ← 14 sp 600, onMoneyContainer
```

Closing note on `surfaceContainerHigh`, 9 dp radius: "Getting this paid cost **KES 6,244** — 6.6% of
the mid-market value. You kept 93.4%."

---

## `1f` Add record — bottom sheet

Scrim over a dimmed Pipeline. Sheet corners 26 dp, 32 × 4 dp drag handle, title `Log work` with the
date right-aligned in Mono 11.5 sp. Target: complete in under ten seconds.

| Field | Control |
| --- | --- |
| Platform | Horizontally scrolling chips, 9 dp radius. Selected: `onSurface` fill, white label. |
| Amount | A 12 dp field: USD/EUR/KES segmented control on the left, right-aligned Mono 24 sp 600 value |
| Hours | 44 dp −/+ square buttons flanking a centred Mono 24 sp value; an **Unpaid assessment** toggle sits on the section header row |
| Stage | Two rows of chips. Work row (Submitted / In review / Approved) — selected is a filled `work` chip, unselected are `workOutline`-bordered with `onWorkContainer` text. Money row identical in green. **The two-row split is the point; do not merge them.** |
| Save | Full-width filled button, 52 dp, fully rounded, `accent` |

Caption: "Defaults come from the last record on this platform."

---

## `1g` Empty state — first run

Hero shows `KES 0` in `ghost` with an empty 6 dp track where the phase bar would be. Then:

- `Nothing in the pipeline yet` — 16 sp 600
- Body 13 sp / 1.55: "Cleared tracks the gap between work done and money landed — including the
  assessment and onboarding hours that never turn into income."
- **The stage model, taught once** — a 12 dp card containing a violet swatch + `WORK PHASE` overline,
  three chips; a 1 dp rule; a green swatch + `MONEY PHASE`, three chips; then 11.5 sp `tertiary`:
  "Records that get rejected leave the work phase without ever entering the money phase. Their hours
  still count."
- Pinned to the bottom: filled `Add your first record` (52 dp) and a text button
  `Import a CSV from a platform` (44 dp).

No carousel, no illustration.

---

## `1h` Components sheet

Not a phone frame — a reference panel, light and dark side by side. Contains: the record row in all
seven stage variants each with a Mono 9.5 sp caption; the seven stage chips; the three age-pill
states; the offline strip; and the figure scale (40/29/14.5/11) with an alignment demo proving
columns line up. **Add the `Prospect` chip here** — it was introduced in `3b` and is currently missing.

---

## `2a` Sync

Status header: an 8 dp `overdueDot` + `Offline since 09:14` at 15 sp 600, body copy, then a
three-column strip: `Queued 3` · `Conflicts 1` (in `onRejectContainer`) · `To send 4.1 KB`.

**Needs you** — a card in `rejectTint` with a `rejectOutline` border: title
`Halo Data · USD 42.50 disagrees`, then two side-by-side mini-cards (`You, 09:31` → Approved chip;
`Platform, 09:12` → Rejected chip), then two buttons — outlined **Keep mine**, filled **Take theirs**.
Explanatory line: "Taking theirs keeps your 1.5 logged hours. The record moves to Rejected and the
hours stay against Halo Data."

**Queued · replays in order** — numbered rows (`01`, `02`, `03` in Mono `tertiary3`), each with a
description, a Mono sub-line (`09:33 · retry 2 of 5 · next in 4 min`), and a state pill —
`waiting` neutral, `retrying` in `overdueContainer`.

**Rates in use** — USD and EUR mid rates, then: "Fetched 08:02, 1 h 40 m old. Landed records keep the
rate they actually converted at — only unlanded estimates move when this refreshes."
Filled `Retry now` button.

---

## `2b` Settle-time model

Reached from a platform card. Figure `11 d` at Mono 34 sp 600 with `median · 96 records`.

**Histogram** — 12 bars, 96 dp tall, 3 dp gaps, 2 dp top radius. Bars below p90 in violet
(`workOutline` → `workMid` → `work` by height), the long tail past p90 in amber. Axis labels
`2 d` · `11 d` · `19 d` · `30 d+`.

**Stat strip** — `p50 11 d` · `p90 · overdue at 19 d` (in `overdue`) · `Drift · 90 d +3 d`.

Note: "The mean here is 13.4 d — dragged up by four records that took over a month. Flagging on the
mean would have missed all four while nagging you about normal ones. Cleared flags at p90 and
recomputes it every time a record lands."

**Time spent in each stage** — a single 26 dp stacked bar across the six stages, work segments
violet, money segments green, with the two largest labelled inline. Note: "Nearly half the wait is
**In review** — a work-phase problem, not a payment problem."

**Chase card** — `One record here is past p90`, with outlined `Remind me in 3 d` and filled
`Draft follow-up`.

---

## `2c` Withdraw advisor

Title `PayPal · USD 412.60`. Figure `4.71%` at Mono 40 sp 600 **in `overdue`**, with `KES 2,495`
beside it. Copy: "The USD 4.99 flat fee is 1.2% at this size. Waiting for the USD 184.00 landing
Thursday takes the whole withdrawal to 4.42% — worth **KES 214**."

**Cost by withdrawal size** — six bars with the percentage above each; the bar at the current balance
is `accent` and its label is bold, the rest are amber (worse) or a pale accent tint (better). Axis:
100 / 200 / 300 / **413** / 600 / 800. Note: "Two withdrawals of USD 200 cost KES 1,283 more than one
of USD 400. Splitting is never cheaper."

**Bigger saving available** — a `moneyTint` card: `Have Halo Data pay to Payoneer instead`, a
two-column comparison (`4.71%` vs `2.37%` in `onMoneyContainer`), and "At last year's Halo Data
volume that difference is **KES 12,400**."

Two settings rows: **Notify me at break-even** with a toggle, and **Currency exposure** showing
`± KES 4,127` `on a 3% move`.

---

## `2d` Bulk triage

Contextual app bar replaces the normal one: `accentContainer` background extending behind the status
bar, a close X on the left, `2 records selected` as the title, and the selected KES total
right-aligned in Mono 13 sp 600.

Rows gain a 20 dp checkbox (4 dp radius) ahead of the phase rail; selected rows take the
`selectedRow` background and an `accent`-filled box with a white check.

**Bottom action bar** — a normal `flex`/`Column` sibling of the list, not an overlay: filled
`Advance stage` button plus a 48 dp overflow button, then a centred caption "Work-phase records go to
their next stage; money-phase records to theirs."

Below the list, a swipe specimen: a green `Advance` panel 96 dp wide revealed to the right of a row.
Caption: "Swipe right advances a stage, left opens the record. Both are undoable from the snackbar
for 8 seconds — no confirmation dialog."

---

## `3a` Discover

**Header** — `Best available right now`, then `KES 3,668` at Mono 34 sp 600 in `accent` +
`per hour`, then: "3.1× your median of KES 1,166/h. Scanned 6 platform boards and 2 community feeds
at 09:14 · 5 of 5 shown".

**Filter chips** — All · Above my median · No assessment · Writing. Same styling as the Platforms sort chips.

**Listing card** — 12 dp radius, 1 dp `outlineCard`:

1. Title 14.5 sp 600; sub-line `Halo Data · AI training · platform board · 3 h ago`
2. `KES 1,651` at Mono 27 sp 600 (in `onRejectContainer` when below median) + `per hour`, with
   `+42% vs your median` right-aligned in `onMoneyContainer`, or the negative in `onRejectContainer`
3. A four-row table above a 1 dp rule: **Pays** / **Hours** / **Then** / **Adjusted**
   - Hours reads `26 h est + 2 h unpaid assessment` in amber when assessment hours exist
   - Adjusted reads `KES 1,552 risk-adjusted at 94%`, or `no approval history`
4. Warning block in `rejectContainer` for below-median listings, with the platform-specific reason

Footer: "Cleared reads public boards and community feeds. It never signs in as you and never applies
on your behalf — tapping through opens the platform."

---

## `3b` Listing detail

Title, sub-line, projected rate at Mono 34 sp 600 in `accent`, and a green comparison line.

**How that number is built** — the projection as arithmetic, same table treatment as `1e`:

```
Stated pay · 40 tasks at USD 9.00        USD 360.00
Platform commission 5%                   −USD 18.00
Withdrawal and FX, your usual route       −USD 8.11
  Payoneer → Equity, 2.37%
Lands as                                 KES 42,554
Divided by hours                               28 h
  26 h of work + 2 h unpaid calibration
Projected effective                       KES 1,520
```

Then: "At Halo Data's 94% approval rate the risk-adjusted figure is **KES 1,429/h** — still your
second-best option this week."

**What you know about this platform** — `Approval 94%` · `Median to land 11 d` · `p90 19 d`, with
"From your own 96 records, not from the listing." and a link through to `2b`.

**Prospect card** — 1 dp **dashed** `outlineDashed` border: a dashed-outline `Prospect` chip beside
`a stage before Submitted`, then "Tracking starts the clock on unpaid hours. If the calibration set
takes 4 h instead of 2, you will see it in Halo Data's effective rate whether or not this ever
becomes a submission."

Filled `Track as prospect` (52 dp) and a text button `Open on Halo Data`.
