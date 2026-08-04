package app.cleared.ui.pipeline

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Week group headers: `THIS WEEK · 3–9 AUG`, `NEXT WEEK · 10–16 AUG`, `WEEK OF 17 AUG`.
 *
 * Weeks run Monday to Sunday — every `expectedWeekStart` in design/sample_data.json is a Monday, so
 * that is the convention the data already uses.
 *
 * One caveat worth knowing. The prototype's "today" is Sunday 2 August 2026, and it labels the week
 * beginning Monday 3 August as *This week*. Under a Monday-start week, Sunday the 2nd still belongs
 * to the week that began Monday 27 July, so the same group comes out as *Next week* here. The
 * design's wording is the colloquial one people use on a Sunday evening; this is the calendar one.
 * If the intent was a Sunday-start week, [currentWeekStart] is the single line to change.
 */
object WeekLabel {

    fun currentWeekStart(today: LocalDate): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun of(weekStart: LocalDate, today: LocalDate): String {
        val current = currentWeekStart(today)
        val prefix = when (weekStart) {
            current -> "This week"
            current.plusWeeks(1) -> "Next week"
            current.minusWeeks(1) -> "Last week"
            else -> return "Week of ${day(weekStart)} ${month(weekStart)}".uppercase(Locale.ROOT)
        }
        return "$prefix · ${range(weekStart)}".uppercase(Locale.ROOT)
    }

    /** `3–9 Aug`, or `31 Aug – 6 Sep` when the week straddles a month. */
    private fun range(weekStart: LocalDate): String {
        val end = weekStart.plusDays(6)
        return if (weekStart.month == end.month) {
            "${day(weekStart)}–${day(end)} ${month(weekStart)}"
        } else {
            "${day(weekStart)} ${month(weekStart)} – ${day(end)} ${month(end)}"
        }
    }

    private fun day(date: LocalDate) = date.dayOfMonth.toString()

    private fun month(date: LocalDate) =
        date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
}
