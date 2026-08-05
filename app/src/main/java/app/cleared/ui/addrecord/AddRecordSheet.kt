package app.cleared.ui.addrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cleared.data.model.Currency
import app.cleared.data.model.Phase
import app.cleared.data.model.Stage
import app.cleared.ui.components.stageLabel
import app.cleared.ui.format.DateFormat
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedShape
import app.cleared.ui.format.MoneyFormat
import app.cleared.ui.theme.Dimens

/**
 * Frame `1f` — log work in under ten seconds.
 *
 * Everything is one tap except the amount: platform, currency, stage and the unpaid toggle are all
 * chips or buttons, hours moves in halves on a pair of 44 dp squares, and Save is pinned to the
 * bottom of the sheet inside thumb reach.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordSheet(
    state: AddRecordUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlatformSelected: (Long) -> Unit,
    onCurrencySelected: (Currency) -> Unit,
    onAmountChanged: (String) -> Unit,
    onHoursChanged: (Double) -> Unit,
    onUnpaidToggled: (Boolean) -> Unit,
    onStageSelected: (Stage) -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = ClearedShape.bottomSheet,
        tonalElevation = 0.dp,
        dragHandle = { DragHandle() }
    ) {
        Column(Modifier.padding(horizontal = Dimens.screenGutter).padding(bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Log work", style = Cleared.type.cardTitle, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(
                    text = DateFormat.date(state.today),
                    style = Cleared.type.captionFigure,
                    color = Cleared.tones.tertiary
                )
            }

            SectionHeader("Platform")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.platforms.forEach { platform ->
                    SelectableChip(
                        label = platform.name,
                        selected = platform.id == state.platformId,
                        onClick = { onPlatformSelected(platform.id) }
                    )
                }
            }

            SectionHeader("Amount")
            AmountField(
                currency = state.currency,
                amount = state.amount,
                onCurrencySelected = onCurrencySelected,
                onAmountChanged = onAmountChanged
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Hours")
                Spacer(Modifier.weight(1f))
                Text(
                    "Unpaid assessment",
                    style = Cleared.type.caption,
                    color = Cleared.tones.onSurfaceVariant2
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = state.unpaid,
                    onCheckedChange = onUnpaidToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = Cleared.tones.chipBg,
                        uncheckedBorderColor = Cleared.tones.outlineField
                    )
                )
            }
            HoursStepper(state.hours, onHoursChanged)

            SectionHeader("Stage")
            // The two-row split is the point: work above, money below, never merged.
            StageRow(Phase.WORK, state.stage, onStageSelected)
            Spacer(Modifier.height(8.dp))
            StageRow(Phase.MONEY, state.stage, onStageSelected)

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().height(Dimens.filledButton),
                shape = ClearedShape.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save", style = Cleared.type.rowPrimary)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Defaults come from the last record on this platform.",
                style = Cleared.type.caption,
                color = Cleared.tones.tertiary2,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DragHandle() {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 32.dp, height = 4.dp)
                .background(Cleared.tones.outlineField, ClearedShape.pill)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = Cleared.type.sectionOverline,
        color = Cleared.tones.label,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

/** Selected is an `onSurface` fill with a white label — the strongest contrast the palette has. */
@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                ClearedShape.filterChip
            )
            .border(
                Dimens.hairline,
                if (selected) MaterialTheme.colorScheme.onSurface else Cleared.tones.outlineField,
                ClearedShape.filterChip
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = Cleared.type.rowPrimary,
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AmountField(
    currency: Currency,
    amount: String,
    onCurrencySelected: (Currency) -> Unit,
    onAmountChanged: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(Dimens.hairline, Cleared.tones.outlineField, ClearedShape.field)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.background(Cleared.tones.chipBg, ClearedShape.smallTile).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Currency.entries.forEach { option ->
                val selected = option == currency
                Box(
                    Modifier
                        .background(
                            if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            ClearedShape.smallTile
                        )
                        .clickable { onCurrencySelected(option) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
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
            value = amount,
            onValueChange = onAmountChanged,
            textStyle = Cleared.type.sectionFigure.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                fontSize = 24.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(160.dp).padding(end = 8.dp)
        )
    }
}

@Composable
private fun HoursStepper(hours: Double, onChange: (Double) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepButton("−") { onChange((hours - 0.5).coerceAtLeast(0.0)) }
        Text(
            text = MoneyFormat.hours(hours),
            style = Cleared.type.sectionFigure.copy(fontSize = 24.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
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

/**
 * One row per phase. Selected is a filled chip in the family colour; unselected is an outline in
 * that family. Merging the rows would hide the boundary the whole app is about.
 */
@Composable
private fun StageRow(phase: Phase, selected: Stage, onSelect: (Stage) -> Unit) {
    val semantics = Cleared.semantics
    val stages = Stage.entries.filter { it.phase == phase }
    val fill = if (phase == Phase.WORK) semantics.work else semantics.money
    val outline = if (phase == Phase.WORK) semantics.workOutline else semantics.moneyOutline
    val onContainer = if (phase == Phase.WORK) semantics.onWorkContainer else semantics.onMoneyContainer

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        stages.forEach { stage ->
            val isSelected = stage == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (isSelected) fill else Color.Transparent, ClearedShape.filterChip)
                    .border(
                        Dimens.hairline,
                        if (isSelected) fill else outline,
                        ClearedShape.filterChip
                    )
                    .clickable { onSelect(stage) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stageLabel(stage),
                    style = Cleared.type.stageChip,
                    color = if (isSelected) Color.White else onContainer
                )
            }
        }
    }
}
