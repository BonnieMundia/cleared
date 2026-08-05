package app.cleared.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.derive.CalendarDays
import app.cleared.data.model.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

/** The calculator's inputs are transient — they belong to the screen, not to the database. */
private data class CalculatorInput(val amount: String, val currency: Currency)

class MoneyViewModel(
    repository: ClearedRepository,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val input = MutableStateFlow(CalculatorInput("300", Currency.USD))

    val state: StateFlow<MoneyUiState> = combine(
        repository.observeWallets(),
        repository.observeRoutes(),
        repository.observeRecordStates(),
        repository.observeRates(),
        input
    ) { wallets, routes, states, rates, calculator ->
        val now = clock()
        MoneyMapper.build(
            wallets = wallets,
            routes = routes,
            states = states,
            rates = rates,
            amount = calculator.amount,
            currency = calculator.currency,
            year = now.atZone(CalendarDays.ZONE).year,
            now = now
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyUiState())

    fun changeAmount(text: String) {
        val cleaned = text.filter { it.isDigit() || it == '.' }
        val normalised = cleaned.indexOf('.').let { first ->
            if (first < 0) cleaned
            else cleaned.substring(0, first + 1) + cleaned.substring(first + 1).filter { it.isDigit() }
        }
        input.value = input.value.copy(amount = normalised)
    }

    fun changeCurrency(currency: Currency) {
        input.value = input.value.copy(currency = currency)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                MoneyViewModel(app.repository)
            }
        }
    }
}
