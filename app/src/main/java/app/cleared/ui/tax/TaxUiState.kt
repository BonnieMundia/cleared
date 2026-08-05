package app.cleared.ui.tax

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.Tax
import app.cleared.data.derive.TaxSummary
import app.cleared.ui.format.DateFormat
import app.cleared.ui.format.MoneyFormat
import java.time.LocalDate

data class TaxBlockUi(
    val overline: String,
    val figure: String,
    val context: String,
    val rowLabel: String,
    val rowValue: String
)

data class TaxUiState(
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val personal: TaxBlockUi? = null,
    val company: TaxBlockUi? = null,
    val setAside: String = "KES 0",
    val setAsideOf: String = "of KES 0",
    val setAsideFraction: Float = 0f,
    val shortfall: String? = null,
    val heldIn: String? = null,
    val exportLabel: String = "Export CSV",
    val exportCaption: String = "",
    val recordCount: Int = 0,
    val loading: Boolean = true
)

/**
 * Frame `1d` — deliberately plain.
 *
 * Personal income and company turnover are kept apart because they are taxed differently, and
 * running them together is how people underpay.
 */
object TaxMapper {

    fun build(
        states: List<RecordState>,
        platforms: List<PlatformEntity>,
        selectedYear: Int?,
        actualSetAsideKes: Long,
        personalRate: Double,
        turnoverTaxRate: Double,
        setAsideLocation: String?,
        setAsideLastMoved: LocalDate?
    ): TaxUiState {
        val summary = Tax.summarise(
            platforms = platforms,
            states = states,
            actualSetAsideKes = actualSetAsideKes,
            personalRate = personalRate,
            turnoverTaxRate = turnoverTaxRate,
            year = selectedYear
        )
        val companyName = platforms.firstOrNull { it.isCompany }?.name

        return TaxUiState(
            years = Tax.yearsWithIncome(states),
            selectedYear = selectedYear,
            personal = personalBlock(summary, personalRate),
            company = companyBlock(summary, turnoverTaxRate, companyName),
            setAside = MoneyFormat.kes(summary.actualSetAsideKes),
            setAsideOf = "of ${MoneyFormat.kes(summary.recommendedSetAsideKes)}",
            setAsideFraction = if (summary.recommendedSetAsideKes <= 0) 0f
            else (summary.actualSetAsideKes.toDouble() / summary.recommendedSetAsideKes)
                .toFloat().coerceIn(0f, 1f),
            shortfall = summary.shortfallKes.takeIf { it > 0 }?.let { MoneyFormat.kes(it) },
            heldIn = setAsideLocation?.let { location ->
                val moved = setAsideLastMoved?.let { " · last moved ${DateFormat.shortDate(it)}" } ?: ""
                "Held in $location$moved"
            },
            exportLabel = "Export CSV" + (selectedYear?.let { " · $it" } ?: " · all"),
            exportCaption = exportCaption(summary),
            recordCount = summary.personalRecordCount + summary.companyRecordCount,
            loading = false
        )
    }

    private fun personalBlock(summary: TaxSummary, rate: Double) = TaxBlockUi(
        overline = "Personal income",
        figure = MoneyFormat.kes(summary.personalIncomeKes),
        context = "Landed from ${summary.personalPlatformCount} platforms · " +
            "${summary.personalRecordCount} records",
        rowLabel = "Set aside at ${MoneyFormat.percent(rate * 100, decimals = 0)}",
        rowValue = MoneyFormat.kes(summary.personalSetAsideKes)
    )

    private fun companyBlock(summary: TaxSummary, rate: Double, companyName: String?) = TaxBlockUi(
        overline = "Company income" + (companyName?.let { " · $it" } ?: ""),
        figure = MoneyFormat.kes(summary.companyIncomeKes),
        // Kenya's turnover tax applies below a 5M threshold; above it the company files differently,
        // so the line states which side of it the figure sits on.
        context = "Turnover · ${summary.companyRecordCount} records · " +
            if (summary.companyIncomeKes < 5_000_000) "below the 5M threshold"
            else "above the 5M threshold",
        rowLabel = "Turnover tax at ${MoneyFormat.percent(rate * 100, decimals = 0)}",
        rowValue = MoneyFormat.kes(summary.companyTaxKes)
    )

    private fun exportCaption(summary: TaxSummary): String {
        val count = summary.personalRecordCount + summary.companyRecordCount
        val records = if (count == 1) "1 record" else "$count records"
        return "$records with every fee, rate and timestamp. " +
            "Personal and company rows are tagged separately."
    }
}
