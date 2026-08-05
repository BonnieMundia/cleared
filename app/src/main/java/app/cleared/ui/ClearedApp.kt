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
import app.cleared.ui.addrecord.AddRecordSheet
import app.cleared.ui.addrecord.AddRecordViewModel
import app.cleared.ui.nav.ClearedBottomBar
import app.cleared.ui.nav.ClearedDestination
import app.cleared.ui.nav.PlaceholderScreen
import app.cleared.ui.pipeline.PipelineScreen
import app.cleared.ui.pipeline.PipelineViewModel
import app.cleared.ui.platforms.PlatformsScreen
import app.cleared.ui.platforms.PlatformsViewModel
import app.cleared.ui.money.MoneyScreen
import app.cleared.ui.money.MoneyViewModel
import app.cleared.ui.record.RecordDetailScreen
import app.cleared.ui.record.RecordDetailViewModel
import app.cleared.ui.tax.TaxScreen
import app.cleared.ui.tax.TaxViewModel
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.Dimens
import java.io.File

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

                Column(Modifier.fillMaxSize()) {
                    ScreenTitle("Pipeline")
                    PipelineScreen(
                        state = state,
                        pendingUndo = undo,
                        modifier = Modifier.weight(1f),
                        onAdvance = { viewModel.advance(it.id, it.platformName) },
                        onOpenRecord = { navController.navigate("record/${it.id}") },
                        onLongPressRecord = { navController.navigate("record/${it.id}") },
                        onUndo = viewModel::undo,
                        onUndoHandled = viewModel::undoHandled,
                        onAddRecord = { sheetOpen = true }
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
                    ScreenTitle("Platforms")
                    PlatformsScreen(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onSelectSort = viewModel::selectSort
                    )
                }
            }

            composable(ClearedDestination.Money.route) {
                val viewModel: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory)
                val state by viewModel.state.collectAsState()

                Column(Modifier.fillMaxSize()) {
                    ScreenTitle("Money")
                    MoneyScreen(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onAmountChanged = viewModel::changeAmount,
                        onCurrencyChanged = viewModel::changeCurrency
                    )
                }
            }

            composable(ClearedDestination.Tax.route) {
                val viewModel: TaxViewModel = viewModel(factory = TaxViewModel.Factory)
                val state by viewModel.state.collectAsState()
                val export by viewModel.pendingExport.collectAsState()
                val context = LocalContext.current

                // The export hands the file to the system share sheet. The user picks where it
                // goes, or picks nothing — the app never sends it anywhere itself.
                LaunchedEffect(export) {
                    val payload = export ?: return@LaunchedEffect
                    runCatching { shareCsv(context, payload.fileName, payload.content) }
                    viewModel.exportHandled()
                }

                Column(Modifier.fillMaxSize()) {
                    ScreenTitle("Tax")
                    TaxScreen(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onSelectYear = viewModel::selectYear,
                        onExport = viewModel::export
                    )
                }
            }

            ClearedDestination.visible
                .filter {
                    it != ClearedDestination.Pipeline && it != ClearedDestination.Platforms &&
                        it != ClearedDestination.Money && it != ClearedDestination.Tax
                }
                .forEach { destination ->
                    composable(destination.route) { PlaceholderScreen(destination) }
                }
        }
    }
}

/**
 * Writes the CSV to the app's own cache and offers it to the system share sheet.
 *
 * A `FileProvider` grant rather than a world-readable file, and one the user resolves themselves —
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

/**
 * 48 dp, title only, flat. M3's own TopAppBar is 64 dp; the extra 16 dp of slack under the title
 * pushed the hero visibly down the screen, so the height is pinned to the spec.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenTitle(title: String) {
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
}
