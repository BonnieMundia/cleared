package app.cleared.ui.settletime

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cleared.data.model.Phase
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `2b` — the settle-time distribution for one platform.
 *
 * The histogram's shape is the argument: a long right tail is why the threshold is p90 and not an
 * average, and the stacked bar underneath says which phase the waiting actually happens in.
 */
@Composable
fun SettleTimeScreen(
    state: SettleTimeUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
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
                Text(
                    text = state.platformName,
                    style = Cleared.type.pushedTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.medianDays,
                    style = Cleared.type.sectionFigureLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = state.sampleCaption,
                    style = Cleared.type.caption,
                    color = Cleared.tones.tertiary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Histogram(state)

            Spacer(Modifier.height(20.dp))
            StatStrip(state)

            if (state.meanNote.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Note(state.meanNote)
            }

            if (state.dwell.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.sectionSpacing))
                SectionLabel("Time spent in each stage")
                DwellBar(state)
                Spacer(Modifier.height(12.dp))
                Text(state.dwellNote, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
            }

            if (state.overdueCount > 0) {
                Spacer(Modifier.height(Dimens.sectionSpacing))
                ChaseCard(state.overdueCount)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Twelve bars, 96 dp tall. Below p90 they step through the violet family by height; past it they
 * turn amber, because that is the tail the threshold exists to catch.
 */
@Composable
private fun Histogram(state: SettleTimeUiState) {
    val semantics = Cleared.semantics
    Column {
        Row(
            Modifier.fillMaxWidth().height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            state.buckets.forEach { bucket ->
                val ratio = bucket.count.toFloat() / state.maxBucketCount
                val color = when {
                    bucket.isTail -> semantics.overdue
                    ratio > 0.66f -> semantics.work
                    ratio > 0.33f -> Cleared.tones.workMid
                    else -> semantics.workOutline
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(ratio.coerceAtLeast(0.02f))
                        .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth()) {
            state.axisLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = Cleared.type.microAnnotation,
                    color = Cleared.tones.tertiary,
                    modifier = Modifier.weight(1f),
                    textAlign = if (index == state.axisLabels.lastIndex) {
                        androidx.compose.ui.text.style.TextAlign.End
                    } else {
                        androidx.compose.ui.text.style.TextAlign.Start
                    }
                )
            }
        }
    }
}

@Composable
private fun StatStrip(state: SettleTimeUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = state.p50,
            style = Cleared.type.caption,
            color = Cleared.tones.onSurfaceVariant2,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = state.p90,
            style = Cleared.type.caption,
            // The threshold is stated in the colour it fires in.
            color = Cleared.semantics.overdue,
            modifier = Modifier.weight(1.4f),
            maxLines = 1
        )
        Text(
            text = state.drift,
            style = Cleared.type.caption,
            color = Cleared.tones.onSurfaceVariant2,
            modifier = Modifier.weight(1.1f),
            maxLines = 1
        )
    }
}

/** One 26 dp bar across every stage the record passes through, violet for work and green for money. */
@Composable
private fun DwellBar(state: SettleTimeUiState) {
    val semantics = Cleared.semantics
    Column {
        Row(
            Modifier.fillMaxWidth().height(26.dp).clip(ClearedShape.progressBar),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            state.dwell.forEach { segment ->
                if (segment.fraction <= 0f) return@forEach
                Box(
                    Modifier
                        .weight(segment.fraction)
                        .fillMaxHeight()
                        .background(if (segment.phase == Phase.WORK) semantics.work else semantics.money),
                    contentAlignment = Alignment.Center
                ) {
                    // Only the segments with room say their name; the rest are in the note below.
                    if (segment.fraction > 0.22f) {
                        Text(
                            text = segment.label,
                            style = Cleared.type.microAnnotation,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChaseCard(count: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .clip(ClearedShape.card)
            .padding(Dimens.cardPadding)
    ) {
        Text(
            text = if (count == 1) "One record here is past p90"
            else "$count records here are past p90",
            style = Cleared.type.rowPrimary,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(Dimens.outlinedButton)
                    .border(Dimens.hairline, Cleared.tones.outlineButton, ClearedShape.pill)
                    .clip(ClearedShape.pill)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text("Remind me in 3 d", style = Cleared.type.tableRow, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(Dimens.outlinedButton)
                    .background(MaterialTheme.colorScheme.primary, ClearedShape.pill)
                    .clip(ClearedShape.pill)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text("Draft follow-up", style = Cleared.type.tableRow, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = Cleared.type.sectionOverline,
        color = Cleared.tones.label,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = Cleared.type.caption,
        color = Cleared.tones.onSurfaceVariant2,
        modifier = Modifier
            .fillMaxWidth()
            .background(Cleared.tones.surfaceHigh, ClearedShape.smallTile)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}
