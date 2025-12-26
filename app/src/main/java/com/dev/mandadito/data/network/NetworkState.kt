package com.dev.mandadito.data.network

/**
 * Estados de red posibles
 */
sealed class NetworkState {
    object Connected : NetworkState()
    object Disconnected : NetworkState()
    data class Retrying(val attempt: Int, val nextRetryInSeconds: Int) : NetworkState()
}
