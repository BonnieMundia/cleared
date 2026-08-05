package app.cleared.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.derive.SettleTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * One record, end to end. Which of the three shapes it takes — `1e`, `4a` or `4b` — is decided by
 * the record, not by the caller.
 */
class RecordDetailViewModel(
    private val repository: ClearedRepository,
    private val recordId: Long,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val successor = MutableStateFlow<RecordDetail?>(null)

    val state: StateFlow<RecordDetailUi?> = combine(
        repository.observeRecordDetail(recordId),
        repository.observeRates(),
        repository.observePlatforms(),
        repository.observeRecordStates(),
        successor
    ) { detail, rates, platforms, allStates, reissue ->
        detail ?: return@combine null
        val now = clock()
        RecordDetailMapper.build(
            detail = detail,
            platform = platforms.firstOrNull { it.id == detail.record.platformId },
            rates = rates,
            successor = reissue,
            p90Days = SettleTime.of(detail.record.platformId, allStates, now).p90Days,
            now = now
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { successor.value = repository.successorOf(recordId) }
    }

    companion object {
        const val ARG_RECORD_ID = "recordId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<String>(ARG_RECORD_ID)?.toLongOrNull()
                    ?: handle.get<Long>(ARG_RECORD_ID)
                    ?: error("record detail opened without a $ARG_RECORD_ID")
                RecordDetailViewModel(app.repository, id)
            }
        }
    }
}
