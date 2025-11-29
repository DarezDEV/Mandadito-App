package com.dev.mandadito.data.network

import android.content.Context
import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class UserColmadoRecord(
    val id: String,
    val user_id: String,
    val colmado_id: String,
    val role_in_colmado: String
)

class SellerRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "SellerRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Obtiene el colmado_id del seller (dueño)
     * Primero busca en user_colmado, si no encuentra, busca en colmados usando seller_id
     */
    suspend fun getSellerColmadoId(userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo colmado del seller: $userId")

            // 1. Intentar obtener desde user_colmado
            val result = supabase.from("user_colmado")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("role_in_colmado", "owner")
                    }
                }
                .decodeSingleOrNull<UserColmadoRecord>()

            if (result != null) {
                Log.d(TAG, "✅ Colmado encontrado en user_colmado: ${result.colmado_id}")
                return@withContext Result.Success(result.colmado_id)
            }

            // 2. Fallback: buscar en colmados usando seller_id
            Log.d(TAG, "⚠️ No se encontró en user_colmado, buscando en colmados...")
            @Serializable
            data class ColmadoRecord(
                val id: String,
                val seller_id: String
            )

            val colmado = supabase.from("colmados")
                .select {
                    filter {
                        eq("seller_id", userId)
                    }
                }
                .decodeSingleOrNull<ColmadoRecord>()

            if (colmado != null) {
                Log.d(TAG, "✅ Colmado encontrado en colmados: ${colmado.id}")
                // Opcional: crear el registro en user_colmado para futuras consultas
                try {
                    supabase.from("user_colmado")
                        .insert(mapOf(
                            "user_id" to userId,
                            "colmado_id" to colmado.id,
                            "role_in_colmado" to "owner"
                        ))
                    Log.d(TAG, "✅ Registro creado en user_colmado")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ No se pudo crear registro en user_colmado: ${e.message}")
                }
                return@withContext Result.Success(colmado.id)
            }

            Log.w(TAG, "⚠️ No se encontró colmado para el seller en ninguna tabla")
            return@withContext Result.Error("No tienes un colmado asignado. Contacta al administrador.")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo colmado: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al obtener información del colmado: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    /**
     * Verifica si un seller tiene un colmado asignado
     */
    suspend fun hasColmado(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("user_colmado")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("role_in_colmado", "owner")
                    }
                }
                .decodeSingleOrNull<UserColmadoRecord>()

            result != null
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando colmado: ${e.message}")
            false
        }
    }
}