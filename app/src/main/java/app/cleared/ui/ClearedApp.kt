package app.cleared.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import app.cleared.ui.record.RecordDetailScreen
import app.cleared.ui.record.RecordDetailViewModel
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.Dimens

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

            ClearedDestination.visible
                .filter { it != ClearedDestination.Pipeline }
                .forEach { destination ->
                    composable(destination.route) { PlaceholderScreen(destination) }
                }
        }
    }
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
