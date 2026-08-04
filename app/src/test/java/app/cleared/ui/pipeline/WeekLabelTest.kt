package app.cleared.ui.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekLabelTest {

    /** Wednesday 5 August 2026, comfortably inside the week beginning Monday the 3rd. */
    private val midweek = LocalDate.of(2026, 8, 5)

    @Test
    fun `the current week reads This week with its date range`() {
        assertEquals("THIS WEEK · 3–9 AUG", WeekLabel.of(LocalDate.of(2026, 8, 3), midweek))
    }

    @Test
    fun `the following week reads Next week`() {
        assertEquals("NEXT WEEK · 10–16 AUG", WeekLabel.of(LocalDate.of(2026, 8, 10), midweek))
    }

    @Test
    fun `anything further out reads Week of`() {
        assertEquals("WEEK OF 17 AUG", WeekLabel.of(LocalDate.of(2026, 8, 17), midweek))
        assertEquals("WEEK OF 24 AUG", WeekLabel.of(LocalDate.of(2026, 8, 24), midweek))
    }

    @Test
    fun `a week straddling two months names both`() {
        assertEquals(
            "THIS WEEK · 31 AUG – 6 SEP",
            WeekLabel.of(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 2))
        )
    }

    /**
     * The prototype's own "today" is Sunday 2 August, and it labels the 3 August group *This week*.
     * Under a Monday-start week — which is what every `expectedWeekStart` in the sample data is —
     * Sunday the 2nd still belongs to the week that began Monday 27 July, so the same group is next
     * week. This pins the calendar reading; [WeekLabel.currentWeekStart] is the line to change if
     * the design meant a Sunday-start week.
     */
    @Test
    fun `on a Sunday the upcoming Monday group is next week, not this week`() {
        val sunday = LocalDate.of(2026, 8, 2)
        assertEquals(LocalDate.of(2026, 7, 27), WeekLabel.currentWeekStart(sunday))
        assertEquals("NEXT WEEK · 3–9 AUG", WeekLabel.of(LocalDate.of(2026, 8, 3), sunday))
    }
}
