package app.cleared

import android.app.Application
import app.cleared.data.ClearedRepository
import app.cleared.data.db.ClearedDatabase
import app.cleared.data.seed.DevSeed
import app.cleared.data.sync.Connectivity
import app.cleared.data.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point, and for now the whole dependency graph — there are two objects in it and
 * a DI framework would be a dependency the design has not asked for.
 *
 * The WorkManager sync queue is wired here in step 8.
 */
class ClearedApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: ClearedDatabase by lazy { ClearedDatabase.get(this) }
    val repository: ClearedRepository by lazy { ClearedRepository(database) }
    val connectivity: Connectivity by lazy { Connectivity(this) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            scope.launch { DevSeed.seedIfEmpty(database) }
        }

        // Drain whatever is queued the moment a connection appears, and again on every reconnect.
        // WorkManager owns the "when"; it survives the app being killed mid-backlog.
        scope.launch {
            connectivity.isOnline.collect { online ->
                if (online) SyncWorker.enqueue(this@ClearedApplication)
            }
        }
    }
}
