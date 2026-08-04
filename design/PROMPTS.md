# Prompts — building Cleared with Claude Code

Copy these one at a time. **One prompt per step.** Wait for it to finish, check the result, commit,
then move on. Asking for several steps at once reliably produces worse code than asking for one.

Prerequisites: `BUILD_SETUP.md` done, `adb devices` shows your phone, `CLAUDE.md` at the repo root.

Run `claude` in the `cleared/` folder. Then:

---

## Step 0 — Orientation and scaffolding

> Read CLAUDE.md and everything in design/. Summarise the stage model back to me in five lines, then
> stop and wait.

Check the summary. It should mention two phases, violet/green, that current stage is derived from an
append-only log, that rejected records keep their hours, and that reversed is terminal. If any of
that is missing, it hasn't read properly — say so and have it re-read before continuing.

Then:

> Set up an empty Jetpack Compose project for an app called Cleared. Kotlin, Material 3, single
> Activity, Compose Navigation, Room with KSP, WorkManager. compileSdk 35, minSdk 26, JDK 17. Bundle
> IBM Plex Sans and IBM Plex Mono as res/font resources — not downloadable fonts.
>
> Build no screens and no data classes yet. Stop when `./gradlew assembleDebug` succeeds and show me
> the file tree.

**Commit here.** `git add . && git commit -m "Scaffold"`

---

## Step 1 — Data model

The most important step. Everything downstream is wrong if this is wrong, so be picky.

> Do step 1 of the build order in CLAUDE.md: the Room schema, the append-only StageEvent log, and
> every derived query in design/DATA_MODEL.md.
>
> Requirements I want to see honoured explicitly:
> - A record's current stage is **derived** from its event log, never stored as a column.
> - REJECTED and REVERSED are terminal. Rejected records keep their logged hours.
> - REVERSED records keep their fees, so their net contribution can be negative.
> - Partial payments: a settlement table, `supersedesRecordId`, and `carriesHours` as specified.
> - A part-paid record's owed figure is only its unlanded remainder.
>
> Write unit tests asserting against design/sample_data.json: owedKes 247119, work/money split
> 188183 / 58936, week subtotals 64393 / 151730 / 30996, Vector Annotate effective rate 402.
>
> No UI. Stop when the tests pass and show me the test output.

If those five figures don't come out exact, do not proceed. Ask:

> The figures don't match. Walk me through how you computed owedKes and where it diverges from
> design/DATA_MODEL.md.

**Commit.**

---

## Step 2 — Theme

> Do step 2: Color.kt, Type.kt, Theme.kt from design/DESIGN_TOKENS.md. Light and dark colour schemes
> both, complete, from the hex values in that file — do not generate a scheme from a seed colour.
>
> IBM Plex Sans for UI, IBM Plex Mono for every figure. Add a `figure` text style that is Plex Mono
> with tabular figures, and use it for all money, dates, durations, percentages and ids.
>
> No gradients, no drop shadows, no elevation overlays anywhere. Cards get a 1dp outline.

**Commit.**

---

## Step 3 — The record row

The atom of the app. Pipeline, bulk triage and the components sheet all reuse it. Get it right here
and the rest goes fast.

> Do step 3: the record row composable, matching frame `1h` in design/SCREENS.md.
>
> Add @Preview composables covering every variant in both light and dark: prospect (dashed outline
> chip), submitted, in review, approved, payout issued, received, landed, rejected (struck-through
> amount, red rail), reversed, part paid (split rail), and overdue (amber age pill).
>
> Stop there. Show me the previews.

Look at the previews in Android Studio side by side with `index.html`. This is the moment to correct
colour, rail weight, type sizes and alignment — fixing it later means fixing it in four places.

**Commit.**

---

## Step 4 — Pipeline

> Do step 4: the Pipeline screen, frame `1a`. Hero figure of total owed in KES with its USD and EUR
> components, the phase-split bar, records grouped by expected arrival week with subtotals, the FAB,
> and the offline strip.
>
> Include the "Needs attention" band pinned above the week groups for reversed records, per frame
> `4c`.
>
> Tapping a row advances it one stage and everything recomputes — hero, split bar, week subtotal.
> Landed records leave the owed figure. Rejected, reversed and landed rows do not advance.
> Undo snackbar for 8 seconds. No confirmation dialogs anywhere in this app.
>
> Animate only the phase crossing: Approved → Payout issued cross-fades the rail and chip from violet
> to green over 200ms. Nothing else animates.

Install it and use it on the phone for a few minutes before moving on.

```bash
./gradlew installDebug
```

**Commit.**

---

## Step 5 — Add record, record detail

> Do step 5: the Add-record bottom sheet (frame `1f`) and Record detail (frame `1e`).
>
> The sheet must be completable in under ten seconds — platform, amount, currency, hours, stage.
> Actions pinned to the bottom of the sheet within thumb reach.
>
> Record detail shows every stage transition with its timestamp, the rate applied, each fee, and
> final KES cleared. Also build the two variants: frame `4a` reversed, where the chain visibly breaks
> after Received and links to the successor record, and frame `4b` part paid, with two settlements
> under one record and a split hero bar.

**Commit.**

---

## Step 6 — Platforms

> Do step 6: the Platforms screen (frame `1b`) and the statistics behind it. One card per platform,
> effective KES/hour as the headline. Sort chips: effective rate (default, descending), total paid,
> approval, days to land.
>
> Rejected hours must be included in the denominator of the effective rate. Assessment and onboarding
> hours too. That is the entire point of the screen.

**Commit.**

---

## Step 7 — Money and Tax

> Do step 7: Money (frame `1c`) and Tax (frame `1d`).
>
> Money: idle wallet balances with staleness from `observedAt`, year-to-date cost of getting paid as
> a stacked bar, and the withdrawal-route calculator. The amount field recomputes all four routes
> live and re-ranks them; cheapest gets the green container and a "cheapest" label, the others show a
> negative delta.
>
> Tax: deliberately plain. Personal and company income kept apart, running set-aside, CSV export.
>
> Charts are Compose Canvas or Rows of Boxes. Do not add a charting library.

**Commit.**

---

## Step 8 — Offline

> Do step 8: offline behaviour. The connectivity strip driven by a ConnectivityManager callback, the
> SyncOp queue table, WorkManager replay in createdAt order with exponential backoff and max 5
> attempts, and the conflict UI from frame `2a`.
>
> Offline is a persistent condition, not an error. Every screen except Discover stays fully usable
> with no network. The strip is a slim amber band under the app bar showing queued-write count,
> tappable through to Sync.

Test it properly: turn on airplane mode, make several edits, turn it off, confirm they replay in
order.

**Commit.**

---

## Step 9 — The analysis screens

> Do step 9: settle-time percentiles (frame `2b`), bulk triage (frame `2d`), and the withdraw advisor
> (frame `2c`).
>
> Overdue fires on the platform's own p90, recomputed when a record lands — never a global constant
> and never a mean.
>
> Bulk triage: long-press enters selection mode with a contextual app bar in accentContainer,
> checkboxes on rows, action bar at the bottom. Swipe right advances a row one stage, swipe left
> opens it.

**Commit.**

---

## Step 10 — Discovery (last)

Only feature needing a backend, and its sources are still unspecified. Build the UI against fixtures:

> Do step 10: Discover (frame `3a`) and Listing detail (frame `3b`), against local fixture data — no
> network calls yet.
>
> Prospect chips are dashed outline, never filled. Hours start counting the moment a prospect is
> tracked. Listing detail shows the projected effective rate as visible arithmetic, not just a
> number. Filter chips: All, Above my median, No assessment, Writing.
>
> Discovery never signs in as the user and never applies on his behalf.

**Commit.**

---

## Then: cloud builds

Once it works, so you can install without a cable:

> Add a GitHub Actions workflow that runs `./gradlew assembleDebug` on every push to main and uploads
> the APK as a build artifact.

Every push then leaves a downloadable APK on the repo's Actions tab.

---

## Running rules

**One step per prompt.** The single biggest factor in output quality.

**Commit after every step.** When a step goes wrong you want to be one `git reset` from safety, not
three.

**Check against `index.html`, not against your memory of it.** Open the design reference next to the
emulator every time.

**When it drifts, quote the spec.** Not "the colours look off" but "design/DESIGN_TOKENS.md gives
work-phase rail as #7C5CD3; you've used the M3 default primary." Claude Code corrects precisely when
pointed precisely.

**Push before asking me to review.** I read the repo, not your screen. Useful asks:
- "Review github.com/BonnieMundia/cleared against design/SCREENS.md and tell me where it drifted."
- "The Platforms screen doesn't feel right in the build — here's a screenshot."
- "Design the Discovery sources" — the one gap still open.
