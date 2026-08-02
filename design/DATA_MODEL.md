# Data model — Cleared

The single most important decision in this app: **a record's current stage is derived from an
append-only event log, never stored as a column.**

That one choice is what makes offline edits replayable, conflicts detectable, settle-time percentiles
free to compute, and the record-detail history a read of the log rather than a parallel table you
have to keep in sync. Get this right before building any UI.

---

## 1. Entities

### `Platform`

| Field | Type | Notes |
| --- | --- | --- |
| `id` | Long | |
| `name` | String | Plain text. No logos, no brand marks — product requirement. |
| `kind` | Enum | `AI_TRAINING`, `WRITING`, `MARKETPLACE`, `OWN_COMPANY` |
| `payCurrency` | Enum | `USD`, `EUR`, `KES` |
| `commissionPct` | Double | Platform's cut, e.g. `0.05` |
| `payoutDestination` | Enum | `PAYPAL`, `PAYONEER` |
| `isCompany` | Boolean | True for the user's own business. Splits personal vs company on the Tax screen. |

### `EarningRecord`

| Field | Type | Notes |
| --- | --- | --- |
| `id` | Long | |
| `platformId` | Long | |
| `grossAmount` | BigDecimal | In `currency` |
| `currency` | Enum | `USD`, `EUR`, `KES` |
| `hoursWorked` | Double | |
| `hoursUnpaid` | Double | Assessments, calibration, onboarding. **Counted even when the record is rejected.** |
| `externalRef` | String? | e.g. `LW-2264` |
| `expectedWeekStart` | LocalDate | Drives the week grouping on Pipeline |
| `createdAt` | Instant | |

No `stage` column. No `currentStage` column. Resist the temptation.

### `StageEvent` — append-only

| Field | Type | Notes |
| --- | --- | --- |
| `id` | Long | |
| `recordId` | Long | |
| `stage` | Enum | See below |
| `occurredAt` | Instant | **When it happened**, not when it was written |
| `source` | Enum | `MANUAL`, `EMAIL_PARSE`, `PLATFORM_API`, `SMS_PARSE` |
| `idempotencyKey` | String | Unique. Makes replay safe. |
| `note` | String? | |

**Never update or delete a row in this table.** A correction is a new event. A conflict resolution is
a new event whose `source` records who won.

```kotlin
enum class Stage(val phase: Phase, val order: Int) {
    PROSPECT      (Phase.PRE,      0),
    SUBMITTED     (Phase.WORK,     1),
    IN_REVIEW     (Phase.WORK,     2),
    APPROVED      (Phase.WORK,     3),
    PAYOUT_ISSUED (Phase.MONEY,    4),
    RECEIVED      (Phase.MONEY,    5),
    LANDED        (Phase.MONEY,    6),
    REJECTED      (Phase.TERMINAL, 7)
}

enum class Phase { PRE, WORK, MONEY, TERMINAL }
```

`RECEIVED` means the money is sitting in PayPal or Payoneer. `LANDED` means it is in KES at Equity
Bank or M-Pesa. The distinction matters — money in a wallet is not money you can spend, and the
Money screen exists to make that gap visible.

### `FeeLine`

| Field | Type | Notes |
| --- | --- | --- |
| `id` | Long | |
| `recordId` | Long | |
| `kind` | Enum | `PLATFORM_COMMISSION`, `WITHDRAWAL_FEE`, `BANK_CREDIT_FEE`, `FX_SPREAD` |
| `amount` | BigDecimal | Always positive; render with a leading minus |
| `currency` | Enum | |
| `occurredAt` | Instant | |

### `ConversionSnapshot`

| Field | Type | Notes |
| --- | --- | --- |
| `recordId` | Long | |
| `fromCurrency` | Enum | |
| `rateApplied` | BigDecimal | What the provider actually gave, e.g. `145.82` |
| `midRate` | BigDecimal | Mid-market at that moment, e.g. `147.60` |
| `appliedAt` | Instant | |

**Landed records keep the rate they converted at, forever.** Only unlanded estimates move when the
current mid rate refreshes. Nothing is ever silently revalued.

### `FxRate` — the current mid, one row per currency

`currency`, `midToKes`, `fetchedAt`. Show `fetchedAt` staleness on the Sync screen; treat anything
older than ~2 h as stale and label unlanded figures as estimates.

### `WalletBalance`

`provider` (`PAYPAL` | `PAYONEER`), `currency`, `amount`, `observedAt`, `idleSince`.

### `WithdrawalRoute`

`id`, `provider`, `destination` (`EQUITY_BANK` | `MPESA`), `flatFee`, `feeCurrency`, `spreadPct`,
`medianDays`, `dailyCapKes?`.

### `SyncOp` — the offline queue

| Field | Type | Notes |
| --- | --- | --- |
| `id` | Long | Ordering key — replay in ascending `id` |
| `entityType` / `entityId` | String / Long | |
| `payload` | String | JSON |
| `createdAt` | Instant | |
| `attempts` | Int | Max 5, exponential backoff |
| `nextAttemptAt` | Instant? | |
| `state` | Enum | `WAITING`, `RETRYING`, `CONFLICT`, `DONE`, `FAILED` |
| `sizeBytes` | Int | Surfaced on the Sync screen — the user is on metered mobile data |

### `Listing` — Discovery

`id`, `platformName`, `title`, `kind`, `statedPay`, `currency`, `estHours`, `assessmentHours`,
`sourceLabel`, `sourceUrl`, `seenAt`, `note`.

---

## 2. Derived values

None of these are stored. All are queries or pure functions.

### Current stage

```
currentStage(record) = the StageEvent with the greatest occurredAt
                       (ties broken by the greater Stage.order)
```

### Age and overdue

```
daysInCurrentPhase = today − occurredAt of the first event in the current phase
isOverdue          = phase in (WORK, MONEY)
                     && daysInCurrentPhase > platformSettleP90(platformId) + graceDays
```

`graceDays` defaults to 0 and is user-configurable.

**Overdue is p90, not a mean.** Settle times are long-tailed. On the sample data the mean for one
platform is 13.4 d while the median is 11 d and p90 is 19 d — flagging on the mean would miss every
genuinely stuck record while nagging about normal ones. Recompute p90 every time a record lands.

### Settle-time statistics, per platform

Over records that reached `LANDED`, take `landedAt − submittedAt` and compute p50, p90, and the
90-day drift (p50 now vs p50 ninety days ago). Also compute mean dwell time **per stage** — the
Settle-time screen (`2b`) uses it to show that most of the wait is in `IN_REVIEW`, which is a
work-phase problem, not a payment problem.

### Effective KES per hour — the headline number

```
effectiveRate(platform) =
      Σ finalKesCleared over ALL landed records for that platform
    ─────────────────────────────────────────────────────────────
      Σ (hoursWorked + hoursUnpaid) over ALL records for that platform,
        INCLUDING rejected ones
```

Rejected work stays in the denominator and contributes nothing to the numerator. That asymmetry is
the entire product thesis — a platform that wastes your time should look bad.

Flag a platform as poor when `effectiveRate < 0.6 × medianRateAcrossPlatforms`.

### Approval rate

`landedCount ÷ (landedCount + rejectedCount)` per platform.

### Total owed — the Pipeline hero

```
owedKes = Σ over records whose phase is WORK or MONEY of
          grossAmount × currentMidRate(currency)
```

`LANDED` and `REJECTED` are excluded. Split it for the phase bar:

```
workKes  = same sum restricted to phase == WORK
moneyKes = owedKes − workKes
```

Week subtotals use the same rule, filtered by `expectedWeekStart`.

### Withdrawal route net

```
netKes(route, amount) = (amount − route.flatFee) × midRate × (1 − route.spreadPct)
```

Rank descending. Best route gets the green container and the label "cheapest"; the rest show
`−(best − net)`. Because the fee is flat, the ranking genuinely changes with size — small amounts
favour the cheap-fee route, large amounts favour the low-spread route. Do not hard-code a winner.

### Cost of getting paid, year to date

`Σ FeeLine.amount` converted to KES, plus FX spread computed as
`Σ (midRate − rateApplied) × convertedAmount`, grouped by `FeeLine.kind`.

### Tax set-aside

```
personalIncomeYtd = Σ finalKesCleared where platform.isCompany == false
companyIncomeYtd  = Σ finalKesCleared where platform.isCompany == true
recommendedSetAside = personalIncomeYtd × personalRate   // default 0.25, configurable
                    + companyIncomeYtd  × turnoverTaxRate // default 0.03
shortfall = recommendedSetAside − amountActuallySetAside
```

Both rates must be user-editable — tax law changes and this app should not need a release to follow it.

### Discovery projection

```
netKes      = statedPay × (1 − platform.commissionPct)
              × (1 − usualRoute.spreadPct) × midRate − flatFeeInKes
projected/h = netKes ÷ (estHours + assessmentHours)
riskAdjusted = projected/h × approvalRate(platform)      // null if no history
```

Show `riskAdjusted` wherever there is history. A listing on a platform that rejects 39% of
submissions is displayed at 61% of its headline rate — that is the reason this feature lives inside
this app rather than in a browser tab.

---

## 3. Sync and conflicts

1. Every local mutation writes its rows **and** appends a `SyncOp` in one transaction.
2. `WorkManager` drains the queue in `id` order when connectivity returns.
3. Each op carries an idempotency key, so a partially-applied replay is safe.
4. If the server reports a different stage for a record whose local event is newer, mark the op
   `CONFLICT` and surface it on the Sync screen (`2a`) with both sides, both timestamps, and two
   actions: **Keep mine** / **Take theirs**.
5. Resolving a conflict appends a **new** `StageEvent` recording the decision. Nothing is rewritten.
6. Resolving in favour of the platform **does not discard logged hours** — the record moves to
   `REJECTED` and the hours stay against the platform. Say so in the UI; the design does.

---

## 4. Offline behaviour

Every screen except Discover is fully usable with no network. The offline strip is a **persistent
condition, not an error** — amber, slim, under the app bar, with a queued-writes count, tappable
through to Sync. No blocking dialogs, no spinners over content, no disabled screens.

Discover shows its last successful scan with a plain staleness line.

---

## 5. Room notes

- Index `StageEvent` on `(recordId, occurredAt DESC)` — the current-stage query is the hottest read
  in the app.
- Consider a materialised `RecordSummary` view refreshed by trigger, if profiling shows the derived
  queries hurt on a list of a few thousand records. Do not start there.
- Everything money is `BigDecimal` stored as a scaled `Long` (minor units). Never `Double`.
- Store `Instant` as epoch millis UTC; format in `Africa/Nairobi`.
- The database holds financial records on a phone: enable SQLCipher or at minimum
  `android:allowBackup="false"`, and put the app behind biometric unlock.
