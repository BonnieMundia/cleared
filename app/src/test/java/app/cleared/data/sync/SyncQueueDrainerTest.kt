package app.cleared.data.sync

import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.model.EventSource
import app.cleared.data.model.Stage
import app.cleared.data.model.SyncOpState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * CLAUDE.md: "Test that offline mutations replay in SyncOp id order and are idempotent under
 * double-replay."
 */
class SyncQueueDrainerTest {

    private val now: Instant = Instant.parse("2026-08-05T09:00:00Z")

    /** An in-memory queue, so the drain logic is testable without Room or a device. */
    private class FakeQueue(ops: List<SyncOpEntity>) : SyncQueuePort {
        val rows = ops.associateBy { it.id }.toMutableMap()
        override suspend fun pending(): List<SyncOpEntity> = rows.values.filter {
            it.state == SyncOpState.WAITING || it.state == SyncOpState.RETRYING
        }
        override suspend fun update(op: SyncOpEntity) { rows[op.id] = op }
    }

    private fun op(
        id: Long,
        createdAt: Instant = now,
        state: SyncOpState = SyncOpState.WAITING,
        attempts: Int = 0,
        nextAttemptAt: Instant? = null
    ) = SyncOpEntity(
        id = id,
        entityType = "StageEvent",
        entityId = id,
        payload = "{}",
        idempotencyKey = "key-$id",
        createdAt = createdAt,
        attempts = attempts,
        nextAttemptAt = nextAttemptAt,
        state = state,
        sizeBytes = 96
    )

    /**
     * Ascending id, which is the ordering key DATA_MODEL.md names. Note the ops here were all
     * written in the *same millisecond* — ordering by `createdAt` would leave them undefined, which
     * is exactly why id is the key.
     */
    @Test
    fun `ops replay in ascending id order`() = runBlocking {
        val pushed = mutableListOf<Long>()
        val queue = FakeQueue(listOf(op(3), op(1), op(2)))
        val backend = SyncBackend { pushed += it.id; PushResult.Accepted }

        SyncQueueDrainer(queue, backend) { now }.drain()

        assertEquals(listOf(1L, 2L, 3L), pushed)
    }

    /**
     * Replaying a drained queue pushes nothing a second time: the ops are DONE and no longer
     * pending. A partially-applied replay is safe for the same reason.
     */
    @Test
    fun `a second drain is a no-op`() = runBlocking {
        var pushes = 0
        val queue = FakeQueue(listOf(op(1), op(2), op(3)))
        val backend = SyncBackend { pushes++; PushResult.Accepted }
        val drainer = SyncQueueDrainer(queue, backend) { now }

        val first = drainer.drain()
        val second = drainer.drain()

        assertEquals(3, first.accepted)
        assertEquals(0, second.accepted)
        assertEquals(3, pushes)
        assertTrue(queue.rows.values.all { it.state == SyncOpState.DONE })
    }

    /** A half-drained queue picks up where it left off, and never re-sends what already went. */
    @Test
    fun `a partial drain resumes without repeating itself`() = runBlocking {
        val pushed = mutableListOf<Long>()
        val queue = FakeQueue(listOf(op(1), op(2), op(3)))
        var failFrom = 2L

        val backend = SyncBackend { op ->
            pushed += op.id
            if (op.id >= failFrom) PushResult.Retry("network dropped") else PushResult.Accepted
        }

        SyncQueueDrainer(queue, backend) { now }.drain()
        assertEquals(listOf(1L, 2L, 3L), pushed)

        // The connection comes back; the two that failed are retried and the first is not.
        pushed.clear()
        failFrom = Long.MAX_VALUE
        SyncQueueDrainer(queue, backend) { now.plus(Duration.ofMinutes(30)) }.drain()

        assertEquals(listOf(2L, 3L), pushed)
        assertTrue(queue.rows.values.all { it.state == SyncOpState.DONE })
    }

    @Test
    fun `a failing op backs off exponentially`() = runBlocking {
        val queue = FakeQueue(listOf(op(1)))
        val backend = SyncBackend { PushResult.Retry("timeout") }

        SyncQueueDrainer(queue, backend) { now }.drain()
        val first = queue.rows.getValue(1L)
        assertEquals(SyncOpState.RETRYING, first.state)
        assertEquals(1, first.attempts)
        assertEquals(now.plus(Duration.ofMinutes(1)), first.nextAttemptAt)

        // Due again, and the wait doubles.
        SyncQueueDrainer(queue, backend) { now.plus(Duration.ofMinutes(2)) }.drain()
        val second = queue.rows.getValue(1L)
        assertEquals(2, second.attempts)
        assertEquals(now.plus(Duration.ofMinutes(2)).plus(Duration.ofMinutes(2)), second.nextAttemptAt)
    }

    /** An op that is not due yet is left alone — otherwise the backoff would mean nothing. */
    @Test
    fun `an op inside its backoff window is not pushed`() = runBlocking {
        var pushes = 0
        val queue = FakeQueue(
            listOf(op(1, state = SyncOpState.RETRYING, attempts = 1, nextAttemptAt = now.plusSeconds(60)))
        )
        val backend = SyncBackend { pushes++; PushResult.Accepted }

        val outcome = SyncQueueDrainer(queue, backend) { now }.drain()

        assertEquals(0, pushes)
        assertEquals(1, outcome.retrying)
        assertEquals(SyncOpState.RETRYING, queue.rows.getValue(1L).state)
    }

    @Test
    fun `an op gives up after five attempts`() = runBlocking {
        val queue = FakeQueue(listOf(op(1, state = SyncOpState.RETRYING, attempts = 4)))
        val backend = SyncBackend { PushResult.Retry("gone") }

        SyncQueueDrainer(queue, backend) { now }.drain()

        val row = queue.rows.getValue(1L)
        assertEquals(SyncOpState.FAILED, row.state)
        assertEquals(5, row.attempts)
        assertEquals("gone", row.lastError)
        assertNull("a failed op stops backing off", row.nextAttemptAt)
    }

    /** A conflict keeps both sides and stops retrying: it is the user's call, not the queue's. */
    @Test
    fun `a conflict keeps the platform's side and waits`() = runBlocking {
        val remoteAt = now.minus(Duration.ofMinutes(19))
        val queue = FakeQueue(listOf(op(1)))
        val backend = SyncBackend {
            PushResult.Conflict(Stage.REJECTED, remoteAt, EventSource.PLATFORM_API)
        }

        val outcome = SyncQueueDrainer(queue, backend) { now }.drain()

        val row = queue.rows.getValue(1L)
        assertEquals(1, outcome.conflicted)
        assertEquals(SyncOpState.CONFLICT, row.state)
        assertEquals(Stage.REJECTED, row.remoteStage)
        assertEquals(remoteAt, row.remoteOccurredAt)
        assertEquals(EventSource.PLATFORM_API, row.remoteSource)
        assertNull(row.nextAttemptAt)

        // And it is not picked up again by a later drain.
        var pushes = 0
        SyncQueueDrainer(queue, SyncBackend { pushes++; PushResult.Accepted }) { now }.drain()
        assertEquals(0, pushes)
    }

    @Test
    fun `backoff doubles and then stops doubling`() {
        assertEquals(Duration.ofMinutes(1), SyncQueueDrainer.backoff(1))
        assertEquals(Duration.ofMinutes(2), SyncQueueDrainer.backoff(2))
        assertEquals(Duration.ofMinutes(4), SyncQueueDrainer.backoff(3))
        assertEquals(Duration.ofMinutes(8), SyncQueueDrainer.backoff(4))
        assertNotNull(SyncQueueDrainer.backoff(9))
    }
}

