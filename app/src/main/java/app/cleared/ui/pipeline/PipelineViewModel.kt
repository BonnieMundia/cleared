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

    /** Transient, and deliberately not persisted: a selection does not survive leaving the screen. */
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    private val bulkUndo = MutableStateFlow<List<UndoableAdvance>>(emptyList())
    val pendingBulkUndo: StateFlow<List<UndoableAdvance>> = bulkUndo

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

    // ── Bulk triage, frame `2d` ─────────────────────────────────────────────────────────────────

    fun toggleSelection(recordId: Long) {
        _selected.value = _selected.value.let { if (recordId in it) it - recordId else it + recordId }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    /**
     * Advances every selected record by one stage.
     *
     * Work-phase records go to their next stage and money-phase records to theirs — a bulk action
     * is many individual advances, not one shared destination, because the records are not all in
     * the same place. Anything terminal or landed is skipped rather than reported as an error.
     */
    fun advanceSelected() {
        val ids = _selected.value.toList().sorted()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val done = mutableListOf<UndoableAdvance>()
            val names = state.value.groups.flatMap { it.rows }.associate { it.id to it.platformName }

            for (id in ids) {
                val from = repository.currentStage(id) ?: continue
                val to = repository.advance(id, at = clock()) ?: continue
                done += UndoableAdvance(id, names[id] ?: "Record", from, to)
            }

            _selected.value = emptySet()
            if (done.isNotEmpty()) bulkUndo.value = done
        }
    }

    fun undoBulk(advances: List<UndoableAdvance>) {
        viewModelScope.launch {
            // Reversed, so a record advanced twice in one batch lands back where it started.
            advances.reversed().forEach { repository.revertTo(it.recordId, it.from, at = clock()) }
            bulkUndo.value = emptyList()
        }
    }

    fun bulkUndoHandled() {
        bulkUndo.value = emptyList()
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
