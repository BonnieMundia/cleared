package app.cleared.ui.tax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.derive.CalendarDays
import app.cleared.data.derive.Tax
import app.cleared.data.export.CsvExport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** A CSV ready to hand to the share sheet. The user chooses where it goes; the app never sends it. */
data class CsvPayload(val fileName: String, val content: String)

class TaxViewModel(
    private val repository: ClearedRepository,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    /** Null is the `All` tab. Defaults to the current year. */
    private val selectedYear = MutableStateFlow<Int?>(clock().atZone(CalendarDays.ZONE).year)

    private val _pendingExport = MutableStateFlow<CsvPayload?>(null)
    val pendingExport: StateFlow<CsvPayload?> = _pendingExport

    val state: StateFlow<TaxUiState> = combine(
        repository.observeRecordStates(),
        repository.observePlatforms(),
        repository.observeTaxSettings(),
        selectedYear
    ) { states, platforms, settings, year ->
        TaxMapper.build(
            states = states,
            platforms = platforms,
            selectedYear = year,
            actualSetAsideKes = settings?.actualSetAsideKes ?: 0,
            personalRate = settings?.personalRate ?: Tax.DEFAULT_PERSONAL_RATE,
            turnoverTaxRate = settings?.turnoverTaxRate ?: Tax.DEFAULT_TURNOVER_RATE,
            setAsideLocation = settings?.setAsideLocation,
            setAsideLastMoved = settings?.setAsideLastMoved
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaxUiState())

    fun selectYear(year: Int?) {
        selectedYear.value = year
    }

    /**
     * Builds the CSV and hands it to the screen, which offers it to the system share sheet. Nothing
     * is sent anywhere: the user picks the destination, or picks nothing.
     */
    fun export() {
        viewModelScope.launch {
            val states = repository.observeRecordStates().first()
            val platforms = repository.observePlatforms().first()
            val year = selectedYear.value
            _pendingExport.value = CsvPayload(
                fileName = CsvExport.fileName(year),
                content = CsvExport.build(states, platforms, year)
            )
        }
    }

    fun exportHandled() {
        _pendingExport.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                TaxViewModel(app.repository)
            }
        }
    }
}
