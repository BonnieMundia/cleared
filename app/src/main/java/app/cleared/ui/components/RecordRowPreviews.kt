package app.cleared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.cleared.data.model.Stage
import app.cleared.ui.theme.Cleared
import app.cleared.ui.theme.ClearedTheme
import app.cleared.ui.theme.Dimens

/**
 * Frame `1h` — the record row in every variant it has, light and dark.
 *
 * The figures are the ones from design/sample_data.json so the previews can be read straight
 * against design/Cleared.dc.html. Each row carries a Mono caption naming the variant, as the
 * components sheet does.
 */
@Composable
private fun RecordRowSheet() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Caption("PRE — not yet work")
            Labelled("prospect · dashed rail and chip, never filled") {
                RecordRow(
                    RecordRowUi(
                        id = 101,
                        platformName = "Halo Data",
                        stage = Stage.PROSPECT,
                        grossText = "USD 360.00",
                        kesText = "KES 46,224 projected",
                        ageText = "0d"
                    )
                )
            }

            Caption("WORK PHASE — violet")
            Labelled("submitted") {
                RecordRow(
                    RecordRowUi(102, "Lumen Writers", Stage.SUBMITTED, "EUR 210.00", "KES 30,996", "1d")
                )
            }
            Labelled("in review · overdue, amber age pill") {
                RecordRow(
                    RecordRowUi(
                        id = 103,
                        platformName = "Lumen Writers",
                        stage = Stage.IN_REVIEW,
                        grossText = "EUR 640.00",
                        kesText = "KES 94,464",
                        ageText = "31d · 7 over",
                        overdue = true
                    )
                )
            }
            Labelled("approved") {
                RecordRow(
                    RecordRowUi(104, "Kibo Studio", Stage.APPROVED, "USD 350.00", "KES 44,940", "4d")
                )
            }

            Caption("MONEY PHASE — green")
            Labelled("payout issued") {
                RecordRow(
                    RecordRowUi(105, "Northline Freelance", Stage.PAYOUT_ISSUED, "USD 275.00", "KES 35,310", "5d")
                )
            }
            Labelled("received · in the wallet, not the bank") {
                RecordRow(
                    RecordRowUi(106, "Halo Data", Stage.RECEIVED, "USD 184.00", "KES 23,626", "3d")
                )
            }
            Labelled("landed · cleared figure in green") {
                RecordRow(
                    RecordRowUi(107, "Lumen Writers", Stage.LANDED, "EUR 640.00", "KES 88,220 cleared", "22d")
                )
            }
            Labelled("part paid · rail split at the cleared fraction") {
                RecordRow(
                    RecordRowUi(
                        id = 108,
                        platformName = "Lumen Writers",
                        stage = Stage.PAYOUT_ISSUED,
                        // The row shows the remaining settlement, not the record total, because it
                        // lives under the week that remainder is expected and the subtotal must add up.
                        grossText = "USD 400.00",
                        kesText = "KES 51,360",
                        ageText = "6d",
                        clearedFraction = 0.6f,
                        chipLabel = "Part paid · 40% left"
                    )
                )
            }

            Caption("TERMINAL — red, and neither one advances")
            Labelled("rejected · struck through, no payout") {
                RecordRow(
                    RecordRowUi(109, "Halo Data", Stage.REJECTED, "USD 15.75", "no payout", "closed")
                )
            }
            Labelled("reversed · not struck, the money existed") {
                RecordRow(
                    RecordRowUi(
                        id = 110,
                        platformName = "Halo Data",
                        stage = Stage.REVERSED,
                        grossText = "USD 200.00",
                        kesText = "KES 1,412 lost",
                        ageText = "17d stalled",
                        overdue = true
                    )
                )
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = Cleared.type.sectionOverline,
        color = Cleared.tones.label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.screenGutter, end = Dimens.screenGutter, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun Labelled(caption: String, content: @Composable () -> Unit) {
    content()
    Text(
        text = caption,
        style = Cleared.type.microAnnotation,
        color = Cleared.tones.tertiary,
        modifier = Modifier.padding(start = Dimens.screenGutter, top = 2.dp, bottom = 6.dp)
    )
}

@Preview(name = "Record row · light", showBackground = true, widthDp = 390, heightDp = 1180)
@Composable
private fun RecordRowSheetLight() = ClearedTheme(darkTheme = false) { RecordRowSheet() }

@Preview(name = "Record row · dark", showBackground = true, widthDp = 390, heightDp = 1180)
@Composable
private fun RecordRowSheetDark() = ClearedTheme(darkTheme = true) { RecordRowSheet() }
