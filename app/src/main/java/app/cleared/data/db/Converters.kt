package app.cleared.data.db

import androidx.room.TypeConverter
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

/**
 * Instants are epoch millis UTC; dates are epoch days. Rates are exact decimals held as strings —
 * they are not money, but they multiply money, so they must not go through a Double.
 */
class Converters {

    @TypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun decimalToString(value: BigDecimal?): String? = value?.toPlainString()
    @TypeConverter fun stringToDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter fun stageToName(value: Stage?): String? = value?.name
    @TypeConverter fun nameToStage(value: String?): Stage? = value?.let(Stage::valueOf)

    @TypeConverter fun currencyToName(value: Currency?): String? = value?.name
    @TypeConverter fun nameToCurrency(value: String?): Currency? = value?.let(Currency::valueOf)

    @TypeConverter fun kindToName(value: PlatformKind?): String? = value?.name
    @TypeConverter fun nameToKind(value: String?): PlatformKind? = value?.let(PlatformKind::valueOf)

    @TypeConverter fun destToName(value: PayoutDestination?): String? = value?.name
    @TypeConverter fun nameToDest(value: String?): PayoutDestination? = value?.let(PayoutDestination::valueOf)

    @TypeConverter fun providerToName(value: WalletProvider?): String? = value?.name
    @TypeConverter fun nameToProvider(value: String?): WalletProvider? = value?.let(WalletProvider::valueOf)

    @TypeConverter fun bankToName(value: BankDestination?): String? = value?.name
    @TypeConverter fun nameToBank(value: String?): BankDestination? = value?.let(BankDestination::valueOf)

    @TypeConverter fun feeKindToName(value: FeeKind?): String? = value?.name
    @TypeConverter fun nameToFeeKind(value: String?): FeeKind? = value?.let(FeeKind::valueOf)

    @TypeConverter fun sourceToName(value: EventSource?): String? = value?.name
    @TypeConverter fun nameToSource(value: String?): EventSource? = value?.let(EventSource::valueOf)

    @TypeConverter fun syncStateToName(value: SyncOpState?): String? = value?.name
    @TypeConverter fun nameToSyncState(value: String?): SyncOpState? = value?.let(SyncOpState::valueOf)
}
