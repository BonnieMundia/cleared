package app.cleared.data.seed

import app.cleared.data.db.ClearedDatabase
import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.FxRateEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.SettlementEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.model.BankDestination
import app.cleared.data.model.Currency
import app.cleared.data.model.EventSource
import app.cleared.data.model.Money
import app.cleared.data.model.PayoutDestination
import app.cleared.data.model.PlatformKind
import app.cleared.data.model.Stage
import app.cleared.data.model.WalletProvider
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
                ev(id, 4, Stage.RECEIVED, start.plus(Duration.ofDays(12))),
                ev(id, 5, Stage.REVERSED, Instant.now().minus(Duration.ofDays(ageDays)), "Name mismatch at the bank")
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
