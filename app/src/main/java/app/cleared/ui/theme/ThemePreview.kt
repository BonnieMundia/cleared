package app.cleared.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A reference sheet for the theme itself: the semantic families, the accent, and the figure scale
 * with a column-alignment check.
 *
 * Not a screen — the record row in frame `1h` is step 3. This exists so the tokens can be read
 * against design/Cleared.dc.html before anything is built on top of them.
 */
@Composable
private fun ThemeSheet() {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp, shadowElevation = 0.dp) {
        Column(
            modifier = Modifier.padding(Dimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap)
        ) {
            Text("SEMANTIC FAMILIES", style = Cleared.type.sectionOverline, color = Cleared.tones.label)

            Family("Work", Cleared.semantics.work, Cleared.semantics.workContainer, Cleared.semantics.onWorkContainer)
            Family("Money", Cleared.semantics.money, Cleared.semantics.moneyContainer, Cleared.semantics.onMoneyContainer)
            Family("Overdue", Cleared.semantics.overdueDot, Cleared.semantics.overdueContainer, Cleared.semantics.overdue)
            Family("Rejected", Cleared.semantics.reject, Cleared.semantics.rejectContainer, Cleared.semantics.onRejectContainer)

            Text("FIGURE SCALE", style = Cleared.type.sectionOverline, color = Cleared.tones.label)

            // The hero: the one accent figure on a screen.
            Row(verticalAlignment = Alignment.Bottom) {
                Text("KES", style = Cleared.type.heroPrefix, color = Cleared.semantics.heroFigure)
                Box(Modifier.width(8.dp))
                Text("247,119", style = Cleared.type.heroFigure, color = Cleared.semantics.heroFigure)
            }

            // Every figure right-aligned in its column, and the columns line up.
            Column {
                listOf("EUR 640.00" to "KES 94,464", "USD 42.50" to "KES 5,457", "USD 184.00" to "KES 23,626")
                    .forEach { (gross, kes) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                gross,
                                style = Cleared.type.rowFigure,
                                color = Cleared.semantics.figure,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                            Box(Modifier.width(16.dp))
                            Text(
                                kes,
                                style = Cleared.type.rowSubFigure,
                                color = Cleared.tones.tertiary2,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
            }

            Text(
                "Unlanded amounts are valued at today's mid rate.",
                style = Cleared.type.caption,
                color = Cleared.tones.tertiary2
            )
        }
    }
}

@Composable
private fun Family(name: String, rail: Color, container: Color, onContainer: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(Dimens.railWidth, Dimens.railHeight).background(rail, ClearedShape.phaseRail))
        Box(Modifier.width(Dimens.rowInternalGap))
        Text(name, style = Cleared.type.rowPrimary, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .background(container, ClearedShape.stageChip)
                .border(Dimens.hairline, onContainer.copy(alpha = 0.2f), ClearedShape.stageChip)
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(name.uppercase(), style = Cleared.type.stageChip, color = onContainer)
        }
    }
}

@Preview(name = "Theme · light", showBackground = true, widthDp = 390)
@Composable
private fun ThemeSheetLight() = ClearedTheme(darkTheme = false) { ThemeSheet() }

@Preview(name = "Theme · dark", showBackground = true, widthDp = 390)
@Composable
private fun ThemeSheetDark() = ClearedTheme(darkTheme = true) { ThemeSheet() }
