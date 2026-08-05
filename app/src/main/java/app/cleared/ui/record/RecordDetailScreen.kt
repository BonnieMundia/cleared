package app.cleared.ui.record

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cleared.ui.components.PhaseRail
import app.cleared.ui.components.StageChip
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `1e` — every stage transition with its timestamp, the rate applied, each fee, and the final
 * KES cleared.
 *
 * The same screen renders `4a` (a payout that bounced) and `4b` (a record paid in parts). They are
 * not separate screens: what differs is which figures exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    state: RecordDetailUi,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenSettlement: (Long) -> Unit = {},
    onOpenReissue: (Long) -> Unit = {},
    onLogReissue: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = { PushedScreenBar(title = "Record", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenGutter)
        ) {
            Header(state)

            if (state.isPartPaid) {
                Settlements(state, onOpenSettlement)
            }

            SectionLabel("History")
            PhaseTimeline(
                phases = state.phases,
                reversed = state.isReversed,
                reversalReason = state.reversalReason,
                splitNote = if (state.isPartPaid) {
                    "Hours are never split. The effective rate is a property of the record, " +
                        "not of a settlement."
                } else null
            )

            SectionLabel("What happened to the money")
            MoneyLedger(state.ledger)

            state.closingNote?.let { ClosingNote(it, state.isReversed) }

            if (state.isReversed) {
                Reissue(state, onOpenReissue, onLogReissue)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * The pushed-screen app bar: 56 dp, back chevron, title.
 *
 * Built directly rather than with M3's `TopAppBar`, whose internal layout assumes 64 dp — squeezed
 * to the 56 dp the spec calls for, its title and navigation icon stop sharing a baseline.
 */
@Composable
private fun PushedScreenBar(title: String, onBack: () -> Unit) {
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
            text = title,
            style = Cleared.type.pushedTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Header(state: RecordDetailUi) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.platformName,
                style = Cleared.type.cardTitle.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
            StageChip(stage = state.stage, label = state.chipLabel)
        }

        state.subLine?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = Cleared.type.caption, color = Cleared.tones.tertiary)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = state.heroFigure,
            style = Cleared.type.sectionFigureLarge,
            color = when (state.heroTone) {
                HeroTone.Cleared -> Cleared.semantics.onMoneyContainer
                // A reversal's money exists — it is in the wrong place, which is not the same
                // thing as never having existed. Frame `4a` is explicit about this.
                HeroTone.Neutral -> MaterialTheme.colorScheme.onSurface
                HeroTone.None -> MaterialTheme.colorScheme.onSurface
            }
        )

        if (state.isPartPaid) {
            Spacer(Modifier.height(10.dp))
            SplitHeroBar(state.clearedFraction)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                state.clearedText?.let {
                    Text(it, style = Cleared.type.caption, color = Cleared.semantics.onMoneyContainer)
                }
                Spacer(Modifier.weight(1f))
                state.inFlightText?.let {
                    Text(it, style = Cleared.type.caption, color = Cleared.tones.onSurfaceVariant2)
                }
            }
        }

        state.heroCaption?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = Cleared.type.caption, color = Cleared.tones.tertiary2)
        }

        Spacer(Modifier.height(16.dp))
        StatStrip(state.stats)
    }
}

/** Cleared portion in `money`, remainder in `moneyContainer`, with a 2 dp gap between them. */
@Composable
private fun SplitHeroBar(clearedFraction: Float) {
    val fraction = clearedFraction.coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth().height(Dimens.splitBar), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (fraction > 0f) {
            Box(
                Modifier.weight(fraction).fillMaxSize()
                    .clip(ClearedShape.progressBar)
                    .background(Cleared.semantics.money)
            )
        }
        if (fraction < 1f) {
            Box(
                Modifier.weight(1f - fraction).fillMaxSize()
                    .clip(ClearedShape.progressBar)
                    .background(Cleared.semantics.moneyContainer)
            )
        }
    }
}

@Composable
private fun StatStrip(stats: List<StatCell>) {
    Row(Modifier.fillMaxWidth()) {
        stats.forEach { cell ->
            Column(Modifier.weight(1f)) {
                Text(cell.label, style = Cleared.type.caption, color = Cleared.tones.tertiary)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = cell.value,
                    style = Cleared.type.rowFigure,
                    color = if (cell.isNegative) Cleared.semantics.onRejectContainer
                    else MaterialTheme.colorScheme.onSurface
                )
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
        modifier = Modifier.padding(top = Dimens.sectionSpacing, bottom = 4.dp)
    )
}

/** The label/value table. Figures right-aligned in their column, mono, never truncated. */
@Composable
private fun MoneyLedger(rows: List<LedgerRow>) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .clip(ClearedShape.card)
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                // Table-row rules inside a card are `outlineVariant`, a step lighter than the
                // card's own border.
                HorizontalDivider(
                    thickness = Dimens.hairline,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.label,
                        style = if (row.isTotal) {
                            Cleared.type.tableRow.copy(fontWeight = FontWeight.SemiBold)
                        } else Cleared.type.tableRow,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    row.subLabel?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = Cleared.type.microAnnotation, color = Cleared.tones.tertiary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = row.value,
                    style = if (row.isTotal) {
                        Cleared.type.rowFigure.copy(fontWeight = FontWeight.SemiBold)
                    } else Cleared.type.tableFigure,
                    color = if (row.isTotal && row.totalCleared) Cleared.semantics.onMoneyContainer
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun ClosingNote(note: String, reversed: Boolean) {
    Text(
        text = note,
        style = Cleared.type.caption,
        color = if (reversed) Cleared.semantics.onRejectContainer else Cleared.tones.onSurfaceVariant2,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .background(
                if (reversed) Cleared.semantics.rejectContainer else Cleared.tones.surfaceHigh,
                ClearedShape.smallTile
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/** Frame `4b` — one card per settlement, each opening a settlement-scoped copy of the ledger. */
@Composable
private fun Settlements(state: RecordDetailUi, onOpen: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = Dimens.sectionSpacing, bottom = 6.dp)) {
        Text("SETTLEMENTS", style = Cleared.type.sectionOverline, color = Cleared.tones.label)
        Spacer(Modifier.weight(1f))
        state.settlementTerms?.let {
            Text(it, style = Cleared.type.caption, color = Cleared.tones.tertiary)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardGap)) {
        state.settlements.forEach { settlement ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
                    .clip(ClearedShape.card)
                    .clickable { onOpen(settlement.id) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhaseRail(stage = settlement.stage, height = 34.dp)
                Spacer(Modifier.width(Dimens.rowInternalGap))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = settlement.label,
                            style = Cleared.type.rowPrimary,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        StageChip(stage = settlement.stage)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(settlement.timing, style = Cleared.type.captionFigure, color = Cleared.tones.tertiary)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(settlement.amount, style = Cleared.type.rowFigure, color = Cleared.semantics.figure)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = settlement.kes,
                        style = Cleared.type.rowSubFigure,
                        color = if (settlement.isLanded) Cleared.semantics.onMoneyContainer
                        else Cleared.tones.tertiary2
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Cleared.tones.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    state.settlementFooter?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = Cleared.type.caption, color = Cleared.tones.tertiary2)
    }
}

/**
 * Frame `4a` — the successor record, linked by `supersedesRecordId`. Recovery is a new record and
 * never a return to an earlier stage.
 */
@Composable
private fun Reissue(state: RecordDetailUi, onOpen: (Long) -> Unit, onLog: () -> Unit) {
    SectionLabel("Re-issued as")

    val reissue = state.reissue
    if (reissue == null) {
        TextButton(onClick = onLog) { Text("Log the re-issue", style = Cleared.type.rowPrimary) }
        return
    }

    Row(
        Modifier
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .clip(ClearedShape.card)
            .clickable { onOpen(reissue.recordId) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhaseRail(stage = reissue.stage, height = 34.dp)
        Spacer(Modifier.width(Dimens.rowInternalGap))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reissue.reference, style = Cleared.type.rowFigure, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                StageChip(stage = reissue.stage)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(reissue.amount, style = Cleared.type.rowFigure, color = Cleared.semantics.figure)
            Spacer(Modifier.height(3.dp))
            Text(reissue.hours, style = Cleared.type.rowSubFigure, color = Cleared.tones.tertiary2)
        }
    }

    Spacer(Modifier.height(8.dp))
    Text(
        text = "Carries no hours of its own — the work was already counted here.",
        style = Cleared.type.caption,
        color = Cleared.tones.tertiary2
    )
}
