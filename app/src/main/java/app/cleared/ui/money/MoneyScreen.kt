package app.cleared.ui.money

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cleared.data.model.Currency
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.theme.Dimens

/**
 * Frame `1c` — what is sitting in a wallet, what getting paid has cost, and which route to use next.
 *
 * Money in a wallet is not money you can spend, and this screen exists to make that gap visible.
 */
@Composable
fun MoneyScreen(
    state: MoneyUiState,
    modifier: Modifier = Modifier,
    onAmountChanged: (String) -> Unit = {},
    onCurrencyChanged: (Currency) -> Unit = {}
) {
    LazyColumn(modifier.fillMaxSize()) {
        item { SectionLabel("Sitting in wallets", top = 6.dp) }
        item { WalletCard(state) }

        item { SectionLabel("Cost of getting paid · this year") }
        item { CostBlock(state) }

        item { SectionLabel("Which route for this amount") }
        item { AmountInput(state, onAmountChanged, onCurrencyChanged) }
        item { Routes(state) }

        item { Footer() }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String, top: Dp = Dimens.sectionSpacing) {
    Text(
        text = text.uppercase(),
        style = Cleared.type.sectionOverline,
        color = Cleared.tones.label,
        modifier = Modifier.padding(horizontal = Dimens.screenGutter).padding(top = top, bottom = 8.dp)
    )
}

@Composable
private fun WalletCard(state: MoneyUiState) {
    Column(
        Modifier
            .padding(horizontal = Dimens.screenGutter)
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineCard, ClearedShape.card)
            .clip(ClearedShape.card)
    ) {
        state.wallets.forEachIndexed { index, wallet ->
            if (index > 0) {
                HorizontalDivider(thickness = Dimens.hairline, color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                Modifier.fillMaxWidth().padding(Dimens.cardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(wallet.provider, style = Cleared.type.rowPrimary, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(3.dp))
                    Text(wallet.balances, style = Cleared.type.captionFigure, color = Cleared.tones.tertiary2)
                }
                Text(
                    text = wallet.kes,
                    style = Cleared.type.rowFigure,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        HorizontalDivider(thickness = Dimens.hairline, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .background(Cleared.tones.surfaceLow)
                .padding(Dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.idleLabel,
                style = Cleared.type.caption,
                color = Cleared.tones.onSurfaceVariant2,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = state.walletTotal,
                style = Cleared.type.rowFigure.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun CostBlock(state: MoneyUiState) {
    Column(Modifier.padding(horizontal = Dimens.screenGutter).fillMaxWidth()) {
        Text(
            text = state.costTotal,
            style = Cleared.type.sectionFigure,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.height(4.dp))
        Text(state.costCaption, style = Cleared.type.caption, color = Cleared.tones.tertiary2)

        if (state.costSegments.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.splitBar)
                    .clip(ClearedShape.progressBar)
                    .background(Cleared.tones.divider)
            ) {
                state.costSegments.forEachIndexed { index, segment ->
                    if (segment.fraction > 0f) {
                        Box(
                            Modifier
                                .weight(segment.fraction)
                                .fillMaxHeight()
                                .background(segmentColor(index))
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            state.costSegments.forEachIndexed { index, segment ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(segmentColor(index), ClearedShape.phaseRail))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = segment.label,
                        style = Cleared.type.caption,
                        color = Cleared.tones.onSurfaceVariant2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = segment.amount,
                        style = Cleared.type.captionFigure,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/** The three money tones, darkest first — the bar reads left to right as largest to smallest. */
@Composable
private fun segmentColor(index: Int): Color = when (index) {
    0 -> Cleared.semantics.money
    1 -> Cleared.tones.moneyMid
    else -> Cleared.tones.moneyPale
}

@Composable
private fun AmountInput(
    state: MoneyUiState,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (Currency) -> Unit
) {
    Row(
        Modifier
            .padding(horizontal = Dimens.screenGutter)
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineField, ClearedShape.field)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.background(Cleared.tones.chipBg, ClearedShape.smallTile).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(Currency.USD, Currency.EUR).forEach { option ->
                val selected = option == state.currency
                Box(
                    Modifier
                        .background(
                            if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            ClearedShape.smallTile
                        )
                        .clickable { onCurrencyChanged(option) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = option.name,
                        style = Cleared.type.captionFigure,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else Cleared.tones.onSurfaceVariant2
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        BasicTextField(
            value = state.amount,
            onValueChange = onAmountChanged,
            textStyle = Cleared.type.sectionFigure.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                fontSize = 22.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(150.dp).padding(end = 8.dp),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (state.amount.isEmpty()) {
                        Text(
                            "0",
                            style = Cleared.type.sectionFigure.copy(fontSize = 22.sp),
                            color = Cleared.tones.ghost
                        )
                    }
                    field()
                }
            }
        )
    }
}

@Composable
private fun Routes(state: MoneyUiState) {
    Column(
        Modifier.padding(horizontal = Dimens.screenGutter).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        state.routes.forEach { route ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (route.isCheapest) Cleared.semantics.moneyContainer else Color.Transparent,
                        ClearedShape.card
                    )
                    .border(
                        Dimens.hairline,
                        if (route.isCheapest) Cleared.semantics.money else Cleared.tones.outlineCard,
                        ClearedShape.card
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = route.label,
                        style = Cleared.type.tableRow.copy(fontWeight = FontWeight.Medium),
                        color = if (route.isCheapest) Cleared.semantics.onMoneyContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = route.subLine,
                        style = Cleared.type.microAnnotation,
                        color = if (route.isCheapest) Cleared.semantics.onMoneyContainer
                        else Cleared.tones.tertiary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = route.net,
                        style = Cleared.type.rowFigure.copy(fontWeight = FontWeight.SemiBold),
                        color = if (route.isCheapest) Cleared.semantics.onMoneyContainer
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = route.delta,
                        style = Cleared.type.rowSubFigure,
                        color = if (route.isCheapest) Cleared.semantics.onMoneyContainer
                        else Cleared.tones.tertiary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun Footer() {
    Text(
        // design/SCREENS.md adds "Under USD 60 the M-Pesa routes win" here. It is not true of the
        // route constants in design/sample_data.json: Payoneer → M-Pesa carries both a higher flat
        // fee and a higher spread than Payoneer → Equity Bank, so it loses at every size. The
        // PayPal pair does cross over, at about USD 291. The general claim is the true one.
        text = "Fees are flat, so the ranking changes with size. Small amounts favour the route " +
            "with the lower fee; large ones favour the route with the lower spread.",
        style = Cleared.type.caption,
        color = Cleared.tones.tertiary2,
        modifier = Modifier.padding(horizontal = Dimens.screenGutter).padding(top = 16.dp)
    )
}
