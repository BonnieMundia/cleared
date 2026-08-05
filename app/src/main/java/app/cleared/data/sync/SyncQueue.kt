package app.cleared.data.sync

import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.model.EventSource
import app.cleared.data.model.Stage
import app.cleared.data.model.SyncOpState
import java.time.Duration
import java.time.Instant

/** What the platform said about one op. There is no backend in v1; see [StubBackend]. */
sealed interface PushResult {
    data object Accepted : PushResult

    /** Transient — the op stays in the queue and backs off. */
    data class Retry(val reason: String) : PushResult

    /**
     * The platform reports a different stage for a record whose local event is newer. Both sides
     * are kept so frame `2a` can show them together with their timestamps.
     */
    data class Conflict(
        val remoteStage: Stage,
        val remoteOccurredAt: Instant,
        val remoteSource: EventSource = EventSource.PLATFORM_API
    ) : PushResult
}

fun interface SyncBackend {
    suspend fun push(op: SyncOpEntity): PushResult
}

/**
 * There is no server yet.
 *
 * design/README.md: "No backend for v1 except an FX-rate fetch." The queue, the ordering, the
 * backoff and the conflict path are all real and tested; this is the one seam where a real platform
 * API will eventually plug in, and until then everything the user writes offline is accepted when
 * connectivity returns.
 */
object StubBackend : SyncBackend {
    override suspend fun push(op: SyncOpEntity): PushResult = PushResult.Accepted
}

/**
 * The narrow view of the queue that [SyncQueueDrainer] needs, so the drain logic can be tested
 * without Room or an emulator.
 */
interface SyncQueuePort {
    suspend fun pending(): List<SyncOpEntity>
    suspend fun update(op: SyncOpEntity)
}

/**
 * Drains the offline queue.
 *
 * Replayed in ascending **id**, which design/DATA_MODEL.md names as the ordering key — two ops
 * written in the same millisecond have the same `createdAt` and no defined order under it, so id is
 * the one that is always total. Each op carries an idempotency key, so a partially-applied replay
 * is safe to run again.
 */
class SyncQueueDrainer(
    private val queue: SyncQueuePort,
    private val backend: SyncBackend,
    private val clock: () -> Instant = Instant::now
) {

    /** Ops that have failed this many times stop retrying and wait for the user. */
    private val maxAttempts = 5

    data class Outcome(val accepted: Int, val conflicted: Int, val retrying: Int, val failed: Int)

    suspend fun drain(): Outcome {
        val now = clock()
        var accepted = 0
        var conflicted = 0
        var retrying = 0
        var failed = 0

        for (op in queue.pending().sortedBy { it.id }) {
            // A backing-off op is not due yet. Skipping it rather than pushing it early is what
            // makes the backoff mean anything.
            if (op.nextAttemptAt?.isAfter(now) == true) {
                retrying++
                continue
            }

            when (val result = backend.push(op)) {
                is PushResult.Accepted -> {
                    queue.update(op.copy(state = SyncOpState.DONE, nextAttemptAt = null))
                    accepted++
                }

                is PushResult.Conflict -> {
                    queue.update(
                        op.copy(
                            state = SyncOpState.CONFLICT,
                            nextAttemptAt = null,
                            remoteStage = result.remoteStage,
                            remoteOccurredAt = result.remoteOccurredAt,
                            remoteSource = result.remoteSource
                        )
                    )
                    conflicted++
                }

                is PushResult.Retry -> {
                    val attempts = op.attempts + 1
                    if (attempts >= maxAttempts) {
                        queue.update(
                            op.copy(
                                state = SyncOpState.FAILED,
                                attempts = attempts,
                                nextAttemptAt = null,
                                lastError = result.reason
                            )
                        )
                        failed++
                    } else {
                        queue.update(
                            op.copy(
                                state = SyncOpState.RETRYING,
                                attempts = attempts,
                                nextAttemptAt = now.plus(backoff(attempts)),
                                lastError = result.reason
                            )
                        )
                        retrying++
                    }
                }
            }
        }

        return Outcome(accepted, conflicted, retrying, failed)
    }

    companion object {
        /** 1, 2, 4, 8 minutes. The fifth attempt is the last. */
        fun backoff(attempts: Int): Duration =
            Duration.ofMinutes(1L shl (attempts - 1).coerceIn(0, 4))
    }
}
