package app.cleared.data.seed

import app.cleared.data.db.ClearedDatabase
import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.FeeLineEntity
import app.cleared.data.db.entity.FxRateEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.SettlementEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.db.entity.TaxSettingsEntity
import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.model.BankDestination
import app.cleared.data.model.Currency
import app.cleared.data.model.EventSource
import app.cleared.data.model.FeeKind
import app.cleared.data.model.Money
import app.cleared.data.model.PayoutDestination
import app.cleared.data.model.PlatformKind
import app.cleared.data.model.Stage
import app.cleared.data.model.SyncOpState
import app.cleared.data.model.WalletProvider
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The eight pipeline records and five platforms from design/sample_data.json, so a debug build has
 * something to show on a phone.
 *
 * Development only — [seedIfEmpty] is called from `ClearedApplication` behind `BuildConfig.DEBUG`
 * and never runs in a release build. Real records replace this the moment the Add-record sheet
 * exists in step 5.
 *
 * Unlike the test fixture this seeds only the open pipeline, not the synthetic history behind the
 * platform aggregates: the point here is to see frame `1a` move, and the Platforms screen that
 * needs the history arrives in step 6.
 */
object DevSeed {

    private val NAIROBI: ZoneId = ZoneId.of("Africa/Nairobi")

    suspend fun seedIfEmpty(db: ClearedDatabase) {
        if (db.recordDao().allDetails().isNotEmpty()) return
        seed(db)
    }

    private suspend fun seed(db: ClearedDatabase) {
        val now = Instant.now()

        db.fxRateDao().upsertAll(
            listOf(
                FxRateEntity(Currency.USD, BigDecimal("128.40"), now),
                FxRateEntity(Currency.EUR, BigDecimal("147.60"), now)
            )
        )

        db.platformDao().upsertAll(
            listOf(
                PlatformEntity(1, "Lumen Writers", PlatformKind.WRITING, Currency.EUR, 0.05, PayoutDestination.PAYONEER, false),
                PlatformEntity(2, "Kibo Studio", PlatformKind.OWN_COMPANY, Currency.USD, 0.00, PayoutDestination.PAYONEER, true),
                PlatformEntity(3, "Halo Data", PlatformKind.AI_TRAINING, Currency.USD, 0.05, PayoutDestination.PAYPAL, false),
                PlatformEntity(4, "Northline Freelance", PlatformKind.MARKETPLACE, Currency.USD, 0.10, PayoutDestination.PAYPAL, false),
                PlatformEntity(5, "Vector Annotate", PlatformKind.AI_TRAINING, Currency.USD, 0.05, PayoutDestination.PAYPAL, false)
            )
        )

        db.withdrawalRouteDao().upsertAll(
            listOf(
                WithdrawalRouteEntity(1, WalletProvider.PAYONEER, BankDestination.EQUITY_BANK, "Payoneer → Equity Bank", Money.minorOf("1.50"), Currency.USD, 0.0200, 2, "USD account · 1–2 days"),
                WithdrawalRouteEntity(2, WalletProvider.PAYONEER, BankDestination.MPESA, "Payoneer → M-Pesa", Money.minorOf("2.00"), Currency.USD, 0.0275, 1, "Same day · 150k daily cap", 150_000),
                WithdrawalRouteEntity(3, WalletProvider.PAYPAL, BankDestination.EQUITY_BANK, "PayPal → Equity Bank", Money.minorOf("4.99"), Currency.USD, 0.0350, 4, "3–5 days"),
                WithdrawalRouteEntity(4, WalletProvider.PAYPAL, BankDestination.MPESA, "PayPal → M-Pesa", Money.minorOf("3.50"), Currency.USD, 0.0400, 2, "1–2 days")
            )
        )

        // Weeks run from the Monday of the current week, so the seed lands in "this week",
        // "next week" and the week after however long it sits unused before a build is installed.
        val monday = LocalDate.now(NAIROBI).with(java.time.DayOfWeek.MONDAY)

        open(db, 1, 3, "184.00", Currency.USD, Stage.RECEIVED, 3, monday, 11.0)
        open(db, 2, 4, "275.00", Currency.USD, Stage.PAYOUT_ISSUED, 5, monday, 16.5)
        open(db, 3, 3, "42.50", Currency.USD, Stage.APPROVED, 2, monday, 1.5)
        open(db, 4, 2, "350.00", Currency.USD, Stage.APPROVED, 4, monday.plusWeeks(1), 22.0)
        open(db, 5, 1, "640.00", Currency.EUR, Stage.IN_REVIEW, 31, monday.plusWeeks(1), 18.5)
        open(db, 6, 5, "96.00", Currency.USD, Stage.SUBMITTED, 41, monday.plusWeeks(1), 12.0, 3.0)
        open(db, 7, 1, "210.00", Currency.EUR, Stage.SUBMITTED, 1, monday.plusWeeks(2), 6.0)
        open(db, 8, 3, "15.75", Currency.USD, Stage.REJECTED, 9, monday.plusWeeks(2), 1.5)

        // One reversed record so the "Needs attention" band from frame `4c` has something in it,
        // and one split record so the part-paid rail is visible.
        reversed(db, 9, 3, "200.00", Currency.USD, 17, monday)
        partPaid(db, 10, 1, monday)

        // Wallet balances for frame `1c`, and the Tax screen's settings row.
        val idleSince = Instant.now().minus(Duration.ofDays(19))
        db.walletDao().upsertAll(
            listOf(
                WalletBalanceEntity(WalletProvider.PAYPAL, Currency.USD, Money.minorOf("412.60"), now, idleSince),
                WalletBalanceEntity(WalletProvider.PAYONEER, Currency.USD, Money.minorOf("268.00"), now, idleSince),
                WalletBalanceEntity(WalletProvider.PAYONEER, Currency.EUR, Money.minorOf("340.00"), now, idleSince)
            )
        )
        db.taxSettingsDao().upsert(
            TaxSettingsEntity(
                personalRate = 0.25,
                turnoverTaxRate = 0.03,
                actualSetAsideKes = 164_000,
                setAsideLocation = "Equity savings",
                setAsideLastMoved = LocalDate.now(NAIROBI).minusDays(8)
            )
        )

        // The history behind the platform aggregates. Without it every effective rate on frame `1b`
        // is zero, because nothing has ever landed.
        //
        // The figures are chosen so that history plus the open records above reproduce
        // design/sample_data.json exactly: Lumen 2,275 KES/h, Kibo 1,476, Halo 1,166, Northline
        // 1,123 and Vector Annotate 402 — the last of those the only platform under 0.6x the median.
        history(db, LUMEN, Currency.EUR, landed = 22, rejected = 3, clearedKes = 399_408, hours = 169.5, unpaid = 9.0, p50 = 24, p90 = 41, idFrom = 100)
        history(db, KIBO, Currency.USD, landed = 8, rejected = 0, clearedKes = 274_500, hours = 164.0, unpaid = 0.0, p50 = 6, p90 = 12, idFrom = 200)
        history(db, HALO, Currency.USD, landed = 15, rejected = 0, clearedKes = 312_400, hours = 245.0, unpaid = 6.0, p50 = 11, p90 = 19, idFrom = 300)
        history(db, NORTHLINE, Currency.USD, landed = 10, rejected = 1, clearedKes = 148_200, hours = 115.5, unpaid = 4.0, p50 = 9, p90 = 16, idFrom = 400)
        history(db, VECTOR, Currency.USD, landed = 11, rejected = 7, clearedKes = 38_600, hours = 81.0, unpaid = 28.0, p50 = 38, p90 = 61, idFrom = 500)

        seedSyncQueue(db)
    }

    /**
     * The queue from frame `2a`: two writes waiting, one backing off, and one conflict.
     *
     * The conflict is the sample's — Halo Data's USD 42.50, where the local event says Approved and
     * the platform says Rejected. It is here so the resolution path can be seen; nothing in the app
     * produces a conflict on its own, because there is no backend yet to disagree with.
     */
    private suspend fun seedSyncQueue(db: ClearedDatabase) {
        val now = Instant.now()

        db.syncOpDao().insert(
            SyncOpEntity(
                entityType = "StageEvent",
                entityId = 5,
                payload = """{"recordId":5,"stage":"IN_REVIEW"}""",
                idempotencyKey = "seed:sync:1",
                createdAt = now.minus(Duration.ofMinutes(42)),
                sizeBytes = 96,
                label = "Lumen Writers → In review"
            )
        )
        db.syncOpDao().insert(
            SyncOpEntity(
                entityType = "EarningRecord",
                entityId = 1,
                payload = """{"recordId":1,"currency":"USD"}""",
                idempotencyKey = "seed:sync:2",
                createdAt = now.minus(Duration.ofMinutes(34)),
                sizeBytes = 148,
                label = "New record · Halo Data USD 184.00"
            )
        )
        db.syncOpDao().insert(
            SyncOpEntity(
                entityType = "StageEvent",
                entityId = 2,
                payload = """{"recordId":2,"stage":"PAYOUT_ISSUED"}""",
                idempotencyKey = "seed:sync:3",
                createdAt = now.minus(Duration.ofMinutes(27)),
                attempts = 2,
                nextAttemptAt = now.plus(Duration.ofMinutes(4)),
                state = SyncOpState.RETRYING,
                sizeBytes = 96,
                label = "Northline → Payout issued"
            )
        )

        val conflictId = db.syncOpDao().insert(
            SyncOpEntity(
                entityType = "StageEvent",
                entityId = 3,
                payload = """{"recordId":3,"stage":"APPROVED"}""",
                idempotencyKey = "seed:sync:4",
                createdAt = now.minus(Duration.ofMinutes(29)),
                sizeBytes = 96,
                label = "Halo Data → Approved"
            )
        )
        db.syncOpDao().byId(conflictId)?.let { op ->
            db.syncOpDao().update(
                op.copy(
                    state = SyncOpState.CONFLICT,
                    remoteStage = Stage.REJECTED,
                    remoteOccurredAt = now.minus(Duration.ofMinutes(48)),
                    remoteSource = EventSource.PLATFORM_API
                )
            )
        }
    }

    private const val LUMEN = 1L
    private const val KIBO = 2L
    private const val HALO = 3L
    private const val NORTHLINE = 4L
    private const val VECTOR = 5L

    /**
     * Landed and rejected history for one platform.
     *
     * Cleared KES divides across the landed records with the remainder on the last, and the unpaid
     * hours load onto the rejected records first — assessments that never turned into income are
     * exactly what that column is for. The flat 100.00 rate makes a record's cleared KES its gross
     * to the shilling, which keeps the seed about the aggregates rather than about FX.
     */
    private suspend fun history(
        db: ClearedDatabase,
        platformId: Long,
        currency: Currency,
        landed: Int,
        rejected: Int,
        clearedKes: Long,
        hours: Double,
        unpaid: Double,
        p50: Int,
        p90: Int,
        idFrom: Long
    ) {
        val total = landed + rejected
        val settleDays = durationsWith(landed, p50, p90)
        val hourTenths = distribute((hours * 10).roundToInt().toLong(), total)
        val unpaidTenths = distributeUnpaid((unpaid * 10).roundToInt().toLong(), hourTenths, rejected)
        val shares = distribute(clearedKes, landed)

        var id = idFrom
        for (i in 0 until landed) {
            val landedAt = Instant.now().minus(Duration.ofDays(30L + i * 9))
            val submittedAt = landedAt.minus(Duration.ofDays(settleDays[i]))
            val recordId = id++

            // Give each record the fees and the spread a real one carries, without moving what it
            // cleared. Cleared is (gross − same-currency fees) × rateApplied − KES fees, so adding
            // the commission and the bank fee back onto the gross leaves it exactly where it was:
            //
            //     (share + commission + 2.20 − commission) × 100 − 220  ==  share × 100
            //
            // The spread costs nothing at all in that identity, because cost is measured as
            // (mid − applied) × converted while cleared only ever uses the applied rate. So the mid
            // can sit 2% above without disturbing a single platform total.
            val commissionMinor = (shares[i] * 5) / 100
            val bankFeeKesMinor = 22_000L
            val grossMinor = shares[i] + commissionMinor + bankFeeKesMinor / 100

            db.recordDao().insert(
                EarningRecordEntity(
                    id = recordId,
                    platformId = platformId,
                    grossMinor = grossMinor,
                    currency = currency,
                    hoursWorked = (hourTenths[i] - unpaidTenths[i]) / 10.0,
                    hoursUnpaid = unpaidTenths[i] / 10.0,
                    expectedWeekStart = LocalDate.ofInstant(landedAt, NAIROBI),
                    createdAt = submittedAt
                )
            )
            db.feeLineDao().insertAll(
                listOf(
                    FeeLineEntity(
                        recordId = recordId,
                        kind = FeeKind.PLATFORM_COMMISSION,
                        label = "Platform commission",
                        amountMinor = commissionMinor,
                        currency = currency,
                        occurredAt = landedAt
                    ),
                    FeeLineEntity(
                        recordId = recordId,
                        kind = FeeKind.BANK_CREDIT_FEE,
                        label = "Bank credit fee",
                        amountMinor = bankFeeKesMinor,
                        currency = Currency.KES,
                        occurredAt = landedAt
                    )
                )
            )
            db.stageEventDao().insertAll(
                listOf(
                    ev(recordId, 1, Stage.SUBMITTED, submittedAt),
                    ev(recordId, 2, Stage.IN_REVIEW, submittedAt.plus(Duration.ofHours(6))),
                    ev(recordId, 3, Stage.APPROVED, landedAt.minus(Duration.ofDays(2))),
                    ev(recordId, 4, Stage.PAYOUT_ISSUED, landedAt.minus(Duration.ofDays(1))),
                    ev(recordId, 5, Stage.RECEIVED, landedAt.minus(Duration.ofHours(6))),
                    ev(recordId, 6, Stage.LANDED, landedAt)
                )
            )
            db.conversionDao().insertAll(
                listOf(
                    ConversionSnapshotEntity(
                        recordId = recordId,
                        fromCurrency = currency,
                        rateApplied = BigDecimal("100.00"),
                        // 2% above what was applied — the spread, which is a real cost even though
                        // no line item is ever issued for it, and the largest one on frame `1c`.
                        midRate = BigDecimal("102.00"),
                        appliedAt = landedAt
                    )
                )
            )
        }

        for (i in 0 until rejected) {
            val index = landed + i
            val rejectedAt = Instant.now().minus(Duration.ofDays(20L + i * 11))
            val recordId = id++
            db.recordDao().insert(
                EarningRecordEntity(
                    id = recordId,
                    platformId = platformId,
                    grossMinor = 0,
                    currency = currency,
                    hoursWorked = (hourTenths[index] - unpaidTenths[index]) / 10.0,
                    hoursUnpaid = unpaidTenths[index] / 10.0,
                    expectedWeekStart = LocalDate.ofInstant(rejectedAt, NAIROBI),
                    createdAt = rejectedAt
                )
            )
            db.stageEventDao().insertAll(
                listOf(
                    ev(recordId, 1, Stage.SUBMITTED, rejectedAt.minus(Duration.ofDays(6))),
                    ev(recordId, 2, Stage.IN_REVIEW, rejectedAt.minus(Duration.ofDays(5))),
                    ev(recordId, 3, Stage.REJECTED, rejectedAt)
                )
            )
        }
    }

    private fun distribute(total: Long, parts: Int): List<Long> {
        if (parts == 0) return emptyList()
        val base = total / parts
        return List(parts) { i -> if (i == parts - 1) total - base * (parts - 1) else base }
    }

    private fun distributeUnpaid(unpaidTenths: Long, hourTenths: List<Long>, rejected: Int): List<Long> {
        val out = MutableList(hourTenths.size) { 0L }
        var remaining = unpaidTenths
        val order = hourTenths.indices.toList().takeLast(rejected) +
            hourTenths.indices.toList().dropLast(rejected)
        for (i in order) {
            if (remaining <= 0) break
            val take = minOf(remaining, hourTenths[i])
            out[i] = take
            remaining -= take
        }
        return out
    }

    /** A settle-time sample whose nearest-rank p50 and p90 land exactly on the sample data's. */
    private fun durationsWith(n: Int, p50: Int, p90: Int): List<Long> {
        val i50 = ceil(0.5 * n).toInt() - 1
        val i90 = ceil(0.9 * n).toInt() - 1
        return List(n) { i ->
            when {
                i < i50 -> max(1, p50 - (i50 - i)).toLong()
                i == i50 -> p50.toLong()
                i <= i90 -> (p50 + ((p90 - p50).toDouble() * (i - i50) / (i90 - i50)).roundToInt()).toLong()
                else -> (p90 + (i - i90) * 3).toLong()
            }
        }
    }

    private suspend fun open(
        db: ClearedDatabase,
        id: Long,
        platformId: Long,
        gross: String,
        currency: Currency,
        stage: Stage,
        ageDays: Long,
        week: LocalDate,
        worked: Double,
        unpaid: Double = 0.0
    ) {
        val phaseStart = Instant.now().minus(Duration.ofDays(ageDays))
        db.recordDao().insert(
            EarningRecordEntity(
                id = id,
                platformId = platformId,
                grossMinor = Money.minorOf(gross),
                currency = currency,
                hoursWorked = worked,
                hoursUnpaid = unpaid,
                expectedWeekStart = week,
                createdAt = phaseStart
            )
        )

        val path = when (stage) {
            Stage.REJECTED -> listOf(Stage.SUBMITTED, Stage.IN_REVIEW, Stage.REJECTED)
            else -> Stage.entries.filter { it.order in 1..stage.order && it.phase != app.cleared.data.model.Phase.TERMINAL }
        }
        val inPhase = path.filter { it.phase == stage.phase }
        val before = path.filter { it.phase != stage.phase }

        val events = mutableListOf<StageEventEntity>()
        before.forEachIndexed { i, s ->
            events += ev(id, i, s, phaseStart.minus(Duration.ofDays((before.size - i).toLong())))
        }
        inPhase.forEachIndexed { i, s ->
            events += ev(id, before.size + i, s, phaseStart.plus(Duration.ofHours(i * 6L)))
        }
        db.stageEventDao().insertAll(events)
    }

    private suspend fun reversed(
        db: ClearedDatabase,
        id: Long,
        platformId: Long,
        gross: String,
        currency: Currency,
        ageDays: Long,
        week: LocalDate
    ) {
        val start = Instant.now().minus(Duration.ofDays(ageDays + 20))
        db.recordDao().insert(
            EarningRecordEntity(
                id = id,
                platformId = platformId,
                grossMinor = Money.minorOf(gross),
                currency = currency,
                hoursWorked = 9.0,
                hoursUnpaid = 0.0,
                expectedWeekStart = week,
                createdAt = start
            )
        )
        db.stageEventDao().insertAll(
            listOf(
                ev(id, 1, Stage.SUBMITTED, start),
                ev(id, 2, Stage.APPROVED, start.plus(Duration.ofDays(4))),
                ev(id, 3, Stage.PAYOUT_ISSUED, start.plus(Duration.ofDays(9))),
                ev(id, 4, Stage.RECEIVED, start.plus(Duration.ofDays(12)), "Payoneer"),
                ev(id, 5, Stage.REVERSED, Instant.now().minus(Duration.ofDays(ageDays)), "Name mismatch at the bank")
            )
        )
        db.conversionDao().insertAll(
            listOf(
                ConversionSnapshotEntity(
                    recordId = id,
                    fromCurrency = currency,
                    rateApplied = BigDecimal("126.85"),
                    midRate = BigDecimal("128.40"),
                    appliedAt = start.plus(Duration.ofDays(9))
                )
            )
        )
        db.feeLineDao().insertAll(
            listOf(
                app.cleared.data.db.entity.FeeLineEntity(
                    recordId = id,
                    kind = app.cleared.data.model.FeeKind.PLATFORM_COMMISSION,
                    label = "Platform commission 5%",
                    amountMinor = Money.minorOf("10.00"),
                    currency = Currency.USD,
                    occurredAt = start
                ),
                app.cleared.data.db.entity.FeeLineEntity(
                    recordId = id,
                    kind = app.cleared.data.model.FeeKind.RETURN_HANDLING_FEE,
                    label = "Return handling fee",
                    amountMinor = Money.minorOf("500"),
                    currency = Currency.KES,
                    occurredAt = start
                )
            )
        )
    }

    /** Lumen pays 60 / 40: the first settlement landed, the second has not. */
    private suspend fun partPaid(db: ClearedDatabase, id: Long, platformId: Long, week: LocalDate) {
        val start = Instant.now().minus(Duration.ofDays(30))
        db.recordDao().insert(
            EarningRecordEntity(
                id = id,
                platformId = platformId,
                grossMinor = Money.minorOf("1000.00"),
                currency = Currency.EUR,
                hoursWorked = 20.0,
                hoursUnpaid = 0.0,
                expectedWeekStart = week,
                createdAt = start
            )
        )
        val first = 900L
        val second = 901L
        db.settlementDao().insertAll(
            listOf(
                SettlementEntity(first, id, "60% on approval", 0.6, Money.minorOf("600.00"), 1, week),
                SettlementEntity(second, id, "40% on publication", 0.4, Money.minorOf("400.00"), 2, week.plusWeeks(1))
            )
        )
        db.stageEventDao().insertAll(
            listOf(
                ev(id, 1, Stage.SUBMITTED, start),
                ev(id, 2, Stage.APPROVED, start.plus(Duration.ofDays(6))),
                ev(id, 3, Stage.PAYOUT_ISSUED, start.plus(Duration.ofDays(8)), settlementId = first),
                ev(id, 4, Stage.LANDED, start.plus(Duration.ofDays(12)), settlementId = first),
                ev(id, 5, Stage.PAYOUT_ISSUED, start.plus(Duration.ofDays(26)), settlementId = second)
            )
        )
        db.conversionDao().insertAll(
            listOf(
                ConversionSnapshotEntity(
                    recordId = id,
                    settlementId = first,
                    fromCurrency = Currency.EUR,
                    rateApplied = BigDecimal("145.82"),
                    midRate = BigDecimal("147.60"),
                    appliedAt = start.plus(Duration.ofDays(12))
                )
            )
        )
    }

    private fun ev(
        recordId: Long,
        ordinal: Int,
        stage: Stage,
        at: Instant,
        note: String? = null,
        settlementId: Long? = null
    ) = StageEventEntity(
        recordId = recordId,
        settlementId = settlementId,
        stage = stage,
        occurredAt = at,
        source = EventSource.MANUAL,
        idempotencyKey = "seed:$recordId:$ordinal:${stage.name}",
        note = note
    )
}
