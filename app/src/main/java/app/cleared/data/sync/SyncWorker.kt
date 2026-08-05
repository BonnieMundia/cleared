package app.cleared.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.cleared.data.db.ClearedDatabase
import app.cleared.data.db.entity.SyncOpEntity
import app.cleared.data.model.SyncOpState
import java.time.Duration

/**
 * Drains the offline queue when connectivity returns.
 *
 * `WorkManager` handles the "when": the work is constrained to a connected network and survives the
 * app being killed. [SyncQueueDrainer] handles the "what", and is plain Kotlin so it can be tested
 * without any of this.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = ClearedDatabase.get(applicationContext)
        val dao = db.syncOpDao()

        val port = object : SyncQueuePort {
            override suspend fun pending(): List<SyncOpEntity> = dao.pending()
            override suspend fun update(op: SyncOpEntity) = dao.update(op)
        }

        val outcome = SyncQueueDrainer(port, StubBackend).drain()

        // Anything still backing off gets another pass; a conflict waits for the user and is not a
        // reason to retry. Failure here means "run me again", never "the app is broken".
        return if (outcome.retrying > 0) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "cleared-sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(1))
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        /** Used by the Sync screen's `Retry now`, which should not wait for a backoff window. */
        fun retryNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

/** Queue states that still count as "not yet sent" for the strip's count. */
val PendingStates = listOf(SyncOpState.WAITING, SyncOpState.RETRYING)
