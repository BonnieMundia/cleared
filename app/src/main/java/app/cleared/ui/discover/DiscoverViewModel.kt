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
import app.cleared.data.db.entity.DiscoveryScanEntity
import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.db.entity.WithdrawalRouteEntity
import app.cleared.data.derive.RecordState
import app.cleared.data.discovery.DiscoverySource
import app.cleared.data.discovery.ScanResult
import app.cleared.data.model.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

/**
 * Reads listings from the database, not from the source.
 *
 * The scan writes; the screen reads. That is what lets Discover show its last successful scan with
 * a staleness line when there is no network, instead of an empty screen.
 */
class DiscoverViewModel(
    private val repository: ClearedRepository,
    private val source: DiscoverySource,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val filter = MutableStateFlow(DiscoverFilter.All)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshDiscovery(source, clock()) }
    }

    val state: StateFlow<DiscoverUiState> = combine(
        repository.observeListings(),
        repository.observeLastScan(),
        repository.observePlatforms(),
        repository.observeRoutes(),
        repository.observeRecordStates(),
        repository.observeRates(),
        filter
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DiscoverMapper.build(
            scan = scanOf(values[0] as List<ListingEntity>, values[1] as DiscoveryScanEntity?, clock()),
            platforms = values[2] as List<PlatformEntity>,
            routes = values[3] as List<WithdrawalRouteEntity>,
            states = values[4] as List<RecordState>,
            rates = values[5] as Map<Currency, BigDecimal>,
            filter = values[6] as DiscoverFilter,
            now = clock()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    fun selectFilter(value: DiscoverFilter) {
        filter.value = value
    }

    companion object {
        /** Stored listings plus the scan they came from, in the shape the mapper expects. */
        internal fun scanOf(
            listings: List<ListingEntity>,
            scan: DiscoveryScanEntity?,
            now: Instant
        ) = ScanResult(
            listings = listings,
            scannedAt = scan?.scannedAt ?: now,
            boardCount = scan?.boardCount ?: 0,
            feedCount = scan?.feedCount ?: 0
        )

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
    private val listingId: Long,
    private val clock: () -> Instant = Instant::now
) : ViewModel() {

    private val _tracked = MutableStateFlow(false)
    val tracked: StateFlow<Boolean> = _tracked

    val state: StateFlow<ListingDetailUiState> = combine(
        repository.observeListings(),
        repository.observeLastScan(),
        repository.observePlatforms(),
        repository.observeRoutes(),
        repository.observeRecordStates(),
        repository.observeRates()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DiscoverMapper.detail(
            listingId = listingId,
            scan = DiscoverViewModel.scanOf(
                values[0] as List<ListingEntity>,
                values[1] as DiscoveryScanEntity?,
                clock()
            ),
            platforms = values[2] as List<PlatformEntity>,
            routes = values[3] as List<WithdrawalRouteEntity>,
            states = values[4] as List<RecordState>,
            rates = values[5] as Map<Currency, BigDecimal>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListingDetailUiState())

    /**
     * The user's own estimate of how long this would take. It is the one number no source can
     * supply, and it survives rescans.
     */
    fun estimateHours(estHours: Double, assessmentHours: Double) {
        viewModelScope.launch {
            repository.estimateListingHours(listingId, estHours, assessmentHours)
        }
    }

    /**
     * Tracking starts the clock on unpaid hours.
     *
     * The prospect becomes a real record with the assessment hours already against the platform, so
     * they land in its effective rate whether or not this ever becomes a submission.
     */
    fun trackAsProspect() {
        viewModelScope.launch {
            val listing = repository.listing(listingId) ?: return@launch
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
                ListingDetailViewModel(app.repository, id)
            }
        }
    }
}
