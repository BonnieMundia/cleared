package app.cleared.ui.platforms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class PlatformsViewModel(
    repository: ClearedRepository,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val sort = MutableStateFlow(PlatformSort.EffectiveRate)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<PlatformsUiState> = combine(
        repository.observeRecordStates(),
        repository.observePlatforms(),
        sort
    ) { states, platforms, currentSort ->
        PlatformsMapper.build(states, platforms, currentSort, clock())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlatformsUiState())

    fun selectSort(value: PlatformSort) {
        sort.value = value
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                PlatformsViewModel(app.repository)
            }
        }
    }
}
