package app.cleared.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.FeeLineEntity
import app.cleared.data.db.entity.FxRateEntity
import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.db.entity.SettlementEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.db.entity.TaxSettingsEntity
import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.model.SyncOpState
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface PlatformDao {
    @Query("SELECT * FROM platform ORDER BY name") fun observeAll(): Flow<List<PlatformEntity>>
    @Query("SELECT * FROM platform") suspend fun all(): List<PlatformEntity>
    @Query("SELECT * FROM platform WHERE id = :id") suspend fun byId(id: Long): PlatformEntity?
    @Upsert suspend fun upsert(platform: PlatformEntity): Long
    @Upsert suspend fun upsertAll(platforms: List<PlatformEntity>)
}

@Dao
interface RecordDao {
    @Transaction
    @Query("SELECT * FROM earning_record")
    fun observeDetails(): Flow<List<RecordDetail>>

    @Transaction
    @Query("SELECT * FROM earning_record")
    suspend fun allDetails(): List<RecordDetail>

    @Transaction
    @Query("SELECT * FROM earning_record WHERE id = :id")
    fun observeDetail(id: Long): Flow<RecordDetail?>

    @Transaction
    @Query("SELECT * FROM earning_record WHERE platformId = :platformId")
    suspend fun detailsForPlatform(platformId: Long): List<RecordDetail>

    @Insert suspend fun insert(record: EarningRecordEntity): Long
    @Insert suspend fun insertAll(records: List<EarningRecordEntity>): List<Long>

    /**
     * Records themselves are editable — an amount typed wrong is a typo, not a history. The stage
     * log is what is append-only.
     */
    @Update suspend fun update(record: EarningRecordEntity)
}

/**
 * Append-only by construction: there is no `@Update` and no `@Delete` on this DAO and there must
 * never be one. A correction is a new event. A conflict resolution is a new event whose `source`
 * records who won.
 *
 * [insert] ignores conflicts on `idempotencyKey`, which is what makes replaying the offline queue
 * safe under double-delivery.
 */
@Dao
interface StageEventDao {
    @Query("SELECT * FROM stage_event WHERE recordId = :recordId ORDER BY occurredAt ASC, stageOrder ASC, id ASC")
    suspend fun forRecord(recordId: Long): List<StageEventEntity>

    @Query(
        """
        SELECT * FROM stage_event
        WHERE recordId = :recordId AND settlementId IS NULL
        ORDER BY occurredAt DESC, stageOrder DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun latestForRecord(recordId: Long): StageEventEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: StageEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<StageEventEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM stage_event WHERE idempotencyKey = :key")
    suspend fun countByKey(key: String): Int
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlement WHERE recordId = :recordId ORDER BY sequence")
    suspend fun forRecord(recordId: Long): List<SettlementEntity>

    @Insert suspend fun insertAll(settlements: List<SettlementEntity>): List<Long>
}

@Dao
interface FeeLineDao {
    @Query("SELECT * FROM fee_line WHERE recordId = :recordId") suspend fun forRecord(recordId: Long): List<FeeLineEntity>
    @Insert suspend fun insertAll(fees: List<FeeLineEntity>): List<Long>
}

@Dao
interface ConversionDao {
    @Insert suspend fun insertAll(snapshots: List<ConversionSnapshotEntity>): List<Long>
    @Query("SELECT * FROM conversion_snapshot WHERE recordId = :recordId")
    suspend fun forRecord(recordId: Long): List<ConversionSnapshotEntity>
}

@Dao
interface FxRateDao {
    @Query("SELECT * FROM fx_rate") fun observeAll(): Flow<List<FxRateEntity>>
    @Query("SELECT * FROM fx_rate") suspend fun all(): List<FxRateEntity>
    @Upsert suspend fun upsertAll(rates: List<FxRateEntity>)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_balance") fun observeAll(): Flow<List<WalletBalanceEntity>>
    @Upsert suspend fun upsertAll(balances: List<WalletBalanceEntity>)
}

@Dao
interface WithdrawalRouteDao {
    @Query("SELECT * FROM withdrawal_route") fun observeAll(): Flow<List<WithdrawalRouteEntity>>
    @Query("SELECT * FROM withdrawal_route") suspend fun all(): List<WithdrawalRouteEntity>
    @Upsert suspend fun upsertAll(routes: List<WithdrawalRouteEntity>)
}

/** Replayed in ascending id — the id is the ordering key, not `createdAt`. */
@Dao
interface SyncOpDao {
    @Query("SELECT * FROM sync_op WHERE state IN (:states) ORDER BY id ASC")
    suspend fun pending(states: List<SyncOpState> = listOf(SyncOpState.WAITING, SyncOpState.RETRYING)): List<SyncOpEntity>

    @Query("SELECT * FROM sync_op ORDER BY id ASC") fun observeAll(): Flow<List<SyncOpEntity>>

    @Query("SELECT COUNT(*) FROM sync_op WHERE state IN ('WAITING','RETRYING')")
    fun observeQueuedCount(): Flow<Int>

    @Insert suspend fun insert(op: SyncOpEntity): Long

    @Query("UPDATE sync_op SET state = :state, attempts = :attempts, nextAttemptAt = :nextAttemptAt WHERE id = :id")
    suspend fun markAttempt(id: Long, state: SyncOpState, attempts: Int, nextAttemptAt: Instant?)
}

@Dao
interface TaxSettingsDao {
    @Query("SELECT * FROM tax_settings WHERE id = 0") fun observe(): Flow<TaxSettingsEntity?>
    @Query("SELECT * FROM tax_settings WHERE id = 0") suspend fun get(): TaxSettingsEntity?
    @Upsert suspend fun upsert(settings: TaxSettingsEntity)
}

@Dao
interface ListingDao {
    @Query("SELECT * FROM listing ORDER BY seenAt DESC") fun observeAll(): Flow<List<ListingEntity>>
    @Upsert suspend fun upsertAll(listings: List<ListingEntity>)
}
