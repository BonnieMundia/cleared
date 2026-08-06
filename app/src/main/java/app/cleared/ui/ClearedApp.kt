package app.cleared.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.cleared.ClearedApplication
import app.cleared.data.sync.SyncWorker
import app.cleared.ui.addrecord.AddRecordSheet
import app.cleared.ui.addrecord.AddRecordViewModel
import app.cleared.ui.advisor.WithdrawAdvisorScreen
import app.cleared.ui.advisor.WithdrawAdvisorViewModel
import app.cleared.ui.components.OfflineStrip
import app.cleared.ui.discover.DiscoverScreen
import app.cleared.ui.discover.DiscoverViewModel
import app.cleared.ui.discover.ListingDetailScreen
import app.cleared.ui.discover.ListingDetailViewModel
import app.cleared.ui.settletime.SettleTimeScreen
import app.cleared.ui.settletime.SettleTimeViewModel
import app.cleared.ui.nav.ClearedBottomBar
import app.cleared.ui.nav.ClearedDestination
import app.cleared.ui.pipeline.PipelineScreen
import app.cleared.ui.pipeline.PipelineViewModel
import app.cleared.ui.platforms.PlatformsScreen
import app.cleared.ui.platforms.PlatformsViewModel
import app.cleared.ui.money.MoneyScreen
import app.cleared.ui.money.MoneyViewModel
import app.cleared.ui.record.RecordDetailScreen
import app.cleared.ui.record.RecordDetailViewModel
import app.cleared.ui.sync.SyncScreen
import app.cleared.ui.sync.SyncViewModel
import app.cleared.ui.tax.TaxScreen
import app.cleared.ui.tax.TaxViewModel
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.Dimens
import java.io.File

/** Pushed, never a tab — reached by tapping the offline strip. */
const val SYNC_ROUTE = "sync"

/**
 * The single Activity's content: a bottom-navigated `NavHost` with Pipeline as the start
 * destination. Record detail, Add record, Sync and the analysis screens are pushed or modal and
 * never tabs.
 */
@Composable
fun ClearedApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = ClearedDestination.visible
        .firstOrNull { it.route == backStackEntry?.destination?.route }
        ?: ClearedDestination.Pipeline

    // Offline is a condition of the app rather than of one screen, so the strip is shared chrome:
    // every tab draws it under its own title, and every one of them keeps working under it.
    val app = LocalContext.current.applicationContext as ClearedApplication
    val online by app.connectivity.isOnline.collectAsState(initial = true)
    val queued by app.repository.observeQueuedWriteCount().collectAsState(initial = 0)

    val chrome = ScreenChrome(
        offline = !online,
        queuedWrites = queued,
        onOpenSync = { navController.navigate(SYNC_ROUTE) }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ClearedBottomBar(
                current = current,
                onSelect = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(ClearedDestination.Pipeline.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ClearedDestination.Pipeline.route,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            composable(ClearedDestination.Pipeline.route) {
                val viewModel: PipelineViewModel = viewModel(factory = PipelineViewModel.Factory)
                val state by viewModel.state.collectAsState()
                val undo by viewModel.pendingUndo.collectAsState()
                var sheetOpen by rememberSaveable { mutableStateOf(false) }

                val selected by viewModel.selected.collectAsState()
                val bulkUndo by viewModel.pendingBulkUndo.collectAsState()

                Column(Modifier.fillMaxSize()) {
                    // The contextual bar replaces the title while a selection is live.
                    if (selected.isEmpty()) ScreenTitle("Pipeline", chrome)
                    PipelineScreen(
                        state = state,
                        pendingUndo = undo,
                        modifier = Modifier.weight(1f),
                        selected = selected,
                        pendingBulkUndo = bulkUndo,
                        onAdvance = { viewModel.advance(it.id, it.platformName) },
                        onOpenRecord = { navController.navigate("record/${it.id}") },
                        onLongPressRecord = { viewModel.toggleSelection(it.id) },
                        onToggleSelection = { viewModel.toggleSelection(it.id) },
                        onClearSelection = viewModel::clearSelection,
                        onAdvanceSelected = viewModel::advanceSelected,
                        onUndo = viewModel::undo,
                        onUndoHandled = viewModel::undoHandled,
                        onUndoBulk = viewModel::undoBulk,
                        onBulkUndoHandled = viewModel::bulkUndoHandled,
                        onAddRecord = { sheetOpen = true },
                        onOpenSync = { navController.navigate(SYNC_ROUTE) }
                    )
                }

                if (sheetOpen) {
                    AddRecordHost(onDismiss = { sheetOpen = false })
                }
            }

            composable(
                route = "record/{${RecordDetailViewModel.ARG_RECORD_ID}}",
                arguments = listOf(navArgument(RecordDetailViewModel.ARG_RECORD_ID) {
                    type = NavType.StringType
                })
            ) {
                val viewModel: RecordDetailViewModel = viewModel(factory = RecordDetailViewModel.Factory)
                val state by viewModel.state.collectAsState()
                state?.let { detail ->
                    RecordDetailScreen(
                        state = detail,
                        onBack = { navController.popBackStack() },
                        onOpenReissue = { navController.navigate("record/$it") }
                    )
                }
            }

            composable(ClearedDestination.Platforms.route) {
                val viewModel: PlatformsViewModel = viewModel(factory = PlatformsViewModel.Factory)
                val state by viewModel.state.collectAsState()

                Column(Modifier.fillMaxSize()) {
                    ScreenTitle("Platforms", chrome)
                    PlatformsScreen(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onSelectSort = viewModel::selectSort,
                        onOpenSettleTime = { navController.navigate("settle/$it") }
                    )
                }
            }

            composable(ClearedDestination.Money.route) {
                val viewModel: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory)
                val state by viewModel.state.collectAsState()

                Column(Modifier.fillMaxSize()) {
                    ScreenTitle("Money", chrome)
                    MoneyScreen(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onAmountChanged = viewModel::changeAmount,
                        onCurrencyChanged = viewModel::changeCurrency,
                        onOpenAdvisor = { navController.navigate("advisor/$it") }
                    )
                }
            }

            composable(ClearedDestination.Tax.route) {
                val viewModel: TaxViewModel = viewModel(factory = TaxViewModel.Factory)
                val state by viewModel.state.collectAsState()
                val export by viewModel.pendingExport.collectAsState()
                val context = LocalContext.current

                // The export hands the file to the system share sheet. The user picks where it
                // goes, or picks nothing â€” the app never sends it anywhere itself.
                LaunchedEffect(export) {
                    val payload = export ?: return@LaunchedEffect
                    runCatching { shareCsv(context, payload.fileName, payload.content) }
                    viewModel.exportHandled()
                }

                Column(Modifier.fillMaxSize()) {
                    ScreenTitle("Tax", chrome)
                    TaxScreen(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onSelectYear = viewModel::selectYear,
                        onExport = viewModel::export
                    )
                }
            }

            composable(
                route = "settle/{${SettleTimeViewModel.ARG_PLATFORM_ID}}",
                arguments = listOf(navArgument(SettleTimeViewModel.ARG_PLATFORM_ID) {
                    type = NavType.StringType
                })
            ) {
                val viewModel: SettleTimeViewModel = viewModel(factory = SettleTimeViewModel.Factory)
                val state by viewModel.state.collectAsState()
                SettleTimeScreen(state = state, onBack = { navController.popBackStack() })
            }

            composable(
                route = "advisor/{${WithdrawAdvisorViewModel.ARG_PROVIDER}}",
                arguments = listOf(navArgument(WithdrawAdvisorViewModel.ARG_PROVIDER) {
                    type = NavType.StringType
                })
            ) {
                val viewModel: WithdrawAdvisorViewModel =
                    viewModel(factory = WithdrawAdvisorViewModel.Factory)
                val state by viewModel.state.collectAsState()
                WithdrawAdvisorScreen(state = state, onBack = { navController.popBackStack() })
            }

            composable(ClearedDestination.Discover.route) {
                val viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory)
                val state by viewModel.state.collectAsState()

                Column(Modifier.fillMaxSize()) {
                    ScreenTitle("Discover", chrome)
                    DiscoverScreen(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onSelectFilter = viewModel::selectFilter,
                        onOpenListing = { navController.navigate("listing/$it") }
                    )
                }
            }

            composable(
                route = "listing/{${ListingDetailViewModel.ARG_LISTING_ID}}",
                arguments = listOf(navArgument(ListingDetailViewModel.ARG_LISTING_ID) {
                    type = NavType.StringType
                })
            ) {
                val viewModel: ListingDetailViewModel =
                    viewModel(factory = ListingDetailViewModel.Factory)
                val state by viewModel.state.collectAsState()
                val tracked by viewModel.tracked.collectAsState()

                // Tracking it makes it a record; the user's next question is about the record, not
                // the listing, so the screen steps out of the way.
                LaunchedEffect(tracked) { if (tracked) navController.popBackStack() }

                ListingDetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onTrackProspect = viewModel::trackAsProspect,
                    onOpenSettleTime = { navController.navigate("settle/$it") },
                    onEstimateHours = viewModel::estimateHours
                )
            }

            composable(SYNC_ROUTE) {
                val viewModel: SyncViewModel = viewModel(factory = SyncViewModel.Factory)
                val state by viewModel.state.collectAsState()
                val context = LocalContext.current

                SyncScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onResolve = viewModel::resolve,
                    onRetryNow = { SyncWorker.retryNow(context) }
                )
            }

            // Every destination is built now. The placeholder loop that used to stand in for the
            // unbuilt ones is gone: with Discover in `visible` it was registering `composable`
            // twice for the same route, and a duplicate route in a NavGraph silently swallows the
            // navigation rather than failing.
        }
    }
}

/**
 * Writes the CSV to the app's own cache and offers it to the system share sheet.
 *
 * A `FileProvider` grant rather than a world-readable file, and one the user resolves themselves â€”
 * this app never sends anything anywhere on its own.
 */
private fun shareCsv(context: Context, fileName: String, content: String) {
    val dir = File(context.cacheDir, "export").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText(content)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV"))
}

/**
 * The Add-record sheet, hosted from Pipeline. Modal rather than a destination: it is a ten-second
 * interruption to the screen behind it, not somewhere the user navigates to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordHost(onDismiss: () -> Unit) {
    val viewModel: AddRecordViewModel = viewModel(factory = AddRecordViewModel.Factory)
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AddRecordSheet(
        state = state,
        sheetState = sheetState,
        onDismiss = onDismiss,
        onPlatformSelected = viewModel::selectPlatform,
        onCurrencySelected = viewModel::selectCurrency,
        onAmountChanged = viewModel::changeAmount,
        onHoursChanged = viewModel::changeHours,
        onUnpaidToggled = viewModel::toggleUnpaid,
        onStageSelected = viewModel::selectStage,
        onSave = { viewModel.save { onDismiss() } }
    )
}

/** What every tab draws above its content: the title, and the offline strip when it applies. */
private data class ScreenChrome(
    val offline: Boolean,
    val queuedWrites: Int,
    val onOpenSync: () -> Unit
)

/**
 * 48 dp title, then the offline strip if there is one.
 *
 * M3's own TopAppBar is 64 dp; the extra 16 dp of slack under the title pushed the hero visibly
 * down the screen, so the height is pinned to the spec.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenTitle(title: String, chrome: ScreenChrome? = null) {
    TopAppBar(
        title = { Text(title, style = Cleared.type.screenTitle) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier.height(Dimens.topAppBar)
    )
    if (chrome != null && chrome.offline) {
        OfflineStrip(
            lastSyncedLabel = null,
            queuedWrites = chrome.queuedWrites,
            onClick = chrome.onOpenSync
        )
    }
}




