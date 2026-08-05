package app.cleared.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cleared.ui.theme.Cleared

/**
 * The bottom destinations, data-driven.
 *
 * The base design has four; Discovery adds a fifth and the labels step down half a point. If the
 * product decides to keep four, the one to fold is Tax — it is consulted monthly, not daily, and
 * sits naturally as a tab inside Money. Removing it from this list is the whole change.
 */
enum class ClearedDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Pipeline("pipeline", "Pipeline", Icons.AutoMirrored.Filled.ListAlt),
    Platforms("platforms", "Platforms", Icons.Filled.GridView),
    Discover("discover", "Discover", Icons.Filled.Radar),
    Money("money", "Money", Icons.Filled.AccountBalanceWallet),
    Tax("tax", "Tax", Icons.AutoMirrored.Filled.ReceiptLong);

    companion object {
        /**
         * Five destinations, in the order design/README.md gives them. M3 permits 3–5; if the
         * product decides to fold one, it is Tax — consulted monthly rather than daily, and it sits
         * naturally as a tab inside Money. Removing it from this list is the whole change.
         */
        val visible: List<ClearedDestination> =
            listOf(Pipeline, Platforms, Discover, Money, Tax)
    }
}

@Composable
fun ClearedBottomBar(
    current: ClearedDestination,
    onSelect: (ClearedDestination) -> Unit,
    destinations: List<ClearedDestination> = ClearedDestination.visible
) {
    val fiveUp = destinations.size >= 5
    NavigationBar(
        containerColor = Cleared.tones.navBar,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        destinations.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = {
                    Text(
                        text = destination.label,
                        style = when {
                            fiveUp && selected -> Cleared.type.navLabelActiveFive
                            fiveUp -> Cleared.type.navLabelInactiveFive
                            selected -> Cleared.type.navLabelActive
                            else -> Cleared.type.navLabelInactive
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = Cleared.tones.onSurfaceVariant2,
                    unselectedTextColor = Cleared.tones.onSurfaceVariant2
                )
            )
        }
    }
}

/**
 * Stands in for a destination that has not been built yet. Steps 6, 7 and 10 replace these; it
 * exists so the nav bar in frame `1a` is real rather than decorative, and so the FAB's 104 dp
 * bottom offset clears something that is actually there.
 */
@Composable
fun PlaceholderScreen(destination: ClearedDestination, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = destination.label,
            style = Cleared.type.screenTitle,
            color = Cleared.tones.ghost,
            modifier = Modifier.padding(20.dp)
        )
    }
}
