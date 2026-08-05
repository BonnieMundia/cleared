package app.cleared.data.export

import app.cleared.data.db.entity.PlatformEntity
import app.cleared.data.derive.Ledger
import app.cleared.data.derive.RecordState
import app.cleared.data.derive.Tax
import app.cleared.data.model.Money
import app.cleared.data.model.Stage
import java.time.ZoneId

/**
 * Frame `1d`: "118 records with every fee, rate and timestamp. Personal and company rows are tagged
 * separately."
 *
 * A pure function over the derived records, so what gets exported is the same arithmetic the
 * screens show rather than a second implementation of it.
 */
object CsvExport {

    private val COLUMNS = listOf(
        "record_id", "platform", "tagged_as", "currency", "gross", "hours_worked", "hours_unpaid",
        "stage", "submitted_at", "landed_at", "rate_applied", "mid_rate",
        "fees", "cleared_kes"
    )

    fun build(
        states: List<RecordState>,
        platforms: List<PlatformEntity>,
        year: Int? = null,
        zone: ZoneId = ZoneId.of("Africa/Nairobi")
    ): String {
        val byId = platforms.associateBy { it.id }
        val rows = states
            .filter { year == null || Tax.landedYear(it, zone) == year }
            .sortedBy { it.record.id }

        return buildString {
            appendLine(COLUMNS.joinToString(","))
            for (state in rows) {
                val record = state.record
                val platform = byId[record.platformId]
                val events = state.detail.events
                val conversion = state.detail.conversions.firstOrNull()

                val cells = listOf(
                    record.id.toString(),
                    platform?.name.orEmpty(),
                    // Personal and company rows are tagged separately, because they are taxed
                    // differently and a single untagged export is useless at filing time.
                    if (platform?.isCompany == true) "company" else "personal",
                    record.currency.name,
                    Money.fromMinor(record.grossMinor).toPlainString(),
                    record.hoursWorked.toString(),
                    record.hoursUnpaid.toString(),
                    state.displayStage.name,
                    events.filter { it.stage == Stage.SUBMITTED }.minByOrNull { it.occurredAt }
                        ?.occurredAt?.toString().orEmpty(),
                    events.filter { it.stage == Stage.LANDED }.maxByOrNull { it.occurredAt }
                        ?.occurredAt?.toString().orEmpty(),
                    conversion?.rateApplied?.toPlainString().orEmpty(),
                    conversion?.midRate?.toPlainString().orEmpty(),
                    // Every fee, as one cell rather than a ragged row width.
                    state.detail.fees.joinToString(" | ") {
                        "${it.kind.name}:${it.currency.name} ${Money.fromMinor(it.amountMinor).toPlainString()}"
                    },
                    Money.toKes(Ledger.finalKesCleared(state.detail)).toString()
                )
                appendLine(cells.joinToString(",") { escape(it) })
            }
        }
    }

    fun fileName(year: Int?): String = if (year == null) "cleared-all.csv" else "cleared-$year.csv"

    /** RFC 4180: quote anything containing a comma, a quote or a newline, and double the quotes. */
    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
