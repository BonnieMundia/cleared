package app.cleared.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the device has a validated internet connection.
 *
 * Offline is a condition, not an error: nothing here throws, blocks or disables anything. It drives
 * a slim amber strip and nothing else.
 */
class Connectivity(context: Context) {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hasInternet())
            }

            override fun onLost(network: Network) {
                trySend(hasInternet())
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(hasInternet())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        trySend(hasInternet())
        manager?.registerNetworkCallback(request, callback)
        awaitClose { manager?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /**
     * `NET_CAPABILITY_VALIDATED` rather than merely connected: a captive portal at a café is not
     * the internet, and treating it as such would queue writes against a network that drops them.
     */
    fun hasInternet(): Boolean {
        val network = manager?.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
