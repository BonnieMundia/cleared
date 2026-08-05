package app.cleared.ui.tax

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `1d` — deliberately plain.
 *
 * No cards, no accent except the export button. Sections are separated by 8 dp bands rather than by
 * containers. This screen is consulted monthly and its job is to be legible, not interesting.
 */
@Composable
fun TaxScreen(
    state: TaxUiState,
    modifier: Modifier = Modifier,
    onSelectYear: (Int?) -> Unit = {},
    onExport: () -> Unit = {}
) {
    LazyColumn(modifier.fillMaxSize()) {
        item { YearTabs(state, onSelectYear) }

        state.personal?.let { block -> item { Block(block) } }
        item { Band() }
        state.company?.let { block -> item { Block(block) } }
        item { Band() }
        item { SetAside(state) }

        item { ExportButton(state, onExport) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun YearTabs(state: TaxUiState, onSelect: (Int?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenGutter)
            .padding(top = 4.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        val tabs: List<Pair<String, Int?>> = state.years.map { it.toString() to it } + ("All" to null)
        tabs.forEach { (label, year) ->
            val selected = year == state.selectedYear
            Column(
                // Width from the label, not from the row: `fillMaxWidth` on the underline made the
                // first tab claim the whole row and pushed every other tab off the screen.
                Modifier.width(IntrinsicSize.Max).clickable { onSelect(year) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = Cleared.type.rowPrimary.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else Cleared.tones.onSurfaceVariant2
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .height(2.dp)
                        .fillMaxWidth()
                        .background(
                            if (selected) MaterialTheme.colorScheme.onSurface
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                )
            }
        }
    }
}

@Composable
private fun Block(block: TaxBlockUi) {
    Column(Modifier.padding(horizontal = Dimens.screenGutter).fillMaxWidth()) {
        Text(
            text = block.overline.uppercase(),
            style = Cleared.type.sectionOverline,
            color = Cleared.tones.label
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = block.figure,
            style = Cleared.type.sectionFigure.copy(fontSize = 28.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.height(6.dp))
        Text(block.context, style = Cleared.type.secondary, color = Cleared.tones.onSurfaceVariant2)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = block.rowLabel,
                style = Cleared.type.secondary,
                color = Cleared.tones.onSurfaceVariant2,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = block.rowValue,
                style = Cleared.type.captionFigure,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** An 8 dp band in surfaceContainerHigh with a rule top and bottom — the only section separator. */
@Composable
private fun Band() {
    Column(Modifier.padding(vertical = Dimens.sectionSpacing)) {
        HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
        Box(Modifier.fillMaxWidth().height(Dimens.spacerBand).background(Cleared.tones.surfaceHigh))
        HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
    }
}

@Composable
private fun SetAside(state: TaxUiState) {
    Column(Modifier.padding(horizontal = Dimens.screenGutter).fillMaxWidth()) {
        Text("RUNNING SET-ASIDE", style = Cleared.type.sectionOverline, color = Cleared.tones.label)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = state.setAside,
                style = Cleared.type.sectionFigure.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = state.setAsideOf,
                style = Cleared.type.captionFigure,
                color = Cleared.tones.onSurfaceVariant2,
                modifier = Modifier.padding(bottom = 4.dp, start = 10.dp),
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(Dimens.splitBar)
                .clip(ClearedShape.progressBar)
                .background(Cleared.tones.divider)
        ) {
            if (state.setAsideFraction > 0f) {
                Box(
                    Modifier
                        .weight(state.setAsideFraction)
                        .fillMaxHeight()
                        .background(Cleared.tones.onSurfaceStrong)
                )
            }
            if (state.setAsideFraction < 1f) {
                Box(Modifier.weight(1f - state.setAsideFraction).fillMaxHeight())
            }
        }

        state.shortfall?.let {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Short by",
                    style = Cleared.type.secondary,
                    color = Cleared.tones.onSurfaceVariant2,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = it,
                    style = Cleared.type.captionFigure,
                    color = Cleared.semantics.overdue,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        state.heldIn?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = Cleared.type.caption, color = Cleared.tones.tertiary2)
        }
    }
}

@Composable
private fun ExportButton(state: TaxUiState, onExport: () -> Unit) {
    Column(Modifier.padding(horizontal = Dimens.screenGutter).padding(top = Dimens.sectionSpacing)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(Dimens.outlinedButton)
                .border(Dimens.hairline, MaterialTheme.colorScheme.primary, ClearedShape.pill)
                .clip(ClearedShape.pill)
                .clickable(onClick = onExport),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.exportLabel,
                style = Cleared.type.rowPrimary,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.exportCaption,
            style = Cleared.type.caption,
            color = Cleared.tones.tertiary2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
