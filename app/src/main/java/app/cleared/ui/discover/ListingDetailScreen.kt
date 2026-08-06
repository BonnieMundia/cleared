package app.cleared.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cleared.ui.components.ProspectChip
import app.cleared.ui.format.MoneyFormat
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `3b` — the projection shown as arithmetic rather than as a number.
 *
 * A figure that decides whether someone spends a week on something should be checkable, so every
 * line of it is on the screen.
 */
@Composable
fun ListingDetailScreen(
    state: ListingDetailUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onTrackProspect: () -> Unit = {},
    onOpenPlatform: () -> Unit = {},
    onOpenSettleTime: (Long) -> Unit = {},
    onEstimateHours: (Double, Double) -> Unit = { _, _ -> }
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.topAppBarWithBack)
                    .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(Dimens.minTouchTarget)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text("Listing", style = Cleared.type.pushedTitle, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenGutter)
        ) {
            Text(state.title, style = Cleared.type.cardTitle, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(state.subLine, style = Cleared.type.caption, color = Cleared.tones.tertiary)

            Spacer(Modifier.height(16.dp))
            Text(
                text = state.rate,
                style = Cleared.type.sectionFigureLarge,
                color = if (state.isBelowMedian) Cleared.semantics.onRejectContainer
                else Cleared.semantics.heroFigure,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = state.comparison,
                style = Cleared.type.caption,
                color = if (state.isBelowMedian) Cleared.semantics.onRejectContainer
                else Cleared.semantics.onMoneyContainer
            )

            SectionLabel(if (state.isPriced) "How that number is built" else "What is known so far")
            Breakdown(state)

            SectionLabel(if (state.hoursEstimatedByUser) "Your estimate" else "How long will it take?")
            HoursEstimate(state, onEstimateHours)

            if (state.riskNote.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(state.riskNote, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
            }

            if (state.platformStats.isNotEmpty()) {
                SectionLabel("What you know about ${state.platformName}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.platformStats.forEach { (label, value) ->
                        Column(Modifier.weight(1f)) {
                            Text(label, style = Cleared.type.caption, color = Cleared.tones.tertiary, maxLines = 1)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = value,
                                style = Cleared.type.rowFigure,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.platformStatsNote,
                    style = Cleared.type.caption,
                    color = Cleared.tones.tertiary2,
                    modifier = state.platformId?.let {
                        Modifier.clickable { onOpenSettleTime(it) }
                    } ?: Modifier
                )
            }

            SectionLabel("Track it")
            ProspectCard(state.prospectNote)

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.filledButton)
                    .background(MaterialTheme.colorScheme.primary, ClearedShape.pill)
                    .clip(ClearedShape.pill)
                    .clickable(onClick = onTrackProspect),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Track as prospect",
                    style = Cleared.type.rowPrimary,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.outlinedButton)
                    .clip(ClearedShape.pill)
                    .clickable(onClick = onOpenPlatform),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open on ${state.platformName}",
                    style = Cleared.type.rowPrimary,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Breakdown(state: ListingDetailUiState) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .clip(ClearedShape.card)
    ) {
        state.breakdown.forEachIndexed { index, line ->
            if (index > 0) {
                HorizontalDivider(thickness = Dimens.hairline, color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = line.label,
                        style = if (line.isTotal) {
                            Cleared.type.tableRow.copy(fontWeight = FontWeight.SemiBold)
                        } else Cleared.type.tableRow,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    line.subLabel?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = Cleared.type.microAnnotation, color = Cleared.tones.tertiary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = line.value,
                    style = if (line.isTotal) {
                        Cleared.type.rowFigure.copy(fontWeight = FontWeight.SemiBold)
                    } else Cleared.type.tableFigure,
                    color = if (line.isTotal) Cleared.semantics.heroFigure
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * The one input on this screen, and the reason it exists.
 *
 * A board post says what it pays, never how long it takes. Until somebody says, the projection has
 * no denominator — so this is where the user supplies the number the source could not, split into
 * the work and the unpaid assessment because only the first is what they are being paid for.
 */
@Composable
private fun HoursEstimate(
    state: ListingDetailUiState,
    onEstimateHours: (Double, Double) -> Unit
) {
    var work by remember(state.listingId, state.estHours) { mutableStateOf(state.estHours) }
    var assessment by remember(state.listingId, state.assessmentHours) {
        mutableStateOf(state.assessmentHours)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .padding(Dimens.cardPadding)
    ) {
        HoursRow(
            label = "Work",
            hours = work,
            onChange = { work = it; onEstimateHours(it, assessment) }
        )
        Spacer(Modifier.height(10.dp))
        HoursRow(
            label = "Unpaid assessment",
            hours = assessment,
            amber = assessment > 0,
            onChange = { assessment = it; onEstimateHours(work, it) }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "No board publishes this. It is your judgement, and it is what every figure " +
                "above divides by.",
            style = Cleared.type.caption,
            color = Cleared.tones.tertiary2
        )
    }
}

@Composable
private fun HoursRow(
    label: String,
    hours: Double,
    amber: Boolean = false,
    onChange: (Double) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = Cleared.type.tableRow,
            color = if (amber) Cleared.semantics.overdue else Cleared.tones.onSurfaceVariant2,
            modifier = Modifier.weight(1f)
        )
        StepButton("−") { onChange((hours - 0.5).coerceAtLeast(0.0)) }
        Text(
            text = MoneyFormat.hours(hours),
            style = Cleared.type.rowFigure,
            color = if (amber) Cleared.semantics.overdue else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(72.dp)
        )
        StepButton("+") { onChange(hours + 0.5) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .border(Dimens.hairline, Cleared.tones.outlineButton, ClearedShape.smallTile)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = Cleared.type.cardTitle, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Dashed border, never filled — the same rule as the chip. It is not yet work. */
@Composable
private fun ProspectCard(note: String) {
    val outline = Cleared.tones.outlineDashed
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    brush = SolidColor(outline),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f
                        )
                    )
                )
            }
            .padding(Dimens.cardPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProspectChip()
            Spacer(Modifier.width(10.dp))
            Text(
                text = "a stage before Submitted",
                style = Cleared.type.caption,
                color = Cleared.tones.tertiary
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(note, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = Cleared.type.sectionOverline,
        color = Cleared.tones.label,
        modifier = Modifier.padding(top = Dimens.sectionSpacing, bottom = 10.dp)
    )
}
