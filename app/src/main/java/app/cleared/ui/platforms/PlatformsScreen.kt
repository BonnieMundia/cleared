package app.cleared.ui.platforms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `1b` — make a bad platform obvious at a glance.
 *
 * One card per platform, effective KES per hour as the headline, and an hours bar whose track is
 * the work that was never paid for. A platform that wastes your time looks bad here, which is the
 * entire product thesis rendered as a screen.
 */
@Composable
fun PlatformsScreen(
    state: PlatformsUiState,
    modifier: Modifier = Modifier,
    onSelectSort: (PlatformSort) -> Unit = {},
    onOpenSettleTime: (Long) -> Unit = {}
) {
    LazyColumn(modifier.fillMaxSize()) {
        item { SortRow(state.sort, onSelectSort) }

        items(state.cards, key = { it.id }) { card ->
            PlatformCard(card, onClick = { onOpenSettleTime(card.id) })
            Spacer(Modifier.height(Dimens.cardGap))
        }

        item { Footer() }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SortRow(selected: PlatformSort, onSelect: (PlatformSort) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenGutter)
            .padding(top = 4.dp, bottom = 14.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Sort", style = Cleared.type.caption, color = Cleared.tones.tertiary)
        PlatformSort.entries.forEach { sort ->
            val isSelected = sort == selected
            Box(
                Modifier
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        ClearedShape.filterChip
                    )
                    .then(
                        if (isSelected) Modifier
                        else Modifier.border(Dimens.hairline, Cleared.tones.outlineField, ClearedShape.filterChip)
                    )
                    .clickable { onSelect(sort) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = sort.label,
                    style = Cleared.type.caption,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else Cleared.tones.onSurfaceVariant2,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun PlatformCard(card: PlatformCardUi, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = Dimens.screenGutter)
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .clip(ClearedShape.card)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(Dimens.cardPadding)) {
            Header(card)
            Spacer(Modifier.height(Dimens.rowInternalGap))
            Headline(card)
            Spacer(Modifier.height(Dimens.rowInternalGap))
            HoursBar(card)
        }

        HorizontalDivider(thickness = Dimens.hairline, color = MaterialTheme.colorScheme.outlineVariant)
        StatsRow(card)

        card.warning?.let { Warning(it) }
    }
}

@Composable
private fun Header(card: PlatformCardUi) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(card.name, style = Cleared.type.cardTitle, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(card.subLine, style = Cleared.type.caption, color = Cleared.tones.tertiary2)
        }
        // Rank takes `tertiary`, not `ghost`: at 11 sp it is normal text and has to reach AA.
        Text(card.rank, style = Cleared.type.rowSubFigure, color = Cleared.tones.tertiary)
    }
}

/**
 * `KES 2,275` · `per hour`, with `+1,109 vs median` right-aligned beneath.
 *
 * design/SCREENS.md puts the comparison on the same baseline as the figure, and at the 390 dp the
 * design was drawn to it fits. Most phones are 360 dp — this one included — and there the three
 * cannot share a line: the 29 sp figure alone takes 157 dp of a 290 dp card, and the comparison
 * needs 106. Squeezing it clipped to "+1,109 vs medi"; shrinking the figure to fit would mean
 * cutting the most important number on the card to make room for its footnote.
 *
 * So the footnote moves down a line and keeps its words. A rate with nothing to compare it against
 * does not tell you a platform is bad, which is the one thing this screen exists to do.
 */
@Composable
private fun Headline(card: PlatformCardUi) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = card.effectiveRate,
                style = Cleared.type.sectionFigure,
                // A poor platform states its rate in red. Nothing else on the card is coloured by it.
                color = if (card.isPoor) Cleared.semantics.onRejectContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "per hour",
                style = Cleared.type.secondary,
                color = Cleared.tones.tertiary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = card.vsMedian,
            style = Cleared.type.rowSubFigure,
            color = if (card.isPoor) Cleared.semantics.onRejectContainer else Cleared.tones.tertiary,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The track is the unpaid hours; the paid portion overlays it. Reading the bar is reading how much
 * of the time given to this platform it ever paid for.
 */
@Composable
private fun HoursBar(card: PlatformCardUi) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Dimens.progressBar)
                .clip(ClearedShape.progressBar)
                .background(Cleared.semantics.overdueBar)
        ) {
            if (card.paidFraction > 0f) {
                Box(
                    Modifier
                        .weight(card.paidFraction)
                        .fillMaxHeight()
                        .background(
                            if (card.isPoor) Cleared.semantics.reject else Cleared.semantics.money
                        )
                )
            }
            if (card.paidFraction < 1f) {
                Box(Modifier.weight(1f - card.paidFraction).fillMaxHeight())
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(card.hoursLogged, style = Cleared.type.caption, color = Cleared.tones.tertiary2)
            Spacer(Modifier.weight(1f))
            Text(
                text = card.hoursUnpaid,
                style = Cleared.type.caption,
                color = when {
                    card.isPoor -> Cleared.semantics.onRejectContainer
                    card.hasUnpaidHours -> Cleared.semantics.overdue
                    else -> Cleared.tones.tertiary2
                }
            )
        }
    }
}

@Composable
private fun StatsRow(card: PlatformCardUi) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.cardPadding, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(card.approval, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
        Spacer(Modifier.width(12.dp))
        Text(card.daysToLand, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
        if (card.reversedCount > 0) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${card.reversedCount} reversed",
                style = Cleared.type.caption,
                color = Cleared.semantics.onRejectContainer
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = card.totalPaid,
            style = Cleared.type.captionFigure,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun Warning(text: String) {
    Row(
        Modifier
            .padding(horizontal = Dimens.cardPadding)
            .padding(bottom = Dimens.cardPadding)
            .fillMaxWidth()
            .background(Cleared.semantics.rejectContainer, ClearedShape.smallTile)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(Cleared.semantics.reject, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, style = Cleared.type.caption, color = Cleared.semantics.onRejectContainer)
    }
}

@Composable
private fun Footer() {
    Text(
        text = "Effective rate divides everything a platform has ever paid you by every hour you " +
            "have given it, including assessments and onboarding that were never billable.",
        style = Cleared.type.caption,
        color = Cleared.tones.tertiary2,
        modifier = Modifier.padding(horizontal = Dimens.screenGutter).padding(top = 10.dp)
    )
}
