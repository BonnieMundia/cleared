package app.cleared.fixture

import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.derive.RecordState
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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * design/sample_data.json, rebuilt as entities.
 *
 * The eight pipeline records and the five platforms' headline statistics are transcribed from that
 * file. What the JSON does not contain is the *history* behind those statistics — it gives
 * `totalPaidKes: 486900` and `hoursTotal: 214` for Lumen Writers without the 25 records that add up
 * to them. [history] synthesises exactly those records: enough landed and rejected rows, carrying
 * exactly the right hours and clearing exactly the right KES, that every aggregate in the JSON
 * falls out of the derivations rather than being asserted into place.
 *
 * Synthesised landed history converts at a flat rate of 100.00 with no fees, so its cleared KES is
 * its gross to the shilling. That keeps this fixture about the aggregation arithmetic; the FX and
 * fee path is exercised separately by [landedRecordDetailExample], which uses the real numbers from
 * the `recordDetailExample` block of the JSON.
 */
object SampleData {

    private val NAIROBI: ZoneId = ZoneId.of("Africa/Nairobi")

    /** "today" in the prototype is 2026-08-02. */
    val NOW: Instant = LocalDate.of(2026, 8, 2).atTime(9, 0).atZone(NAIROBI).toInstant()

    val USD_MID: BigDecimal = BigDecimal("128.40")
    val EUR_MID: BigDecimal = BigDecimal("147.60")
    val RATES: Map<Currency, BigDecimal> = mapOf(Currency.USD to USD_MID, Currency.EUR to EUR_MID)

    const val LUMEN = 1L
    const val KIBO = 2L
    const val HALO = 3L
    const val NORTHLINE = 4L
    const val VECTOR = 5L

    val platforms: List<PlatformEntity> = listOf(
        PlatformEntity(LUMEN, "Lumen Writers", PlatformKind.WRITING, Currency.EUR, 0.05, PayoutDestination.PAYONEER, false),
        PlatformEntity(KIBO, "Kibo Studio", PlatformKind.OWN_COMPANY, Currency.USD, 0.00, PayoutDestination.PAYONEER, true),
        PlatformEntity(HALO, "Halo Data", PlatformKind.AI_TRAINING, Currency.USD, 0.05, PayoutDestination.PAYPAL, false),
        PlatformEntity(NORTHLINE, "Northline Freelance", PlatformKind.MARKETPLACE, Currency.USD, 0.10, PayoutDestination.PAYPAL, false),
        PlatformEntity(VECTOR, "Vector Annotate", PlatformKind.AI_TRAINING, Currency.USD, 0.05, PayoutDestination.PAYPAL, false)
    )

    val routes: List<WithdrawalRouteEntity> = listOf(
        WithdrawalRouteEntity(1, WalletProvider.PAYONEER, BankDestination.EQUITY_BANK, "Payoneer → Equity Bank", Money.minorOf("1.50"), Currency.USD, 0.0200, 2, "USD account · 1–2 days"),
        WithdrawalRouteEntity(2, WalletProvider.PAYONEER, BankDestination.MPESA, "Payoneer → M-Pesa", Money.minorOf("2.00"), Currency.USD, 0.0275, 1, "Same day · 150k daily cap", dailyCapKes = 150_000),
        WithdrawalRouteEntity(3, WalletProvider.PAYPAL, BankDestination.EQUITY_BANK, "PayPal → Equity Bank", Money.minorOf("4.99"), Currency.USD, 0.0350, 4, "3–5 days"),
        WithdrawalRouteEntity(4, WalletProvider.PAYPAL, BankDestination.MPESA, "PayPal → M-Pesa", Money.minorOf("3.50"), Currency.USD, 0.0400, 2, "1–2 days")
    )

    val WEEK_1: LocalDate = LocalDate.of(2026, 8, 3)
    val WEEK_2: LocalDate = LocalDate.of(2026, 8, 10)
    val WEEK_3: LocalDate = LocalDate.of(2026, 8, 17)

    /**
     * Halo Data's settle-time sample, chosen so the nearest-rank median is 11 d, p90 is 19 d and the
     * mean is 13.4 d — the three figures DATA_MODEL.md cites when it explains why overdue fires on
     * p90 rather than on a mean.
     */
    private val HALO_SETTLE_DAYS = listOf(5L, 6, 7, 8, 9, 10, 10, 11, 12, 13, 14, 15, 17, 19, 45)

    private var eventSeq = 10_000L

    // ── The eight pipeline records, `pipelineRecords` in the JSON ────────────────────────────────

    val pipeline: List<RecordDetail> by lazy {
        listOf(
        open(1, HALO, "184.00", Currency.USD, Stage.RECEIVED, ageDays = 3, week = WEEK_1, worked = 11.0),
        open(2, NORTHLINE, "275.00", Currency.USD, Stage.PAYOUT_ISSUED, ageDays = 5, week = WEEK_1, worked = 16.5),
        open(3, HALO, "42.50", Currency.USD, Stage.APPROVED, ageDays = 2, week = WEEK_1, worked = 1.5),
        open(4, KIBO, "350.00", Currency.USD, Stage.APPROVED, ageDays = 4, week = WEEK_2, worked = 22.0),
        open(5, LUMEN, "640.00", Currency.EUR, Stage.IN_REVIEW, ageDays = 31, week = WEEK_2, worked = 18.5),
        open(6, VECTOR, "96.00", Currency.USD, Stage.SUBMITTED, ageDays = 41, week = WEEK_2, worked = 12.0, unpaid = 3.0),
        open(7, LUMEN, "210.00", Currency.EUR, Stage.SUBMITTED, ageDays = 1, week = WEEK_3, worked = 6.0),
            open(8, HALO, "15.75", Currency.USD, Stage.REJECTED, ageDays = 9, week = WEEK_3, worked = 1.5)
        )
    }

    val pipelineById: Map<Long, RecordDetail> by lazy { pipeline.associateBy { it.record.id } }

    // ── Synthesised history, so the platform aggregates in the JSON are derived, not asserted ────

    /**
     * Per platform: the landed and rejected counts that produce the JSON's approval percentage,
     * the KES those landed records cleared, and the hours left over once the pipeline records above
     * have taken their share of `hoursTotal`.
     */
    val history: List<RecordDetail> by lazy {
        buildList {
        addAll(historyFor(LUMEN, Currency.EUR, landed = 22, rejected = 3, clearedKes = 486_900, hours = 189.5, unpaid = 9.0, p50 = 24, p90 = 41, idFrom = 100))
        addAll(historyFor(KIBO, Currency.USD, landed = 8, rejected = 0, clearedKes = 274_500, hours = 164.0, unpaid = 0.0, p50 = 6, p90 = 12, idFrom = 200))
        addAll(historyFor(HALO, Currency.USD, landed = 15, rejected = 0, clearedKes = 312_400, hours = 254.0, unpaid = 6.0, p50 = 11, p90 = 19, idFrom = 300, durations = HALO_SETTLE_DAYS))
        addAll(historyFor(NORTHLINE, Currency.USD, landed = 10, rejected = 1, clearedKes = 148_200, hours = 115.5, unpaid = 4.0, p50 = 9, p90 = 16, idFrom = 400))
        addAll(historyFor(VECTOR, Currency.USD, landed = 11, rejected = 7, clearedKes = 38_600, hours = 81.0, unpaid = 28.0, p50 = 38, p90 = 61, idFrom = 500))
        }
    }

    val all: List<RecordDetail> by lazy { pipeline + history }

    val states: List<RecordState> get() = all.map(RecordState::of)
    val pipelineStates: List<RecordState> get() = pipeline.map(RecordState::of)

    fun stateOf(id: Long): RecordState = RecordState.of(all.first { it.record.id == id })

    // ── Frame `1e`: one landed record, end to end, with the real fee and FX path ─────────────────

    /**
     * `recordDetailExample` from the JSON. EUR 640.00 less EUR 32.00 commission and EUR 1.50
     * withdrawal fee is EUR 606.50, converted at 145.82 against a mid of 147.60, less a KES 220
     * bank credit fee — KES 88,220 cleared.
     */
    val landedRecordDetailExample: RecordDetail by lazy {
        val id = 900L
        val submitted = LocalDate.of(2026, 6, 12).atTime(9, 14).atZone(NAIROBI).toInstant()
        RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = LUMEN,
                grossMinor = Money.minorOf("640.00"),
                currency = Currency.EUR,
                hoursWorked = 18.5,
                hoursUnpaid = 0.0,
                externalRef = "LW-2264",
                expectedWeekStart = LocalDate.of(2026, 6, 29),
                createdAt = submitted,
                description = "Long-form writing · 3 briefs"
            ),
            events = listOf(
                event(id, 1, Stage.SUBMITTED, submitted, EventSource.MANUAL),
                event(id, 2, Stage.IN_REVIEW, LocalDate.of(2026, 6, 12).atTime(18, 2).atZone(NAIROBI).toInstant(), EventSource.EMAIL_PARSE),
                event(id, 3, Stage.APPROVED, LocalDate.of(2026, 6, 19).atTime(11, 40).atZone(NAIROBI).toInstant(), EventSource.EMAIL_PARSE),
                event(id, 4, Stage.PAYOUT_ISSUED, LocalDate.of(2026, 6, 30).atTime(8, 5).atZone(NAIROBI).toInstant(), EventSource.EMAIL_PARSE),
                event(id, 5, Stage.RECEIVED, LocalDate.of(2026, 7, 2).atTime(14, 22).atZone(NAIROBI).toInstant(), EventSource.MANUAL, "Payoneer"),
                event(id, 6, Stage.LANDED, LocalDate.of(2026, 7, 4).atTime(10, 7).atZone(NAIROBI).toInstant(), EventSource.MANUAL, "Equity Bank")
            ),
            fees = listOf(
                fee(id, 1, app.cleared.data.model.FeeKind.PLATFORM_COMMISSION, "Platform commission 5%", "32.00", Currency.EUR, submitted),
                fee(id, 2, app.cleared.data.model.FeeKind.WITHDRAWAL_FEE, "Payoneer withdrawal fee", "1.50", Currency.EUR, submitted),
                fee(id, 3, app.cleared.data.model.FeeKind.BANK_CREDIT_FEE, "Bank credit fee", "220", Currency.KES, submitted)
            ),
            conversions = listOf(
                ConversionSnapshotEntity(
                    id = id,
                    recordId = id,
                    fromCurrency = Currency.EUR,
                    rateApplied = BigDecimal("145.82"),
                    midRate = BigDecimal("147.60"),
                    appliedAt = submitted
                )
            )
        )
    }

    // ── Builders ────────────────────────────────────────────────────────────────────────────────

    private fun event(
        recordId: Long,
        ordinal: Int,
        stage: Stage,
        at: Instant,
        source: EventSource = EventSource.MANUAL,
        note: String? = null,
        settlementId: Long? = null
    ) = StageEventEntity(
        id = eventSeq++,
        recordId = recordId,
        settlementId = settlementId,
        stage = stage,
        occurredAt = at,
        source = source,
        idempotencyKey = "fixture:$recordId:$ordinal:${stage.name}",
        note = note
    )

    /**
     * An open pipeline record. [ageDays] is days in the *current phase*, matching the age pill: the
     * first event of the current phase is placed exactly that far back, and anything from an earlier
     * phase sits before it.
     */
    private fun open(
        id: Long,
        platformId: Long,
        gross: String,
        currency: Currency,
        stage: Stage,
        ageDays: Long,
        week: LocalDate,
        worked: Double,
        unpaid: Double = 0.0
    ): RecordDetail {
        val phaseStart = NOW.minus(Duration.ofDays(ageDays))
        val path = pathTo(stage)
        val inPhase = path.filter { it.phase == stage.phase }
        val before = path.filter { it.phase != stage.phase }

        val events = mutableListOf<StageEventEntity>()
        before.forEachIndexed { i, s ->
            val offset = Duration.ofDays((before.size - i).toLong()).plusHours(2)
            events += event(id, i, s, phaseStart.minus(offset))
        }
        inPhase.forEachIndexed { i, s ->
            events += event(id, before.size + i, s, phaseStart.plus(Duration.ofHours(i * 6L)))
        }

        return RecordDetail(
            record = EarningRecordEntity(
                id = id,
                platformId = platformId,
                grossMinor = Money.minorOf(gross),
                currency = currency,
                hoursWorked = worked,
                hoursUnpaid = unpaid,
                expectedWeekStart = week,
                createdAt = phaseStart
            ),
            events = events
        )
    }

    /** Every stage a record passes through on its way to [stage], in order. */
    private fun pathTo(stage: Stage): List<Stage> = when (stage) {
        Stage.PROSPECT -> listOf(Stage.PROSPECT)
        Stage.REJECTED -> listOf(Stage.SUBMITTED, Stage.IN_REVIEW, Stage.REJECTED)
        Stage.REVERSED -> listOf(Stage.SUBMITTED, Stage.APPROVED, Stage.PAYOUT_ISSUED, Stage.RECEIVED, Stage.REVERSED)
        else -> Stage.entries.filter { it.phase != app.cleared.data.model.Phase.TERMINAL && it.order in 1..stage.order }
    }

    private fun fee(
        recordId: Long,
        ordinal: Long,
        kind: app.cleared.data.model.FeeKind,
        label: String,
        amount: String,
        currency: Currency,
        at: Instant
    ) = app.cleared.data.db.entity.FeeLineEntity(
        id = recordId * 10 + ordinal,
        recordId = recordId,
        kind = kind,
        label = label,
        amountMinor = Money.minorOf(amount),
        currency = currency,
        occurredAt = at
    )

    /**
     * Landed and rejected history for one platform.
     *
     * Cleared KES is divided across the landed records with the remainder on the last, so the sum
     * is exactly [clearedKes]. Hours are divided in tenths the same way, and the unpaid hours are
     * loaded onto the rejected records first — assessments that never turned into income are
     * exactly what the unpaid-hours column is for.
     */
    private fun historyFor(
        platformId: Long,
        currency: Currency,
        landed: Int,
        rejected: Int,
        clearedKes: Long,
        hours: Double,
        unpaid: Double,
        p50: Int,
        p90: Int,
        idFrom: Long,
        durations: List<Long>? = null
    ): List<RecordDetail> {
        val total = landed + rejected
        require(total > 0)

        val settleDays = durations ?: durationsWith(landed, p50, p90)
        require(settleDays.size == landed) { "need $landed settle times, got ${settleDays.size}" }

        val hourTenths = distribute((hours * 10).roundToInt().toLong(), total)
        val unpaidTenths = distributeUnpaid((unpaid * 10).roundToInt().toLong(), hourTenths, rejected)
        val clearedShares = distribute(clearedKes, landed)

        val out = mutableListOf<RecordDetail>()
        var id = idFrom

        for (i in 0 until landed) {
            // Landed a while ago, spread back through the year so the year-to-date figures have
            // something to sum. The exact date does not affect any asserted figure.
            val landedAt = NOW.minus(Duration.ofDays(30L + i * 9))
            val submittedAt = landedAt.minus(Duration.ofDays(settleDays[i]))
            val recordId = id++
            val grossMinor = clearedShares[i] // × 100.00 converts a cent to a whole shilling
            out += RecordDetail(
                record = EarningRecordEntity(
                    id = recordId,
                    platformId = platformId,
                    grossMinor = grossMinor,
                    currency = currency,
                    hoursWorked = (hourTenths[i] - unpaidTenths[i]) / 10.0,
                    hoursUnpaid = unpaidTenths[i] / 10.0,
                    expectedWeekStart = LocalDate.ofInstant(landedAt, NAIROBI),
                    createdAt = submittedAt
                ),
                events = landedChain(recordId, submittedAt, settleDays[i]),
                conversions = listOf(
                    ConversionSnapshotEntity(
                        id = recordId,
                        recordId = recordId,
                        fromCurrency = currency,
                        rateApplied = BigDecimal("100.00"),
                        midRate = BigDecimal("100.00"),
                        appliedAt = landedAt
                    )
                )
            )
        }

        for (i in 0 until rejected) {
            val index = landed + i
            val rejectedAt = NOW.minus(Duration.ofDays(20L + i * 11))
            val recordId = id++
            out += RecordDetail(
                record = EarningRecordEntity(
                    id = recordId,
                    platformId = platformId,
                    grossMinor = 0,
                    currency = currency,
                    hoursWorked = (hourTenths[index] - unpaidTenths[index]) / 10.0,
                    hoursUnpaid = unpaidTenths[index] / 10.0,
                    expectedWeekStart = LocalDate.ofInstant(rejectedAt, NAIROBI),
                    createdAt = rejectedAt
                ),
                events = listOf(
                    event(recordId, 1, Stage.SUBMITTED, rejectedAt.minus(Duration.ofDays(6))),
                    event(recordId, 2, Stage.IN_REVIEW, rejectedAt.minus(Duration.ofDays(5))),
                    event(recordId, 3, Stage.REJECTED, rejectedAt)
                )
            )
        }

        return out
    }

    private fun landedChain(recordId: Long, submittedAt: Instant, days: Long): List<StageEventEntity> {
        val step = { fraction: Double -> submittedAt.plus(Duration.ofMinutes((days * 24 * 60 * fraction).toLong())) }
        return listOf(
            event(recordId, 1, Stage.SUBMITTED, submittedAt),
            event(recordId, 2, Stage.IN_REVIEW, step(0.05)),
            event(recordId, 3, Stage.APPROVED, step(0.55)),
            event(recordId, 4, Stage.PAYOUT_ISSUED, step(0.60)),
            event(recordId, 5, Stage.RECEIVED, step(0.85)),
            event(recordId, 6, Stage.LANDED, submittedAt.plus(Duration.ofDays(days)))
        )
    }

    /** Splits [total] into [parts], remainder onto the last part, so the sum is exact. */
    private fun distribute(total: Long, parts: Int): List<Long> {
        if (parts == 0) return emptyList()
        val base = total / parts
        return List(parts) { i -> if (i == parts - 1) total - base * (parts - 1) else base }
    }

    /**
     * Unpaid hours go onto the rejected records first, then spill onto the landed ones, and never
     * exceed a record's own hours.
     */
    private fun distributeUnpaid(unpaidTenths: Long, hourTenths: List<Long>, rejected: Int): List<Long> {
        val out = MutableList(hourTenths.size) { 0L }
        var remaining = unpaidTenths
        val order = (hourTenths.indices.toList().takeLast(rejected) + hourTenths.indices.toList().dropLast(rejected))
        for (i in order) {
            if (remaining <= 0) break
            val take = minOf(remaining, hourTenths[i])
            out[i] = take
            remaining -= take
        }
        require(remaining == 0L) { "could not place all unpaid hours" }
        return out
    }

    /**
     * A settle-time sample whose nearest-rank p50 and p90 are exactly [p50] and [p90] — the same
     * percentile definition [app.cleared.data.derive.SettleTime] uses.
     */
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
}
