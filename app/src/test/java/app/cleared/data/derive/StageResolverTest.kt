package app.cleared.data.derive

import app.cleared.data.db.entity.EarningRecordEntity
import app.cleared.data.db.entity.RecordDetail
import app.cleared.data.db.entity.StageEventEntity
import app.cleared.data.model.Currency
import app.cleared.data.model.EventSource
import app.cleared.data.model.Stage
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Current stage is the greatest `occurredAt`, ties broken by the greater `Stage.order`. Nothing
 * reads a stored column, because there is not one to read.
 */
class StageResolverTest {

    private val now = SampleData.NOW

    @Test
    fun `current stage is the latest event, not the last written`() {
        // Written second but happened first: the earlier event must not win.
        val detail = detailWith(
            ev(1, Stage.IN_REVIEW, now.minus(Duration.ofDays(2))),
            ev(2, Stage.SUBMITTED, now.minus(Duration.ofDays(9)))
        )
        assertEquals(Stage.IN_REVIEW, StageResolver.recordStage(detail))
    }

    @Test
    fun `a tie at the same instant is broken by the greater stage order`() {
        val at = now.minus(Duration.ofDays(1))
        val detail = detailWith(
            ev(1, Stage.APPROVED, at),
            ev(2, Stage.IN_REVIEW, at)
        )
        assertEquals(Stage.APPROVED, StageResolver.recordStage(detail))
    }

    /**
     * A correction is a new event. The log keeps both, and the record reads the correction.
     */
    @Test
    fun `a correction appended later wins without anything being rewritten`() {
        val original = detailWith(
            ev(1, Stage.SUBMITTED, now.minus(Duration.ofDays(10))),
            ev(2, Stage.APPROVED, now.minus(Duration.ofDays(3)))
        )
        assertEquals(Stage.APPROVED, StageResolver.recordStage(original))

        val corrected = original.copy(
            events = original.events + ev(3, Stage.REJECTED, now.minus(Duration.ofDays(1)))
        )
        assertEquals(Stage.REJECTED, StageResolver.recordStage(corrected))
        assertEquals("nothing is deleted", 3, corrected.events.size)
    }

    /**
     * Age is measured from the first event of the *current* phase, not from creation. Record 5 has
     * been in the work phase 31 days even though it entered In review later than that.
     */
    @Test
    fun `age is measured from the start of the current phase`() {
        assertEquals(31L, StageResolver.daysInCurrentPhase(SampleData.pipelineById.getValue(5L), now))
        assertEquals(3L, StageResolver.daysInCurrentPhase(SampleData.pipelineById.getValue(1L), now))
        assertEquals(41L, StageResolver.daysInCurrentPhase(SampleData.pipelineById.getValue(6L), now))
    }

    /** Every one of the eight sample records resolves to the stage the JSON gives it. */
    @Test
    fun `every sample record resolves to its documented stage`() {
        val expected = mapOf(
            1L to Stage.RECEIVED,
            2L to Stage.PAYOUT_ISSUED,
            3L to Stage.APPROVED,
            4L to Stage.APPROVED,
            5L to Stage.IN_REVIEW,
            6L to Stage.SUBMITTED,
            7L to Stage.SUBMITTED,
            8L to Stage.REJECTED
        )
        for ((id, stage) in expected) {
            assertEquals("record $id", stage, StageResolver.recordStage(SampleData.pipelineById.getValue(id)))
        }
    }

    private fun detailWith(vararg events: StageEventEntity) = RecordDetail(
        record = EarningRecordEntity(
            id = 1,
            platformId = SampleData.HALO,
            grossMinor = 10_000,
            currency = Currency.USD,
            hoursWorked = 1.0,
            hoursUnpaid = 0.0,
            expectedWeekStart = LocalDate.of(2026, 8, 3),
            createdAt = now
        ),
        events = events.toList()
    )

    private fun ev(id: Long, stage: Stage, at: Instant) = StageEventEntity(
        id = id,
        recordId = 1,
        stage = stage,
        occurredAt = at,
        source = EventSource.MANUAL,
        idempotencyKey = "test:$id"
    )
}
