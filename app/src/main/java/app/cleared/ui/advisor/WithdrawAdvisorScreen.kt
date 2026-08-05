package app.cleared.ui.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `2c` — what this withdrawal costs, and the two cheaper things you could do instead.
 *
 * The headline percentage is in amber rather than red: it is a cost to be aware of, not a mistake.
 */
@Composable
fun WithdrawAdvisorScreen(
    state: WithdrawAdvisorUiState,
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
                Text(state.title, style = Cleared.type.pushedTitle, color = MaterialTheme.colorScheme.onSurface)
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
                    text = state.costPct,
                    style = Cleared.type.heroFigure,
                    color = Cleared.semantics.overdue,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = state.costKes,
                    style = Cleared.type.rowFigure,
                    color = Cleared.tones.onSurfaceVariant2,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            if (state.explanation.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(state.explanation, style = Cleared.type.body, color = Cleared.tones.onSurfaceVariant2)
            }

            Spacer(Modifier.height(Dimens.sectionSpacing))
            SectionLabel("Cost by withdrawal size")
            CostBars(state)

            Spacer(Modifier.height(14.dp))
            Text(state.splitNote, style = Cleared.type.caption, color = Cleared.tones.tertiary2)

            state.advice?.let {
                Spacer(Modifier.height(Dimens.sectionSpacing))
                SectionLabel("Bigger saving available")
                Advice(it)
            }

            Spacer(Modifier.height(Dimens.sectionSpacing))
            SettingsRows(state)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CostBars(state: WithdrawAdvisorUiState) {
    // 110 dp of bar plus a label above and below needs about 160; at 140 the tallest bar pushed
    // its own axis label off the bottom.
    Row(
        Modifier.fillMaxWidth().height(164.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        state.bars.forEach { bar ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = bar.pctLabel,
                    style = Cleared.type.microAnnotation.copy(
                        fontWeight = if (bar.isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (bar.isCurrent) MaterialTheme.colorScheme.primary
                    else Cleared.tones.tertiary,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((110 * bar.fraction).dp.coerceAtLeast(4.dp))
                        .background(
                            when {
                                bar.isCurrent -> MaterialTheme.colorScheme.primary
                                bar.isWorse -> Cleared.semantics.overdue
                                else -> Cleared.tones.accentTintHover
                            },
                            RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = bar.amountLabel,
                    style = Cleared.type.microAnnotation.copy(
                        fontWeight = if (bar.isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (bar.isCurrent) MaterialTheme.colorScheme.onSurface
                    else Cleared.tones.tertiary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun Advice(advice: DestinationAdviceUi) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Cleared.tones.moneyTint, ClearedShape.card)
            .border(Dimens.hairline, Cleared.semantics.moneyOutline, ClearedShape.card)
            .padding(Dimens.cardPadding)
    ) {
        Text(advice.title, style = Cleared.type.rowPrimary, color = MaterialTheme.colorScheme.onSurface)

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Now", style = Cleared.type.caption, color = Cleared.tones.tertiary)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = advice.fromPct,
                    style = Cleared.type.rowFigure,
                    color = Cleared.semantics.overdue
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Instead", style = Cleared.type.caption, color = Cleared.tones.tertiary)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = advice.toPct,
                    style = Cleared.type.rowFigure,
                    color = Cleared.semantics.onMoneyContainer
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "At that volume the difference is ${advice.annualSaving} a year.",
            style = Cleared.type.caption,
            color = Cleared.tones.onSurfaceVariant2
        )
    }
}

@Composable
private fun SettingsRows(state: WithdrawAdvisorUiState) {
    var notify by remember { mutableStateOf(false) }

    Column {
        HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Notify me at break-even",
                    style = Cleared.type.rowPrimary,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(state.breakEven, style = Cleared.type.caption, color = Cleared.tones.tertiary)
            }
            Switch(
                checked = notify,
                onCheckedChange = { notify = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = Cleared.tones.chipBg,
                    uncheckedBorderColor = Cleared.tones.outlineField
                )
            )
        }

        HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Currency exposure",
                style = Cleared.type.rowPrimary,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = state.fxExposure,
                    style = Cleared.type.rowFigure,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.height(2.dp))
                Text(state.fxCaption, style = Cleared.type.caption, color = Cleared.tones.tertiary)
            }
        }
        HorizontalDivider(thickness = Dimens.hairline, color = Cleared.tones.divider)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = Cleared.type.sectionOverline,
        color = Cleared.tones.label,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
