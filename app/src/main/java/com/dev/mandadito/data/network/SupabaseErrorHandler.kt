package com.dev.mandadito.data.network

import android.util.Log
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.network.sockets.*
import java.net.UnknownHostException

/**
 * Diferencia entre errores de red vs errores del servidor
 */
object SupabaseErrorHandler {

    private const val TAG = "SupabaseErrorHandler"

    /**
     * Determina si un error es de conectividad de red
     */
    fun isNetworkError(exception: Throwable): Boolean {
        val isNetwork = when (exception) {
            is UnknownHostException -> true
            is ConnectTimeoutException -> true
            is SocketTimeoutException -> true
            is HttpRequestException -> {
                // Supabase wraps network errors in HttpRequestException
                exception.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                exception.message?.contains("timeout", ignoreCase = true) == true ||
                exception.message?.contains("network", ignoreCase = true) == true
            }
            else -> false
        }

        Log.d(TAG, "Error es de red: $isNetwork - ${exception.message}")
        return isNetwork
    }

    /**
     * Obtiene un mensaje de error amigable
     */
    fun getUserFriendlyMessage(exception: Throwable): String {
        return when {
            isNetworkError(exception) -> "Sin conexión a internet"

            exception is HttpRequestException -> {
                // Analizar el mensaje para determinar el tipo de error
                val message = exception.message ?: ""
                when {
                    message.contains("401") -> "No autorizado. Inicia sesión nuevamente"
                    message.contains("403") -> "Acceso denegado"
                    message.contains("404") -> "Recurso no encontrado"
                    message.contains("500") || message.contains("502") || message.contains("503") ->
                        "Error del servidor. Intenta más tarde"
                    else -> "Error: ${exception.message}"
                }
            }

            else -> exception.message ?: "Error desconocido"
        }
    }
}
