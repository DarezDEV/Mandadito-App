package com.dev.mandadito.presentation.viewmodels.common

/**
 * Estado unificado de UI para todas las pantallas
 * Reemplaza los estados individuales de cada ViewModel
 */
sealed class UiState<out T> {

    object Idle : UiState<Nothing>()

    object Loading : UiState<Nothing>()

    data class Success<T>(
        val data: T,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null
    ) : UiState<T>()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
        val canRetry: Boolean = true
    ) : UiState<Nothing>()

    data class Offline<T>(
        val cachedData: T? = null,
        val message: String = "Sin conexión. Mostrando datos guardados."
    ) : UiState<T>()

    data class Retrying(
        val attempt: Int,
        val nextRetryInSeconds: Int,
        val message: String = "Reintentando conexión..."
    ) : UiState<Nothing>()
}

/**
 * Extensiones útiles
 */
fun <T> UiState<T>.isLoading() = this is UiState.Loading
fun <T> UiState<T>.isSuccess() = this is UiState.Success
fun <T> UiState<T>.isError() = this is UiState.Error
fun <T> UiState<T>.isOffline() = this is UiState.Offline
fun <T> UiState<T>.isRetrying() = this is UiState.Retrying

fun <T> UiState<T>.dataOrNull(): T? = when (this) {
    is UiState.Success -> data
    is UiState.Offline -> cachedData
    else -> null
}
