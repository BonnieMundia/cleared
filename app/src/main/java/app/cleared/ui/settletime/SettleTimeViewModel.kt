package app.cleared.ui.settletime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class SettleTimeViewModel(
    repository: ClearedRepository,
    private val platformId: Long,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    val state: StateFlow<SettleTimeUiState> = combine(
        repository.observeRecordStates(),
        repository.observePlatforms()
    ) { states, platforms ->
        val platform = platforms.firstOrNull { it.id == platformId }
            ?: return@combine SettleTimeUiState(loading = false)
        SettleTimeMapper.build(platform, states, clock())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettleTimeUiState())

    companion object {
        const val ARG_PLATFORM_ID = "platformId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<String>(ARG_PLATFORM_ID)?.toLongOrNull()
                    ?: handle.get<Long>(ARG_PLATFORM_ID)
                    ?: error("settle time opened without a $ARG_PLATFORM_ID")
                SettleTimeViewModel(app.repository, id)
            }
        }
    }
}
