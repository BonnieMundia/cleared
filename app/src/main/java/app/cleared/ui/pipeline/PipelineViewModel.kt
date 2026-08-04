package app.cleared.ui.pipeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.derive.SettleTime
import app.cleared.data.model.Stage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What an undo snackbar needs to put a record back where it was. */
data class UndoableAdvance(
    val recordId: Long,
    val platformName: String,
    val from: Stage,
    val to: Stage
)

/**
 * Pipeline's state, assembled from the repository's flows. Nothing is cached: advancing a record
 * appends an event, the record flow re-emits, and the hero, the split bar and the week subtotals
 * all recompute from the log.
 */
class PipelineViewModel(
    private val repository: ClearedRepository,
    private val zone: ZoneId = ZoneId.of("Africa/Nairobi"),
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val undoRequests = MutableStateFlow<UndoableAdvance?>(null)
    val pendingUndo: StateFlow<UndoableAdvance?> = undoRequests

    val state: StateFlow<PipelineUiState> = combine(
        repository.observeRecordStates(),
        repository.observeRates(),
        repository.observePlatforms(),
        repository.observeQueuedWriteCount()
    ) { states, rates, platforms, queued ->
        val now = clock()
        PipelineMapper.build(
            states = states,
            platforms = platforms,
            rates = rates,
            p90ByPlatform = SettleTime.p90ByPlatform(platforms.map { it.id }, states, now),
            now = now,
            today = LocalDate.ofInstant(now, zone),
            queuedWrites = queued
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PipelineUiState())

    /**
     * Tap advances one stage. Landed, rejected and reversed rows do not move, and the tap is
     * simply ignored rather than explained — there is nothing to explain.
     */
    fun advance(recordId: Long, platformName: String) {
        viewModelScope.launch {
            val from = repository.currentStage(recordId) ?: return@launch
            val to = repository.advance(recordId, at = clock()) ?: return@launch
            undoRequests.value = UndoableAdvance(recordId, platformName, from, to)
        }
    }

    /**
     * Undo, never confirm. The previous stage is appended as a new event — the log keeps both, so
     * the record's history shows the correction rather than hiding it.
     */
    fun undo(advance: UndoableAdvance) {
        viewModelScope.launch {
            repository.revertTo(advance.recordId, advance.from, at = clock())
            undoRequests.value = null
        }
    }

    fun undoHandled() {
        undoRequests.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                PipelineViewModel(app.repository)
            }
        }
    }
}
