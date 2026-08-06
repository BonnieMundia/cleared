package app.cleared.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
 * Frame `3a` — open work, priced in the only unit that matters.
 *
 * Every listing is shown at what it would actually pay per hour after commission, after the
 * withdrawal, and after the platform's own rejection rate. A browser tab shows you the headline
 * number; this shows you the one you would keep.
 */
@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    modifier: Modifier = Modifier,
    onSelectFilter: (DiscoverFilter) -> Unit = {},
    onOpenListing: (Long) -> Unit = {}
) {
    LazyColumn(modifier.fillMaxSize()) {
        item { Header(state) }
        item { Filters(state.filter, onSelectFilter) }

        items(state.listings, key = { it.id }) { listing ->
            ListingCard(listing, onClick = { onOpenListing(listing.id) })
            Spacer(Modifier.height(Dimens.cardGap))
        }

        item { Footer() }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header(state: DiscoverUiState) {
    Column(Modifier.padding(horizontal = Dimens.screenGutter).padding(top = 6.dp, bottom = 14.dp)) {
        Text(
            text = "Best available right now",
            style = Cleared.type.heroOverline,
            color = Cleared.tones.onSurfaceVariant3
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = state.bestRate,
                style = Cleared.type.sectionFigureLarge,
                // The one accent figure on this screen.
                color = Cleared.semantics.heroFigure,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "per hour",
                style = Cleared.type.secondary,
                color = Cleared.tones.tertiary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        if (state.bestCaption.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(state.bestCaption, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
        }
        Spacer(Modifier.height(4.dp))
        Text(state.scanCaption, style = Cleared.type.caption, color = Cleared.tones.tertiary2)
    }
}

@Composable
private fun Filters(selected: DiscoverFilter, onSelect: (DiscoverFilter) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenGutter)
            .padding(bottom = 14.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DiscoverFilter.entries.forEach { filter ->
            val isSelected = filter == selected
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
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filter.label,
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
private fun ListingCard(listing: ListingRowUi, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = Dimens.screenGutter)
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .clip(ClearedShape.card)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(Dimens.cardPadding)) {
            Text(listing.title, style = Cleared.type.listingTitle, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(listing.subLine, style = Cleared.type.caption, color = Cleared.tones.tertiary2)

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = listing.rate,
                    // An unpriced listing is not a bad one, so it takes the neutral supporting tone
                    // rather than either the figure colour or the rejected red.
                    style = if (listing.isPriced) {
                        Cleared.type.sectionFigure.copy(fontSize = 27.sp)
                    } else {
                        Cleared.type.rowPrimary
                    },
                    color = when {
                        !listing.isPriced -> Cleared.tones.onSurfaceVariant2
                        listing.isBelowMedian -> Cleared.semantics.onRejectContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    softWrap = false
                )
                if (listing.isPriced) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "per hour",
                        style = Cleared.type.secondary,
                        color = Cleared.tones.tertiary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = listing.vsMedian,
                style = Cleared.type.rowSubFigure,
                color = when {
                    !listing.isPriced -> Cleared.tones.tertiary
                    listing.isBelowMedian -> Cleared.semantics.onRejectContainer
                    else -> Cleared.semantics.onMoneyContainer
                },
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(thickness = Dimens.hairline, color = MaterialTheme.colorScheme.outlineVariant)

        Column(Modifier.padding(Dimens.cardPadding)) {
            TableRow("Pays", listing.pays)
            TableRow(
                label = "Hours",
                value = listing.hours,
                // Unpaid assessment hours are amber wherever they appear: they are the thing this
                // whole app is about noticing.
                valueColor = if (listing.hasAssessment) Cleared.semantics.overdue else null
            )
            TableRow("Then", listing.then)
            TableRow("Adjusted", listing.adjusted)
        }

        listing.warning?.let { Warning(it) }
    }
}

@Composable
private fun TableRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = Cleared.type.caption,
            color = Cleared.tones.tertiary,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = Cleared.type.caption,
            color = valueColor ?: Cleared.tones.onSurfaceVariant2,
            modifier = Modifier.weight(1f)
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
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .background(Cleared.semantics.reject, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = Cleared.type.caption, color = Cleared.semantics.onRejectContainer)
    }
}

@Composable
private fun Footer() {
    Text(
        text = "Cleared reads public boards and community feeds. It never signs in as you and " +
            "never applies on your behalf — tapping through opens the platform.",
        style = Cleared.type.caption,
        color = Cleared.tones.tertiary2,
        modifier = Modifier.padding(horizontal = Dimens.screenGutter).padding(top = 10.dp)
    )
}
