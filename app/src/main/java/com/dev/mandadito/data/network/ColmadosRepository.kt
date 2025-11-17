package com.dev.mandadito.data.network

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.Colmado
import com.dev.mandadito.data.models.ColmadoWithOwner
import com.dev.mandadito.data.models.UpdateColmadoDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ColmadosRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "ColmadosRepository"

    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Obtener todos los colmados con información del dueño
     */
    suspend fun getAllColmados(): List<ColmadoWithOwner> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo todos los colmados...")

            val colmados = supabase.from("colmados_with_owner")
                .select()
                .decodeList<ColmadoWithOwner>()

            Log.d(TAG, "✅ Colmados obtenidos: ${colmados.size}")
            return@withContext colmados

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener colmados: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * Obtener colmados activos
     */
    suspend fun getActiveColmados(): List<ColmadoWithOwner> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo colmados activos...")

            val colmados = supabase.from("colmados_with_owner")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<ColmadoWithOwner>()

            Log.d(TAG, "✅ Colmados activos: ${colmados.size}")
            return@withContext colmados

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener colmados activos: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * Obtener colmados inactivos
     */
    suspend fun getInactiveColmados(): List<ColmadoWithOwner> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo colmados inactivos...")

            val colmados = supabase.from("colmados_with_owner")
                .select {
                    filter {
                        eq("is_active", false)
                    }
                }
                .decodeList<ColmadoWithOwner>()

            Log.d(TAG, "✅ Colmados inactivos: ${colmados.size}")
            return@withContext colmados

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener colmados inactivos: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * Desactivar un colmado (también desactiva usuarios relacionados vía trigger)
     */
    suspend fun deactivateColmado(colmadoId: String): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Desactivando colmado: $colmadoId")

            supabase.from("colmados")
                .update(UpdateColmadoDto(isActive = false)) {
                    filter {
                        eq("id", colmadoId)
                    }
                }

            Log.d(TAG, "✅ Colmado desactivado: $colmadoId")
            return@withContext Result.Success

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al desactivar colmado: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                e.message?.contains("not found", ignoreCase = true) == true ->
                    "Colmado no encontrado"
                else -> "Error al desactivar el colmado"
            }
            return@withContext Result.Error(errorMessage)
        }
    }

    /**
     * Activar un colmado (también activa usuarios relacionados vía trigger)
     */
    suspend fun activateColmado(colmadoId: String): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Activando colmado: $colmadoId")

            supabase.from("colmados")
                .update(UpdateColmadoDto(isActive = true)) {
                    filter {
                        eq("id", colmadoId)
                    }
                }

            Log.d(TAG, "✅ Colmado activado: $colmadoId")
            return@withContext Result.Success

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al activar colmado: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                e.message?.contains("not found", ignoreCase = true) == true ->
                    "Colmado no encontrado"
                else -> "Error al activar el colmado"
            }
            return@withContext Result.Error(errorMessage)
        }
    }

    /**
     * Eliminar un colmado (también elimina usuarios relacionados vía trigger)
     */
    suspend fun deleteColmado(colmadoId: String): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Eliminando colmado: $colmadoId")

            supabase.from("colmados")
                .delete {
                    filter {
                        eq("id", colmadoId)
                    }
                }

            Log.d(TAG, "✅ Colmado eliminado: $colmadoId")
            return@withContext Result.Success

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al eliminar colmado: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                e.message?.contains("not found", ignoreCase = true) == true ->
                    "Colmado no encontrado"
                e.message?.contains("foreign key", ignoreCase = true) == true ->
                    "No se puede eliminar: tiene datos relacionados"
                else -> "Error al eliminar el colmado"
            }
            return@withContext Result.Error(errorMessage)
        }
    }

    /**
     * Obtener un colmado por ID
     */
    suspend fun getColmadoById(colmadoId: String): ColmadoWithOwner? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo colmado: $colmadoId")

            val colmado = supabase.from("colmados_with_owner")
                .select {
                    filter {
                        eq("id", colmadoId)
                    }
                }
                .decodeSingleOrNull<ColmadoWithOwner>()

            Log.d(TAG, if (colmado != null) "✅ Colmado encontrado" else "⚠️ Colmado no encontrado")
            return@withContext colmado

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener colmado: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Buscar colmados por nombre, dirección o teléfono
     */
    suspend fun searchColmados(query: String): List<ColmadoWithOwner> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext getAllColmados()
            }

            Log.d(TAG, "Buscando colmados: $query")

            // Obtener todos y filtrar localmente para búsqueda más flexible
            val allColmados = getAllColmados()
            val searchLower = query.lowercase()

            val filtered = allColmados.filter { colmado ->
                colmado.name.lowercase().contains(searchLower) ||
                        colmado.address.lowercase().contains(searchLower) ||
                        colmado.phone.contains(query) ||
                        colmado.ownerName?.lowercase()?.contains(searchLower) == true ||
                        colmado.ownerEmail?.lowercase()?.contains(searchLower) == true
            }

            Log.d(TAG, "✅ Colmados encontrados: ${filtered.size}")
            return@withContext filtered

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al buscar colmados: ${e.message}", e)
            return@withContext emptyList()
        }
    }
}