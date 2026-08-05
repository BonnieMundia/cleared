package app.cleared.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import app.cleared.data.model.BankDestination
import app.cleared.data.model.Currency
import app.cleared.data.model.EventSource
import app.cleared.data.model.FeeKind
import app.cleared.data.model.PayoutDestination
import app.cleared.data.model.PlatformKind
import app.cleared.data.model.Stage
import app.cleared.data.model.SyncOpState
import app.cleared.data.model.WalletProvider
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Plain text name only. No logos, no brand marks — a product requirement, not an oversight. */
@Entity(tableName = "platform")
data class PlatformEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: PlatformKind,
    val payCurrency: Currency,
    val commissionPct: Double,
    val payoutDestination: PayoutDestination,
    val isCompany: Boolean,
    /** User-configurable slack on top of p90 before a record is flagged overdue. */
    val graceDays: Int = 0
)

/**
 * There is no `stage` column and no `currentStage` column, deliberately. Stage is derived from the
 * append-only [StageEventEntity] log.
 */
@Entity(
    tableName = "earning_record",
    foreignKeys = [
        ForeignKey(
            entity = PlatformEntity::class,
            parentColumns = ["id"],
            childColumns = ["platformId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("platformId"), Index("expectedWeekStart"), Index("supersedesRecordId")]
)
data class EarningRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformId: Long,
    /** Minor units of [currency]. Scaled Long, never a Double. */
    val grossMinor: Long,
    val currency: Currency,
    val hoursWorked: Double,
    /** Assessments, calibration, onboarding. Counted even when the record is rejected. */
    val hoursUnpaid: Double,
    val externalRef: String? = null,
    val expectedWeekStart: LocalDate,
    val createdAt: Instant,
    val description: String? = null,
    /** Set on a re-issue. Points at the record whose payout was reversed. */
    val supersedesRecordId: Long? = null,
    /** False on a re-issue — the hours were already counted on the predecessor. */
    val carriesHours: Boolean = true
)

/**
 * Append-only. Never updated, never deleted. A correction is a new event; a conflict resolution is
 * a new event whose [source] records who won.
 *
 * [stageOrder] is a write-time denormalisation of `Stage.order`, present only so the
 * greatest-occurredAt query can break ties in SQL. It is derived, never independently set.
 */
@Entity(
    tableName = "stage_event",
    foreignKeys = [
        ForeignKey(
            entity = EarningRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recordId", "occurredAt"], orders = [Index.Order.ASC, Index.Order.DESC]),
        Index("settlementId"),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class StageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    /** Money-phase events may attach to a settlement. Work-phase events never do. */
    val settlementId: Long? = null,
    val stage: Stage,
    val stageOrder: Int = stage.order,
    /** When it happened, not when it was written. */
    val occurredAt: Instant,
    val source: EventSource,
    /** Unique. Makes offline replay safe under double-delivery. */
    val idempotencyKey: String,
    val note: String? = null
)

/**
 * Some platforms pay a fraction on approval and the rest on publication. The record stays whole;
 * only the money divides. There is no `hoursWorked` here and there never will be — hours are never
 * split and the effective rate is a property of the record.
 */
@Entity(
    tableName = "settlement",
    foreignKeys = [
        ForeignKey(
            entity = EarningRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId")]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val label: String,
    /** Must sum to 1.0 across a record — validated on write. */
    val fraction: Double,
    val amountMinor: Long,
    val sequence: Int,
    val expectedWeekStart: LocalDate? = null
)

/** Amounts are always positive; the minus sign is a rendering decision. */
@Entity(
    tableName = "fee_line",
    foreignKeys = [
        ForeignKey(
            entity = EarningRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId"), Index("settlementId")]
)
data class FeeLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val settlementId: Long? = null,
    val kind: FeeKind,
    val label: String,
    /** Almost always false. A reversal returns the principal and keeps the fees. */
    val refundable: Boolean = false,
    val amountMinor: Long,
    val currency: Currency,
    val occurredAt: Instant
)

/** Landed records keep the rate they actually converted at, forever. */
@Entity(
    tableName = "conversion_snapshot",
    foreignKeys = [
        ForeignKey(
            entity = EarningRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId"), Index("settlementId")]
)
data class ConversionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val settlementId: Long? = null,
    val fromCurrency: Currency,
    val rateApplied: BigDecimal,
    val midRate: BigDecimal,
    val appliedAt: Instant
)

/** The current mid, one row per currency. Only unlanded estimates move when this refreshes. */
@Entity(tableName = "fx_rate")
data class FxRateEntity(
    @PrimaryKey val currency: Currency,
    val midToKes: BigDecimal,
    val fetchedAt: Instant
)

@Entity(tableName = "wallet_balance", primaryKeys = ["provider", "currency"])
data class WalletBalanceEntity(
    val provider: WalletProvider,
    val currency: Currency,
    val amountMinor: Long,
    val observedAt: Instant,
    val idleSince: Instant?
)

@Entity(tableName = "withdrawal_route")
data class WithdrawalRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: WalletProvider,
    val destination: BankDestination,
    val label: String,
    val flatFeeMinor: Long,
    val feeCurrency: Currency,
    val spreadPct: Double,
    val medianDays: Int,
    val note: String? = null,
    val dailyCapKes: Long? = null
)

/** The offline queue. Append-only, replayed in ascending [id]. */
@Entity(tableName = "sync_op", indices = [Index("state")])
data class SyncOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val payload: String,
    val idempotencyKey: String,
    val createdAt: Instant,
    val attempts: Int = 0,
    val nextAttemptAt: Instant? = null,
    val state: SyncOpState = SyncOpState.WAITING,
    /** Surfaced on the Sync screen — the user is on metered mobile data. */
    val sizeBytes: Int
)

/**
 * The Tax screen's own settings — the two rates and what has actually been put aside.
 *
 * design/DATA_MODEL.md does not give this a table, but it requires one: "Both rates must be
 * user-editable — tax law changes and this app should not need a release to follow it." Frame `1d`
 * also shows where the money is held and when it last moved, and neither is derivable from records.
 *
 * A single row, id 0.
 */
@Entity(tableName = "tax_settings")
data class TaxSettingsEntity(
    @PrimaryKey val id: Long = 0,
    val personalRate: Double = 0.25,
    val turnoverTaxRate: Double = 0.03,
    val actualSetAsideKes: Long = 0,
    val setAsideLocation: String? = null,
    val setAsideLastMoved: LocalDate? = null
)

@Entity(tableName = "listing")
data class ListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformName: String,
    val title: String,
    val kind: String,
    val statedPayMinor: Long,
    val currency: Currency,
    val estHours: Double,
    val assessmentHours: Double,
    val sourceLabel: String,
    val sourceUrl: String? = null,
    val seenAt: Instant,
    val note: String? = null
)

/**
 * A record with everything hanging off it. This is what the derivations consume, which is why they
 * are testable without Room: it is an ordinary data class.
 */
data class RecordDetail(
    @Embedded val record: EarningRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val events: List<StageEventEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val settlements: List<SettlementEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val fees: List<FeeLineEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val conversions: List<ConversionSnapshotEntity> = emptyList()
)
