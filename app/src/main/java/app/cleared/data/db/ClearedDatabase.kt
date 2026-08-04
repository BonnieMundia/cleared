package app.cleared.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.FeeLineEntity
import app.cleared.data.db.entity.FxRateEntity
import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.SettlementEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.db.entity.WalletBalanceEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity

@Database(
    entities = [
        PlatformEntity::class,
        EarningRecordEntity::class,
        StageEventEntity::class,
        SettlementEntity::class,
        FeeLineEntity::class,
        ConversionSnapshotEntity::class,
        FxRateEntity::class,
        WalletBalanceEntity::class,
        WithdrawalRouteEntity::class,
        SyncOpEntity::class,
        ListingEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ClearedDatabase : RoomDatabase() {

    abstract fun platformDao(): PlatformDao
    abstract fun recordDao(): RecordDao
    abstract fun stageEventDao(): StageEventDao
    abstract fun settlementDao(): SettlementDao
    abstract fun feeLineDao(): FeeLineDao
    abstract fun conversionDao(): ConversionDao
    abstract fun fxRateDao(): FxRateDao
    abstract fun walletDao(): WalletDao
    abstract fun withdrawalRouteDao(): WithdrawalRouteDao
    abstract fun syncOpDao(): SyncOpDao
    abstract fun listingDao(): ListingDao

    companion object {
        private const val NAME = "cleared.db"

        @Volatile private var instance: ClearedDatabase? = null

        fun get(context: Context): ClearedDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ClearedDatabase::class.java, NAME)
                .build()
                .also { instance = it }
        }
    }
}
