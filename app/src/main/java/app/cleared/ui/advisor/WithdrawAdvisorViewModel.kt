package app.cleared.ui.advisor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.model.WalletProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class WithdrawAdvisorViewModel(
    repository: ClearedRepository,
    private val provider: WalletProvider
) : ViewModel() {

    val state: StateFlow<WithdrawAdvisorUiState> = combine(
        repository.observeWallets(),
        repository.observeRoutes(),
        repository.observePlatforms(),
        repository.observeRecordStates(),
        repository.observeRates()
    ) { wallets, routes, platforms, states, rates ->
        WithdrawAdvisorMapper.build(provider, wallets, routes, platforms, states, rates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WithdrawAdvisorUiState())

    companion object {
        const val ARG_PROVIDER = "provider"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                val name = handle.get<String>(ARG_PROVIDER)
                    ?: error("withdraw advisor opened without a $ARG_PROVIDER")
                WithdrawAdvisorViewModel(app.repository, WalletProvider.valueOf(name))
            }
        }
    }
}
