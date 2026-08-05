package app.cleared.data.derive

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Days, counted the way a person counts them: how many times the date changed, in the timezone the
 * records were made in.
 *
 * Not `Duration.toDays()`, which counts whole 24-hour periods and truncates. The record in frame
 * `1e` moves from In review at 18:02 on 12 June to Approved at 11:40 on 19 June — seven days later
 * by the calendar, six by the clock, and the design says `+7 d`. The same six-hour shortfall turns
 * an 18-day work phase into 17 and a 22-day record into 21.
 *
 * Instants are stored as epoch millis UTC and counted in `Africa/Nairobi`, which is where the work
 * was done.
 */
object CalendarDays {

    val ZONE: ZoneId = ZoneId.of("Africa/Nairobi")

    fun between(from: Instant, to: Instant, zone: ZoneId = ZONE): Long =
        ChronoUnit.DAYS.between(from.atZone(zone).toLocalDate(), to.atZone(zone).toLocalDate())
}
