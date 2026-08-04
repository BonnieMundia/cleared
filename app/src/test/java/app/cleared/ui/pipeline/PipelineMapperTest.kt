package app.cleared.ui.pipeline

import app.cleared.data.derive.SettleTime
import app.cleared.data.model.Stage
import app.cleared.fixture.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Frame `1a`, assembled from the derivations — the hero, the split bar, the week groups and the
 * copy around them.
 */
class PipelineMapperTest {

    private val today = LocalDate.of(2026, 8, 2)

    private fun state(
        states: List<app.cleared.data.derive.RecordState> = SampleData.states,
        p90: Map<Long, Int> = SettleTime.p90ByPlatform(
            SampleData.platforms.map { it.id }, SampleData.states, SampleData.NOW
        )
    ) = PipelineMapper.build(
        states = states,
        platforms = SampleData.platforms,
        rates = SampleData.RATES,
        p90ByPlatform = p90,
        now = SampleData.NOW,
        today = today
    )

    @Test
    fun `the hero carries the owed figure and its components`() {
        val ui = state()
        assertEquals(247_119L, ui.owedKes)
        assertEquals("247,119", ui.heroDigits)
        assertEquals(listOf("USD 947.50", "EUR 850.00"), ui.components)
    }

    @Test
    fun `the split bar is sized by the work share`() {
        val ui = state()
        assertEquals("Work KES 188,183", ui.workLegend)
        assertEquals("Money KES 58,936", ui.moneyLegend)
        assertEquals(188_183f / 247_119f, ui.workFraction, 1e-4f)
    }

    /** Three week groups, each with the subtotal from design/sample_data.json. */
    @Test
    fun `records are grouped by expected arrival week with subtotals`() {
        val groups = state().groups.filterNot { it.isNeedsAttention }
        assertEquals(3, groups.size)
        assertEquals("KES 64,393", groups[0].subtitleFigure)
        assertEquals("KES 151,730", groups[1].subtitleFigure)
        assertEquals("KES 30,996", groups[2].subtitleFigure)
    }

    /**
     * On the prototype's Sunday "today", a Monday-start week puts the 3 August group in *next*
     * week. See the note on [WeekLabel].
     */
    @Test
    fun `week headers name the week`() {
        val groups = state().groups.filterNot { it.isNeedsAttention }
        assertEquals("NEXT WEEK · 3–9 AUG", groups[0].title)
        assertEquals("WEEK OF 10 AUG", groups[1].title)
        assertEquals("WEEK OF 17 AUG", groups[2].title)
    }

    @Test
    fun `the caption counts open records and drops a zero overdue clause`() {
        val ui = state()
        assertEquals(0, ui.overdueCount)
        assertEquals("7 open records", ui.caption)
    }

    @Test
    fun `the caption names the overdue count when there is one`() {
        // The sample's own flags are p50-based; at p50 two records are late.
        val p50 = SampleData.platforms.associate {
            it.id to SettleTime.of(it.id, SampleData.states, SampleData.NOW).p50Days!!
        }
        val ui = state(p90 = p50)
        assertEquals(2, ui.overdueCount)
        assertEquals("7 open records · 2 past their usual settle time", ui.caption)
    }

    /** The rejected record still appears in its week, struck through and worth nothing. */
    @Test
    fun `a rejected record stays on the screen`() {
        val rows = state().groups.flatMap { it.rows }
        val rejected = rows.first { it.stage == Stage.REJECTED }
        assertEquals("Halo Data", rejected.platformName)
        assertEquals("USD 15.75", rejected.grossText)
        assertEquals("no payout", rejected.kesText)
        assertEquals("closed", rejected.ageText)
    }

    @Test
    fun `every row carries a KES figure and a currency code`() {
        val rows = state().groups.flatMap { it.rows }
        assertEquals(8, rows.size)
        for (row in rows) {
            assertTrue(row.grossText, row.grossText.take(3).all { it.isUpperCase() })
            assertFalse(row.grossText, row.grossText.any { it in "$€£" })
        }
    }

    /** The band is absent when empty — never shown with a zero. */
    @Test
    fun `the needs-attention band is absent when nothing is reversed`() {
        assertTrue(state().groups.none { it.isNeedsAttention })
    }

    @Test
    fun `a landed record leaves the hero, its week subtotal and the open count`() {
        val landed = SampleData.all.map { detail ->
            if (detail.record.id != 1L) app.cleared.data.derive.RecordState.of(detail)
            else app.cleared.data.derive.RecordState.of(
                detail.copy(
                    events = detail.events + app.cleared.data.db.entity.StageEventEntity(
                        id = 99_001,
                        recordId = 1,
                        stage = Stage.LANDED,
                        occurredAt = SampleData.NOW,
                        source = app.cleared.data.model.EventSource.MANUAL,
                        idempotencyKey = "test:landed"
                    )
                )
            )
        }

        val before = state()
        val after = state(states = landed)

        assertEquals(before.owedKes - 23_626L, after.owedKes)
        assertEquals(6, after.openCount)
        assertEquals(
            "KES 40,767",
            after.groups.first { it.title.endsWith("3–9 AUG") }.subtitleFigure
        )
    }
}
