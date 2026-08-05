package app.cleared.ui.addrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.model.Currency
import app.cleared.data.model.Stage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

data class AddRecordUiState(
    val platforms: List<PlatformEntity> = emptyList(),
    val platformId: Long? = null,
    val currency: Currency = Currency.USD,
    val amount: String = "",
    val hours: Double = 0.0,
    /** Assessments, calibration, onboarding — hours that count even if nothing is ever paid. */
    val unpaid: Boolean = false,
    val stage: Stage = Stage.SUBMITTED,
    val today: Instant = Instant.now(),
    val saving: Boolean = false
) {
    val canSave: Boolean
        get() = platformId != null && amount.toBigDecimalOrNull()?.signum() == 1 && !saving
}

private fun String.toBigDecimalOrNull(): BigDecimal? = trim().takeIf { it.isNotEmpty() }?.let {
    runCatching { BigDecimal(it) }.getOrNull()
}

/**
 * Frame `1f`.
 *
 * Defaults come from the last record on the selected platform — its currency and the hours it took
 * — which is what makes the sheet completable in under ten seconds on the second use.
 */
class AddRecordViewModel(
    private val repository: ClearedRepository,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val _state = MutableStateFlow(AddRecordUiState(today = clock()))
    val state: StateFlow<AddRecordUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val platforms = repository.platforms()
            _state.update { current ->
                val first = current.platformId ?: platforms.firstOrNull()?.id
                current.copy(platforms = platforms, platformId = first)
                    .let { if (first != null) it.applyDefaultsFor(first, platforms) else it }
            }
        }
    }

    fun selectPlatform(id: Long) {
        viewModelScope.launch {
            val defaults = repository.lastRecordFor(id)
            _state.update { current ->
                current.copy(
                    platformId = id,
                    currency = defaults?.currency
                        ?: current.platforms.firstOrNull { it.id == id }?.payCurrency
                        ?: current.currency,
                    hours = defaults?.hoursWorked ?: current.hours
                )
            }
        }
    }

    private fun AddRecordUiState.applyDefaultsFor(id: Long, platforms: List<PlatformEntity>) =
        copy(currency = platforms.firstOrNull { it.id == id }?.payCurrency ?: currency)

    fun selectCurrency(currency: Currency) = _state.update { it.copy(currency = currency) }

    fun changeAmount(text: String) {
        // One decimal separator, digits only — the field is a figure, not free text.
        val cleaned = text.filter { it.isDigit() || it == '.' }
        val normalised = cleaned.indexOf('.').let { first ->
            if (first < 0) cleaned
            else cleaned.substring(0, first + 1) + cleaned.substring(first + 1).filter { it.isDigit() }
        }
        _state.update { it.copy(amount = normalised) }
    }

    fun changeHours(hours: Double) = _state.update { it.copy(hours = hours) }

    fun toggleUnpaid(unpaid: Boolean) = _state.update { it.copy(unpaid = unpaid) }

    fun selectStage(stage: Stage) = _state.update { it.copy(stage = stage) }

    /**
     * Writes the record and its opening stage event in one transaction, so a record can never exist
     * without a stage to derive.
     */
    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        val platformId = current.platformId ?: return
        val amount = current.amount.toBigDecimalOrNull() ?: return

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = repository.createRecord(
                platformId = platformId,
                amount = amount,
                currency = current.currency,
                hoursWorked = if (current.unpaid) 0.0 else current.hours,
                hoursUnpaid = if (current.unpaid) current.hours else 0.0,
                stage = current.stage,
                at = clock()
            )
            _state.update { AddRecordUiState(platforms = it.platforms, platformId = platformId, today = clock()) }
            onSaved(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                AddRecordViewModel(app.repository)
            }
        }
    }
}
