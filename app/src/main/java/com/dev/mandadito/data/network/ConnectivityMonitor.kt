package com.dev.mandadito.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitorea el estado de conectividad de red en tiempo real
 * Usa NetworkCallback para reaccionar instantáneamente a cambios
 */
class ConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val TAG = "ConnectivityMonitor"

    /**
     * Flow que emite true cuando hay internet, false cuando no
     * Se actualiza automáticamente cuando cambia el estado de red
     */
    val isConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                networks.add(network)
                Log.d(TAG, "✅ Red disponible. Redes activas: ${networks.size}")
                trySend(true)
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                val hasConnection = networks.isNotEmpty()
                Log.d(TAG, "❌ Red perdida. Redes activas: ${networks.size}")
                trySend(hasConnection)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                if (hasInternet) {
                    networks.add(network)
                } else {
                    networks.remove(network)
                }

                trySend(networks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emitir estado inicial
        trySend(isCurrentlyConnected())

        awaitClose {
            Log.d(TAG, "Deteniendo monitoreo de conectividad")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() // Solo emitir cuando cambia el estado

    /**
     * Chequeo sincrónico del estado actual
     */
    fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
