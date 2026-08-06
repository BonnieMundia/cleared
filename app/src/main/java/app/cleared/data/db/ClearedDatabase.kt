package app.cleared.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cleared.data.db.entity.ConversionSnapshotEntity
import app.cleared.data.db.entity.DiscoveryScanEntity
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
        ListingEntity::class,
        DiscoveryScanEntity::class
    ],
    version = 4,
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
    abstract fun discoveryScanDao(): DiscoveryScanDao

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

        /** Adds what a conflicted op has to remember: the platform's side of the disagreement. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sync_op` ADD COLUMN `label` TEXT")
                db.execSQL("ALTER TABLE `sync_op` ADD COLUMN `remoteStage` TEXT")
                db.execSQL("ALTER TABLE `sync_op` ADD COLUMN `remoteOccurredAt` INTEGER")
                db.execSQL("ALTER TABLE `sync_op` ADD COLUMN `remoteSource` TEXT")
                db.execSQL("ALTER TABLE `sync_op` ADD COLUMN `lastError` TEXT")
            }
        }

        /**
         * Makes a listing's hours nullable and adds the scan record.
         *
         * SQLite cannot relax a NOT NULL column in place, so the table is rebuilt. Nothing is lost:
         * listings were never persisted before this version, so the copy is over an empty table in
         * practice — but it is written properly rather than dropped, because the next person to read
         * this migration should not learn that dropping user data is an acceptable shortcut here.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `listing_new` (
                        `id` INTEGER NOT NULL,
                        `platformName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `statedPayMinor` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `estHours` REAL,
                        `assessmentHours` REAL,
                        `sourceLabel` TEXT NOT NULL,
                        `sourceUrl` TEXT,
                        `seenAt` INTEGER NOT NULL,
                        `note` TEXT,
                        `hoursEstimatedByUser` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `listing_new`
                        (id, platformName, title, kind, statedPayMinor, currency,
                         estHours, assessmentHours, sourceLabel, sourceUrl, seenAt, note,
                         hoursEstimatedByUser)
                    SELECT id, platformName, title, kind, statedPayMinor, currency,
                           estHours, assessmentHours, sourceLabel, sourceUrl, seenAt, note, 0
                    FROM `listing`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `listing`")
                db.execSQL("ALTER TABLE `listing_new` RENAME TO `listing`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `discovery_scan` (
                        `id` INTEGER NOT NULL,
                        `scannedAt` INTEGER NOT NULL,
                        `boardCount` INTEGER NOT NULL,
                        `feedCount` INTEGER NOT NULL,
                        `lastError` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile private var instance: ClearedDatabase? = null

        fun get(context: Context): ClearedDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ClearedDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }
    }
}
