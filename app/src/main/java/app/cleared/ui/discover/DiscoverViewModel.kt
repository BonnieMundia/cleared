package app.cleared.ui.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cleared.ClearedApplication
import app.cleared.data.ClearedRepository
import app.cleared.data.discovery.DiscoverySource
import app.cleared.data.discovery.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class DiscoverViewModel(
    private val repository: ClearedRepository,
    private val source: DiscoverySource,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val scan = MutableStateFlow<ScanResult?>(null)
    private val filter = MutableStateFlow(DiscoverFilter.All)

    /** Set once a prospect has been tracked, so the screen can say so and pop back. */
    private val _tracked = MutableStateFlow<Long?>(null)
    val tracked: StateFlow<Long?> = _tracked

    init {
        // Discover is the one screen that needs a scan. It shows its last successful one and a
        // staleness line rather than blocking, which is why nothing here waits on it.
        viewModelScope.launch { scan.value = source.scan() }
    }

    val state: StateFlow<DiscoverUiState> = combine(
        scan.filterNotNull(),
        repository.observePlatforms(),
        repository.observeRoutes(),
        repository.observeRecordStates(),
        repository.observeRates(),
        filter
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DiscoverMapper.build(
            scan = values[0] as ScanResult,
            platforms = values[1] as List<app.cleared.data.db.entity.PlatformEntity>,
            routes = values[2] as List<app.cleared.data.db.entity.WithdrawalRouteEntity>,
            states = values[3] as List<app.cleared.data.derive.RecordState>,
            rates = values[4] as Map<app.cleared.data.model.Currency, java.math.BigDecimal>,
            filter = values[5] as DiscoverFilter,
            now = clock()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    fun selectFilter(value: DiscoverFilter) {
        filter.value = value
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                DiscoverViewModel(app.repository, app.discoverySource)
            }
        }
    }
}

class ListingDetailViewModel(
    private val repository: ClearedRepository,
    private val source: DiscoverySource,
    private val listingId: Long,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val scan = MutableStateFlow<ScanResult?>(null)
    private val _tracked = MutableStateFlow(false)
    val tracked: StateFlow<Boolean> = _tracked

    init {
        viewModelScope.launch { scan.value = source.scan() }
    }

    val state: StateFlow<ListingDetailUiState> = combine(
        scan.filterNotNull(),
        repository.observePlatforms(),
        repository.observeRoutes(),
        repository.observeRecordStates(),
        repository.observeRates()
    ) { scanned, platforms, routes, states, rates ->
        DiscoverMapper.detail(listingId, scanned, platforms, routes, states, rates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListingDetailUiState())

    /**
     * Tracking starts the clock on unpaid hours.
     *
     * The prospect becomes a real record with the assessment hours already against the platform, so
     * they land in its effective rate whether or not this ever becomes a submission. That is the
     * point of the pre-stage: the cost of trying is a cost.
     */
    fun trackAsProspect() {
        val listing = scan.value?.listings?.firstOrNull { it.id == listingId } ?: return
        viewModelScope.launch {
            repository.trackProspect(listing, at = clock())
            _tracked.value = true
        }
    }

    companion object {
        const val ARG_LISTING_ID = "listingId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ClearedApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<String>(ARG_LISTING_ID)?.toLongOrNull()
                    ?: error("listing detail opened without a $ARG_LISTING_ID")
                ListingDetailViewModel(app.repository, app.discoverySource, id)
            }
        }
    }
}
