package app.cleared.data

import androidx.room.withTransaction
import app.cleared.data.db.ClearedDatabase
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.DiscoveryScanEntity
import app.cleared.data.db.entity.FxRateEntity
import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.db.entity.TaxSettingsEntity
import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.derive.Pipeline
import app.cleared.data.derive.PipelineTotals
import app.cleared.data.derive.PlatformStatistics
import app.cleared.data.derive.PlatformStats
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.SettleTime
import app.cleared.data.derive.StageResolver
import app.cleared.data.discovery.DiscoverySource
import app.cleared.data.model.Currency
import app.cleared.data.model.EventSource
import app.cleared.data.model.Money
import app.cleared.data.model.Stage
import app.cleared.data.model.SyncOpState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * The one place the derived views are assembled. Everything it exposes is computed from the tables;
 * nothing here reads a cached figure.
 */
class ClearedRepository(private val db: ClearedDatabase) {

    private val NAIROBI: ZoneId = ZoneId.of("Africa/Nairobi")

    fun observeRecordStates(): Flow<List<RecordState>> =
        db.recordDao().observeDetails().map { details -> details.map(RecordState::of) }

    fun observePlatforms(): Flow<List<PlatformEntity>> = db.platformDao().observeAll()

    fun observeRates(): Flow<Map<Currency, BigDecimal>> =
        db.fxRateDao().observeAll().map { rows -> rows.associate { it.currency to it.midToKes } }

    fun observePipeline(now: () -> Instant = Instant::now): Flow<PipelineTotals> =
        combine(observeRecordStates(), observeRates(), db.platformDao().observeAll()) { states, rates, platforms ->
            val at = now()
            Pipeline.totals(
                states = states,
                rates = rates,
                now = at,
                overdueP90Days = SettleTime.p90ByPlatform(platforms.map { it.id }, states, at),
                graceDays = platforms.associate { it.id to it.graceDays }
            )
        }

    fun observePlatformStats(): Flow<List<PlatformStats>> =
        combine(db.platformDao().observeAll(), observeRecordStates()) { platforms, states ->
            PlatformStatistics.all(platforms, states)
        }

    /**
     * Advance a record one stage.
     *
     * Writes a new [StageEventEntity] and a [SyncOpEntity] in one transaction — the invariant that
     * makes offline replay complete. Nothing is updated; the previous stage stays in the log.
     * Returns the new stage, or null when the record does not advance (landed and terminal rows).
     */
    suspend fun advance(recordId: Long, at: Instant = Instant.now(), source: EventSource = EventSource.MANUAL): Stage? =
        db.withTransaction {
            val current = db.stageEventDao().latestForRecord(recordId)?.stage ?: Stage.PROSPECT
            val next = current.next() ?: return@withTransaction null

            val key = "advance:$recordId:${next.name}:${UUID.randomUUID()}"
            db.stageEventDao().insert(
                StageEventEntity(
                    recordId = recordId,
                    stage = next,
                    occurredAt = at,
                    source = source,
                    idempotencyKey = key
                )
            )
            db.syncOpDao().insert(
                SyncOpEntity(
                    entityType = "StageEvent",
                    entityId = recordId,
                    payload = """{"recordId":$recordId,"stage":"${next.name}","occurredAt":"${at}"}""",
                    idempotencyKey = key,
                    createdAt = at,
                    sizeBytes = 96
                )
            )
            next
        }

    /**
     * Undo is the same mechanism, not a rollback: it appends the previous stage back onto the log.
     * The app never deletes an event, so the record's history shows the correction.
     */
    suspend fun revertTo(recordId: Long, stage: Stage, at: Instant = Instant.now()) {
        db.withTransaction {
            // The undo must sort after the event it undoes. Ties are broken by the greater
            // Stage.order, which would hand the tie to the advance and silently do nothing, so a
            // same-millisecond undo is nudged a tick past it rather than left to lose.
            val latest = db.stageEventDao().latestForRecord(recordId)?.occurredAt
            val occurredAt =
                if (latest != null && !at.isAfter(latest)) latest.plusMillis(1) else at

            val key = "undo:$recordId:${stage.name}:${UUID.randomUUID()}"
            db.stageEventDao().insert(
                StageEventEntity(
                    recordId = recordId,
                    stage = stage,
                    occurredAt = occurredAt,
                    source = EventSource.MANUAL,
                    idempotencyKey = key,
                    note = "Undo"
                )
            )
        }
    }

    suspend fun platforms(): List<PlatformEntity> = db.platformDao().all()

    fun observeWallets(): Flow<List<WalletBalanceEntity>> = db.walletDao().observeAll()

    fun observeRoutes(): Flow<List<WithdrawalRouteEntity>> = db.withdrawalRouteDao().observeAll()

    fun observeTaxSettings(): Flow<TaxSettingsEntity?> = db.taxSettingsDao().observe()

    suspend fun saveTaxSettings(settings: TaxSettingsEntity) = db.taxSettingsDao().upsert(settings)

    fun observeRecordDetail(recordId: Long): Flow<RecordDetail?> = db.recordDao().observeDetail(recordId)

    suspend fun recordDetail(recordId: Long): RecordDetail? =
        db.recordDao().allDetails().firstOrNull { it.record.id == recordId }

    /** The successor of a reversed record, if one has been logged yet. */
    suspend fun successorOf(recordId: Long): RecordDetail? =
        db.recordDao().allDetails().firstOrNull { it.record.supersedesRecordId == recordId }

    /** Defaults for the Add-record sheet: the most recent record on this platform. */
    suspend fun lastRecordFor(platformId: Long): EarningRecordEntity? =
        db.recordDao().detailsForPlatform(platformId)
            .maxByOrNull { it.record.createdAt }
            ?.record

    /**
     * Creates a record and its opening stage event in one transaction, plus the `SyncOp` that will
     * replay it. A record can never exist without an event, because its stage is derived from them.
     */
    suspend fun createRecord(
        platformId: Long,
        amount: BigDecimal,
        currency: Currency,
        hoursWorked: Double,
        hoursUnpaid: Double,
        stage: Stage,
        at: Instant = Instant.now(),
        expectedWeekStart: LocalDate = LocalDate.ofInstant(at, NAIROBI)
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
        externalRef: String? = null,
        description: String? = null
    ): Long = db.withTransaction {
        val recordId = db.recordDao().insert(
            EarningRecordEntity(
                platformId = platformId,
                grossMinor = Money.toMinor(amount),
                currency = currency,
                hoursWorked = hoursWorked,
                hoursUnpaid = hoursUnpaid,
                externalRef = externalRef,
                expectedWeekStart = expectedWeekStart,
                createdAt = at,
                description = description
            )
        )

        val key = "create:$recordId:${UUID.randomUUID()}"
        db.stageEventDao().insert(
            StageEventEntity(
                recordId = recordId,
                stage = stage,
                occurredAt = at,
                source = EventSource.MANUAL,
                idempotencyKey = key
            )
        )
        db.syncOpDao().insert(
            SyncOpEntity(
                entityType = "EarningRecord",
                entityId = recordId,
                payload = """{"recordId":$recordId,"stage":"${stage.name}","currency":"${currency.name}"}""",
                idempotencyKey = key,
                createdAt = at,
                sizeBytes = 148
            )
        )
        recordId
    }

    // ── Discovery ───────────────────────────────────────────────────────────────────────────────

    fun observeListings(): Flow<List<ListingEntity>> = db.listingDao().observeAll()

    fun observeLastScan(): Flow<DiscoveryScanEntity?> = db.discoveryScanDao().observe()

    suspend fun listing(id: Long): ListingEntity? = db.listingDao().byId(id)

    /**
     * Runs a scan and stores what it found.
     *
     * A failed scan is not an error state for the screen: the previous results stay, and the scan
     * row records why the refresh did not happen so Discover can say "last scanned at 09:14"
     * instead of going blank. This is the same posture as the offline strip — a condition, stated,
     * not a dialog.
     */
    suspend fun refreshDiscovery(source: DiscoverySource, at: Instant = Instant.now()) {
        val result = runCatching { source.scan() }

        result.onSuccess { scan ->
            db.withTransaction {
                db.listingDao().replaceScan(scan.listings)
                db.discoveryScanDao().upsert(
                    DiscoveryScanEntity(
                        scannedAt = scan.scannedAt,
                        boardCount = scan.boardCount,
                        feedCount = scan.feedCount,
                        lastError = null
                    )
                )
            }
        }.onFailure { error ->
            val previous = db.discoveryScanDao().get()
            db.discoveryScanDao().upsert(
                DiscoveryScanEntity(
                    scannedAt = previous?.scannedAt ?: at,
                    boardCount = previous?.boardCount ?: 0,
                    feedCount = previous?.feedCount ?: 0,
                    lastError = error.message ?: "could not reach the sources"
                )
            )
        }
    }

    /**
     * Records the user's own estimate of how long a listing would take.
     *
     * No board publishes this and it is what the whole projection divides by, so it is kept across
     * rescans — see `ListingDao.replaceScan`.
     */
    suspend fun estimateListingHours(listingId: Long, estHours: Double, assessmentHours: Double) {
        val listing = db.listingDao().byId(listingId) ?: return
        db.listingDao().update(
            listing.copy(
                estHours = estHours,
                assessmentHours = assessmentHours,
                hoursEstimatedByUser = true
            )
        )
    }

    /**
     * Tracks a Discovery listing as a prospect.
     *
     * The hours start counting immediately: the assessment is logged as unpaid work against the
     * platform from the moment the user commits to trying, which is when it actually starts costing
     * them. A prospect is in the PRE phase, so it is not owed and never appears on Pipeline — but
     * it is in the platform's denominator, and if the calibration set takes twice as long as
     * advertised, that shows up in the effective rate whether or not this ever becomes work.
     */
    suspend fun trackProspect(
        listing: ListingEntity,
        at: Instant = Instant.now()
    ): Long? = db.withTransaction {
        val platform = db.platformDao().all().firstOrNull { it.name == listing.platformName }
            ?: return@withTransaction null

        val recordId = db.recordDao().insert(
            EarningRecordEntity(
                platformId = platform.id,
                grossMinor = listing.statedPayMinor,
                currency = listing.currency,
                hoursWorked = 0.0,
                // Only the assessment starts now; the work itself is not committed to yet.
                hoursUnpaid = listing.assessmentHours ?: 0.0,
                expectedWeekStart = LocalDate.ofInstant(at, NAIROBI)
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
                createdAt = at,
                description = listing.title
            )
        )
        db.stageEventDao().insert(
            StageEventEntity(
                recordId = recordId,
                stage = Stage.PROSPECT,
                occurredAt = at,
                source = EventSource.MANUAL,
                idempotencyKey = "prospect:${listing.id}:$recordId",
                note = "Tracked from ${listing.sourceLabel}"
            )
        )
        recordId
    }

    suspend fun currentStage(recordId: Long): Stage? = db.stageEventDao().latestForRecord(recordId)?.stage

    suspend fun stageHistory(recordId: Long): List<StageEventEntity> = db.stageEventDao().forRecord(recordId)

    fun observeQueuedWriteCount(): Flow<Int> = db.syncOpDao().observeQueuedCount()

    fun observeSyncOps(): Flow<List<SyncOpEntity>> = db.syncOpDao().observeAll()

    fun observeConflicts(): Flow<List<SyncOpEntity>> = db.syncOpDao().observeConflicts()

    fun observeBytesToSend(): Flow<Int> = db.syncOpDao().observeBytesToSend()

    fun observeFxRates(): Flow<List<FxRateEntity>> = db.fxRateDao().observeAll()

    /**
     * Resolving a conflict appends a **new** `StageEvent` recording the decision. Nothing is
     * rewritten, and the losing side stays in the log.
     *
     * Resolving in favour of the platform does **not** discard logged hours. The record moves to
     * whatever the platform says — usually `REJECTED` — and the hours stay against the platform,
     * dragging its effective rate down exactly as they should. That is the whole point of the app,
     * and it is the one thing a conflict resolution must not quietly undo.
     */
    suspend fun resolveConflict(opId: Long, takeTheirs: Boolean, at: Instant = Instant.now()) {
        db.withTransaction {
            val op = db.syncOpDao().byId(opId) ?: return@withTransaction
            if (takeTheirs) {
                val stage = op.remoteStage ?: return@withTransaction
                db.stageEventDao().insert(
                    StageEventEntity(
                        recordId = op.entityId,
                        stage = stage,
                        occurredAt = op.remoteOccurredAt ?: at,
                        source = op.remoteSource ?: EventSource.PLATFORM_API,
                        idempotencyKey = "conflict:${op.id}:theirs",
                        note = "Conflict resolved in favour of the platform"
                    )
                )
            }
            // Either way the op leaves the queue: keeping mine means the local event already in the
            // log is the answer, and there is nothing further to send.
            db.syncOpDao().update(op.copy(state = SyncOpState.DONE, nextAttemptAt = null))
        }
    }

    /** Puts an op back in conflict, for the dev seed and for tests. */
    suspend fun markConflict(
        opId: Long,
        remoteStage: Stage,
        remoteOccurredAt: Instant,
        source: EventSource = EventSource.PLATFORM_API
    ) {
        val op = db.syncOpDao().byId(opId) ?: return
        db.syncOpDao().update(
            op.copy(
                state = SyncOpState.CONFLICT,
                remoteStage = remoteStage,
                remoteOccurredAt = remoteOccurredAt,
                remoteSource = source
            )
        )
    }
}
