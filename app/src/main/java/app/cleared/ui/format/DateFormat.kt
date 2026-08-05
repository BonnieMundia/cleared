package app.cleared.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dates and times as the app writes them. Instants are stored as epoch millis UTC and always
 * formatted in `Africa/Nairobi` — the user is in one timezone and the records are his own.
 */
object DateFormat {

    val NAIROBI: ZoneId = ZoneId.of("Africa/Nairobi")

    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val timeOfDay = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    /** `4 Jul 2026`. */
    fun date(at: Instant, zone: ZoneId = NAIROBI): String =
        dayMonthYear.format(at.atZone(zone))

    /** `18 Jul`, for a line that already establishes the year. */
    fun shortDate(at: Instant, zone: ZoneId = NAIROBI): String =
        dayMonth.format(at.atZone(zone))

    fun shortDate(date: LocalDate): String = dayMonth.format(date)

    /** `09:14`. */
    fun time(at: Instant, zone: ZoneId = NAIROBI): String = timeOfDay.format(at.atZone(zone))

    /** `12 Jun 2026 · 09:14`, the timestamp under a timeline entry. */
    fun timestamp(at: Instant, zone: ZoneId = NAIROBI): String =
        "${date(at, zone)} · ${time(at, zone)}"

    /** `+7 d`, the gap between two timeline entries. Zero renders as `same day`. */
    fun delta(days: Long): String = if (days == 0L) "same day" else "+$days d"

    /** `22 d`. */
    fun days(days: Long): String = "$days d"
}
