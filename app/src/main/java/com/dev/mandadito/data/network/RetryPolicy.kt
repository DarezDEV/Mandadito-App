package com.dev.mandadito.data.network

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Política de reintentos exponencial con backoff
 * Similar a WhatsApp/Instagram
 */
object RetryPolicy {

    private const val TAG = "RetryPolicy"

    // Configuración de reintentos
    private const val INITIAL_DELAY_MS = 3000L      // 3 segundos
    private const val MAX_DELAY_MS = 60000L         // 60 segundos max
    private const val MULTIPLIER = 1.5              // Incremento exponencial

    /**
     * Ejecuta una operación con reintentos automáticos infinitos
     * Solo se detiene cuando:
     * 1. La operación tiene éxito
     * 2. Se detecta conexión de red y aún falla (error del servidor)
     *
     * @param isConnected Si hay conexión actual
     * @param operation La operación a ejecutar
     */
    suspend fun <T> retryWithBackoff(
        isConnected: Boolean,
        operation: suspend () -> T
    ): Flow<RetryState<T>> = flow {
        var attempt = 0
        var currentDelay = INITIAL_DELAY_MS

        while (true) {
            attempt++

            if (!isConnected) {
                Log.d(TAG, "🔄 Reintento #$attempt - Sin conexión, esperando ${currentDelay}ms")
                emit(RetryState.Retrying(attempt, (currentDelay / 1000).toInt()))
                delay(currentDelay)

                // Incremento exponencial con límite
                currentDelay = (currentDelay * MULTIPLIER).toLong().coerceAtMost(MAX_DELAY_MS)
                continue
            }

            try {
                Log.d(TAG, "🔥 Intento #$attempt - Ejecutando operación")
                emit(RetryState.Loading)
                val result = operation()
                Log.d(TAG, "✅ Operación exitosa en intento #$attempt")
                emit(RetryState.Success(result))
                break // Salir del loop

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en intento #$attempt: ${e.message}")

                // Si hay conexión pero falla, es error del servidor
                if (isConnected) {
                    emit(RetryState.Error(e.message ?: "Error del servidor"))
                    break // No reintentar errores del servidor
                }

                // Si no hay conexión, reintentar
                emit(RetryState.Retrying(attempt, (currentDelay / 1000).toInt()))
                delay(currentDelay)
                currentDelay = (currentDelay * MULTIPLIER).toLong().coerceAtMost(MAX_DELAY_MS)
            }
        }
    }
}

/**
 * Estados del proceso de reintento
 */
sealed class RetryState<out T> {
    object Loading : RetryState<Nothing>()
    data class Success<T>(val data: T) : RetryState<T>()
    data class Error(val message: String) : RetryState<Nothing>()
    data class Retrying(val attempt: Int, val nextRetryInSeconds: Int) : RetryState<Nothing>()
}
