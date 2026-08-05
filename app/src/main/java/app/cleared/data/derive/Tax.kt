package app.cleared.data.derive

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.model.Money
import app.cleared.data.model.Stage
import java.math.BigDecimal
import java.time.ZoneId

/**
 * Frame `1d`. Personal and company income kept apart, because they are taxed differently and
 * conflating them is how people underpay.
 *
 * Both rates are user-editable: tax law changes and this app should not need a release to follow it.
 */
data class TaxSummary(
    val personalIncomeKes: Long,
    val companyIncomeKes: Long,
    val personalSetAsideKes: Long,
    val companyTaxKes: Long,
    val recommendedSetAsideKes: Long,
    val actualSetAsideKes: Long,
    val personalRecordCount: Int,
    val personalPlatformCount: Int,
    val companyRecordCount: Int
) {
    val shortfallKes: Long get() = recommendedSetAsideKes - actualSetAsideKes
}

object Tax {

    const val DEFAULT_PERSONAL_RATE = 0.25
    const val DEFAULT_TURNOVER_RATE = 0.03

    /**
     *     recommendedSetAside = personalIncomeYtd × personalRate
     *                         + companyIncomeYtd  × turnoverTaxRate
     *
     * Returns the two halves separately, because the Tax screen shows them separately — conflating
     * personal income with company turnover is how people underpay.
     */
    fun setAside(
        personalIncomeKes: Long,
        companyIncomeKes: Long,
        personalRate: Double = DEFAULT_PERSONAL_RATE,
        turnoverTaxRate: Double = DEFAULT_TURNOVER_RATE
    ): Pair<Long, Long> = Pair(
        Money.toKes(BigDecimal.valueOf(personalIncomeKes).multiply(Money.pct(personalRate))),
        Money.toKes(BigDecimal.valueOf(companyIncomeKes).multiply(Money.pct(turnoverTaxRate)))
    )

    /**
     * @param year restricts to money that landed in that calendar year, in `Africa/Nairobi`. Null
     *        is the `All` tab. Income is recognised when it lands, not when the work was done —
     *        which is also the only date the app can defend, since it is the one on the event.
     */
    fun summarise(
        platforms: List<PlatformEntity>,
        states: List<RecordState>,
        actualSetAsideKes: Long,
        personalRate: Double = DEFAULT_PERSONAL_RATE,
        turnoverTaxRate: Double = DEFAULT_TURNOVER_RATE,
        year: Int? = null,
        zone: ZoneId = CalendarDays.ZONE
    ): TaxSummary {
        val companyIds = platforms.filter { it.isCompany }.map { it.id }.toSet()

        var personal = BigDecimal.ZERO
        var company = BigDecimal.ZERO
        val personalPlatforms = mutableSetOf<Long>()
        var personalRecords = 0
        var companyRecords = 0

        for (state in states) {
            if (year != null && landedYear(state, zone) != year) continue
            val cleared = Ledger.finalKesCleared(state.detail)
            if (cleared.signum() == 0) continue
            if (state.record.platformId in companyIds) {
                company += cleared
                companyRecords++
            } else {
                personal += cleared
                personalPlatforms += state.record.platformId
                personalRecords++
            }
        }

        val personalKes = Money.toKes(personal)
        val companyKes = Money.toKes(company)
        val (personalSetAside, turnoverTax) = setAside(personalKes, companyKes, personalRate, turnoverTaxRate)

        return TaxSummary(
            personalIncomeKes = personalKes,
            companyIncomeKes = companyKes,
            personalSetAsideKes = personalSetAside,
            companyTaxKes = turnoverTax,
            recommendedSetAsideKes = personalSetAside + turnoverTax,
            actualSetAsideKes = actualSetAsideKes,
            personalRecordCount = personalRecords,
            personalPlatformCount = personalPlatforms.size,
            companyRecordCount = companyRecords
        )
    }

    /** The year the record's money landed in, or null if it has not. */
    fun landedYear(state: RecordState, zone: ZoneId = CalendarDays.ZONE): Int? =
        state.detail.events
            .filter { it.stage == Stage.LANDED }
            .maxByOrNull { it.occurredAt }
            ?.occurredAt
            ?.atZone(zone)?.year

    /** Every year in which anything landed, most recent first — the year tabs on frame `1d`. */
    fun yearsWithIncome(states: List<RecordState>, zone: ZoneId = CalendarDays.ZONE): List<Int> =
        states.mapNotNull { landedYear(it, zone) }.distinct().sortedDescending()
}
