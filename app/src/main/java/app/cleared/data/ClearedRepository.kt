package app.cleared.data

import androidx.room.withTransaction
import app.cleared.data.db.ClearedDatabase
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.derive.Pipeline
import app.cleared.data.derive.PipelineTotals
import app.cleared.data.derive.PlatformStatistics
import app.cleared.data.derive.PlatformStats
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.SettleTime
import app.cleared.data.derive.StageResolver
import app.cleared.data.model.Currency
import app.cleared.data.model.EventSource
import app.cleared.data.model.Stage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * The one place the derived views are assembled. Everything it exposes is computed from the tables;
 * nothing here reads a cached figure.
 */
class ClearedRepository(private val db: ClearedDatabase) {

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
            val key = "undo:$recordId:${stage.name}:${UUID.randomUUID()}"
            db.stageEventDao().insert(
                StageEventEntity(
                    recordId = recordId,
                    stage = stage,
                    occurredAt = at,
                    source = EventSource.MANUAL,
                    idempotencyKey = key,
                    note = "Undo"
                )
            )
        }
    }

    suspend fun currentStage(recordId: Long): Stage? = db.stageEventDao().latestForRecord(recordId)?.stage

    suspend fun stageHistory(recordId: Long): List<StageEventEntity> = db.stageEventDao().forRecord(recordId)

    fun observeQueuedWriteCount(): Flow<Int> = db.syncOpDao().observeQueuedCount()
}
