package app.cleared.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.sync.Connectivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class SyncViewModel(
    private val repository: ClearedRepository,
    connectivity: Connectivity,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    /** When the connection was last present — what "Offline since 09:14" is counted from. */
    private val lastOnlineAt = MutableStateFlow<Instant?>(null)

    private val online = connectivity.isOnline
        .onEach { if (it) lastOnlineAt.value = clock() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val state: StateFlow<SyncUiState> = combine(
        online,
        repository.observeSyncOps(),
        repository.observeRecordStates(),
        repository.observePlatforms(),
        repository.observeFxRates()
    ) { isOnline, ops, states, platforms, rates ->
        SyncMapper.build(
            online = isOnline,
            ops = ops,
            states = states,
            platforms = platforms,
            rates = rates,
            lastSyncedAt = lastOnlineAt.value,
            now = clock()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    fun resolve(opId: Long, takeTheirs: Boolean) {
        viewModelScope.launch { repository.resolveConflict(opId, takeTheirs, clock()) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                SyncViewModel(app.repository, app.connectivity)
            }
        }
    }
}
