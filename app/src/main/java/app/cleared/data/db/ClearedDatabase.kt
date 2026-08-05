package app.cleared.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.FeeLineEntity
import app.cleared.data.db.entity.FxRateEntity
import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.SettlementEntity
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.db.entity.TaxSettingsEntity
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
        TaxSettingsEntity::class,
        ListingEntity::class
    ],
    version = 2,
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
    abstract fun taxSettingsDao(): TaxSettingsDao
    abstract fun listingDao(): ListingDao

    companion object {
        private const val NAME = "cleared.db"

        /**
         * Adds the Tax screen's settings row. Written out rather than destructive: this database
         * holds financial records, and a migration that drops them to save five minutes of work is
         * not a trade anyone would accept.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tax_settings` (
                        `id` INTEGER NOT NULL,
                        `personalRate` REAL NOT NULL,
                        `turnoverTaxRate` REAL NOT NULL,
                        `actualSetAsideKes` INTEGER NOT NULL,
                        `setAsideLocation` TEXT,
                        `setAsideLastMoved` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile private var instance: ClearedDatabase? = null

        fun get(context: Context): ClearedDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ClearedDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
