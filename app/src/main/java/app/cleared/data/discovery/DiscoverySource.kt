package app.cleared.data.discovery

import app.cleared.data.db.entity.ListingEntity
import app.cleared.data.model.Currency
import app.cleared.data.model.Money
import java.time.Duration
import java.time.Instant

/** The result of a scan: what was found, from where, and when. */
data class ScanResult(
    val listings: List<ListingEntity>,
    val scannedAt: Instant,
    val boardCount: Int,
    val feedCount: Int
)

/**
 * Where Discovery gets its listings.
 *
 * **The sources are not decided.** CLAUDE.md lists them under "Not yet designed — ask before
 * inventing": the design says "6 platform boards and 2 community feeds" without naming one. So this
 * is an interface with a fixture behind it, and the real implementation is a drop-in replacement
 * once somebody says which boards.
 *
 * Whatever ends up here reads **public** boards and feeds. It never signs in as the user and never
 * applies on his behalf — tapping a listing opens the platform in a browser, and that is the only
 * way anything is ever submitted.
 */
interface DiscoverySource {
    suspend fun scan(): ScanResult
}

/**
 * The listings from design/sample_data.json, so `3a` and `3b` can be built and reviewed before the
 * sources question is answered. No network calls.
 */
class FixtureDiscoverySource(
    private val clock: () -> Instant = Instant::now
) : DiscoverySource {

    override suspend fun scan(): ScanResult {
        val now = clock()
        return ScanResult(
            listings = listOf(
                listing(
                    id = 1,
                    platform = "Northline Freelance",
                    title = "Landing page copy, fixed price",
                    kind = "Writing",
                    pay = "400.00",
                    currency = Currency.USD,
                    estHours = 14.0,
                    assessmentHours = 0.0,
                    source = "platform board",
                    seenAt = now.minus(Duration.ofMinutes(41)),
                    note = "New client, no history on the platform. Escrow releases on approval, " +
                        "median 9 d."
                ),
                listing(
                    id = 2,
                    platform = "Lumen Writers",
                    title = "Technical explainer series, 6 pieces",
                    kind = "Writing",
                    pay = "540.00",
                    currency = Currency.EUR,
                    estHours = 34.0,
                    assessmentHours = 0.0,
                    source = "platform board",
                    seenAt = now.minus(Duration.ofHours(2)),
                    note = "You are already onboarded. Slowest payer you use — 24 d median, 41 d " +
                        "at p90."
                ),
                listing(
                    id = 3,
                    platform = "Halo Data",
                    title = "Dialogue rating, Swahili–English",
                    kind = "AI training",
                    pay = "360.00",
                    currency = Currency.USD,
                    estHours = 26.0,
                    assessmentHours = 2.0,
                    source = "platform board",
                    seenAt = now.minus(Duration.ofHours(3)),
                    note = "Language pair pays a premium over your usual queue here. 2 h " +
                        "calibration set is unpaid."
                ),
                listing(
                    id = 4,
                    platform = "Meridian Transcribe",
                    title = "Audio QA, 12 h per week",
                    kind = "AI training",
                    pay = "210.00",
                    currency = Currency.USD,
                    estHours = 24.0,
                    assessmentHours = 5.0,
                    source = "community feed",
                    seenAt = now.minus(Duration.ofHours(5)),
                    note = "Platform you have never used. No settle-time history, so the days " +
                        "figure is their claim, not your data."
                ),
                listing(
                    id = 5,
                    platform = "Vector Annotate",
                    title = "Bounding box batch, 900 frames",
                    kind = "AI training",
                    pay = "65.00",
                    currency = Currency.USD,
                    estHours = 9.0,
                    assessmentHours = 3.0,
                    source = "platform board",
                    seenAt = now.minus(Duration.ofHours(6)),
                    note = "Your worst platform. 39% of what you submit here is rejected, and the " +
                        "hours still count."
                )
            ),
            scannedAt = now,
            boardCount = 6,
            feedCount = 2
        )
    }

    private fun listing(
        id: Long,
        platform: String,
        title: String,
        kind: String,
        pay: String,
        currency: Currency,
        estHours: Double,
        assessmentHours: Double,
        source: String,
        seenAt: Instant,
        note: String
    ) = ListingEntity(
        id = id,
        platformName = platform,
        title = title,
        kind = kind,
        statedPayMinor = Money.minorOf(pay),
        currency = currency,
        estHours = estHours,
        assessmentHours = assessmentHours,
        sourceLabel = source,
        sourceUrl = null,
        seenAt = seenAt,
        note = note
    )
}
