package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.local.database.MandaditoDatabase
import com.dev.mandadito.data.local.entities.*
import com.dev.mandadito.data.models.ColmadoWithOwner
import com.dev.mandadito.data.models.UpdateColmadoDto
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ColmadosRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val TAG = "ColmadosRepository"

    // Room Database para caché persistente
    private val database = MandaditoDatabase.getDatabase(context)
    private val colmadoDao = database.colmadoDao()
    private val cacheMetadataDao = database.cacheMetadataDao()

    companion object {
        private const val CACHE_TTL = 15 * 60 * 1000L // 15 minutos
    }

    sealed class Result<out T> {
        data class Success<T>(
            val data: T,
            val isFromCache: Boolean = false,
            val cacheTimestamp: Long? = null
        ) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

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
     * Obtener colmados activos con caché persistente
     */
    suspend fun getActiveColmados(): Result<List<ColmadoWithOwner>> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = CacheStrategy.generateKey("colmados", "active")
            val isConnected = connectivityMonitor.isCurrentlyConnected()

            // 1. Verificar si el caché es válido
            val cacheMetadata = cacheMetadataDao.getMetadata(cacheKey)
            val isCacheValid = CacheStrategy.isValid(cacheMetadata, CACHE_TTL)

            Log.d(TAG, "🔍 Caché válido: $isCacheValid, Conectado: $isConnected")

            // 2. Si hay caché válido Y NO hay internet, retornar caché
            if (isCacheValid && !isConnected) {
                Log.d(TAG, "📦 Retornando colmados desde caché (sin conexión)")
                val cachedColmados = loadFromCache(true)
                return@withContext Result.Success(
                    data = cachedColmados,
                    isFromCache = true,
                    cacheTimestamp = cacheMetadata?.timestamp
                )
            }

            // 3. Si HAY internet, intentar cargar de red
            if (isConnected) {
                try {
                    Log.d(TAG, "🌐 Obteniendo colmados desde servidor...")
                    val colmados = supabase.from("colmados_with_owner")
                        .select {
                            filter {
                                eq("is_active", true)
                            }
                        }
                        .decodeList<ColmadoWithOwner>()

                    Log.d(TAG, "✅ ${colmados.size} colmados obtenidos de red")

                    // 4. Guardar en caché persistente
                    saveToCache(colmados, true)

                    return@withContext Result.Success(
                        data = colmados,
                        isFromCache = false
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error de red: ${e.message}")

                    // Si falla la red pero hay caché (aunque expirado), usarlo
                    if (cacheMetadata != null) {
                        Log.d(TAG, "📦 Retornando caché expirado como fallback")
                        val cachedColmados = loadFromCache(true)
                        return@withContext Result.Success(
                            data = cachedColmados,
                            isFromCache = true,
                            cacheTimestamp = cacheMetadata.timestamp
                        )
                    }

                    throw e
                }
            }

            // 4. Si NO hay internet y NO hay caché, error
            Log.w(TAG, "⚠️ Sin conexión y sin caché")
            return@withContext Result.Error("Sin conexión a internet. No hay datos guardados.")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener colmados: ${e.message}", e)
            Result.Error("Error al cargar colmados: ${e.message}")
        }
    }

    /**
     * Guarda colmados en caché local
     */
    private suspend fun saveToCache(colmados: List<ColmadoWithOwner>, activeOnly: Boolean) {
        try {
            Log.d(TAG, "💾 Guardando ${colmados.size} colmados en caché...")

            // 1. Limpiar caché anterior si es para activos
            if (activeOnly) {
                // Solo eliminar activos para no perder inactivos
                colmadoDao.getActiveColmados().forEach { colmadoDao.deleteColmado(it.id) }
            }

            // 2. Guardar colmados
            val colmadoEntities = colmados.map { it.toEntity() }
            colmadoDao.insertColmados(colmadoEntities)

            // 3. Actualizar metadata de caché
            val cacheKey = CacheStrategy.generateKey("colmados", if (activeOnly) "active" else "all")
            cacheMetadataDao.insertMetadata(
                CacheMetadata(
                    key = cacheKey,
                    timestamp = System.currentTimeMillis(),
                    dataType = "colmados",
                    relatedId = if (activeOnly) "active" else "all"
                )
            )

            Log.d(TAG, "✅ Caché guardado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando en caché: ${e.message}", e)
        }
    }

    /**
     * Carga colmados desde caché local
     */
    private suspend fun loadFromCache(activeOnly: Boolean): List<ColmadoWithOwner> {
        return try {
            Log.d(TAG, "📂 Cargando colmados desde caché local...")

            val entities = if (activeOnly) {
                colmadoDao.getActiveColmados()
            } else {
                colmadoDao.getAllColmados()
            }

            val result = entities.map { it.toColmadoWithOwner() }

            Log.d(TAG, "✅ ${result.size} colmados cargados desde caché")
            result

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando desde caché: ${e.message}", e)
            emptyList()
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
    suspend fun deactivateColmado(colmadoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Desactivando colmado: $colmadoId")

            supabase.from("colmados")
                .update(UpdateColmadoDto(isActive = false)) {
                    filter {
                        eq("id", colmadoId)
                    }
                }

            // Invalidar caché para forzar recarga
            invalidateCache()

            Log.d(TAG, "✅ Colmado desactivado: $colmadoId")
            return@withContext Result.Success(Unit)

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
    suspend fun activateColmado(colmadoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Activando colmado: $colmadoId")

            supabase.from("colmados")
                .update(UpdateColmadoDto(isActive = true)) {
                    filter {
                        eq("id", colmadoId)
                    }
                }

            // Invalidar caché para forzar recarga
            invalidateCache()

            Log.d(TAG, "✅ Colmado activado: $colmadoId")
            return@withContext Result.Success(Unit)

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
    suspend fun deleteColmado(colmadoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Eliminando colmado: $colmadoId")

            supabase.from("colmados")
                .delete {
                    filter {
                        eq("id", colmadoId)
                    }
                }

            // Invalidar caché para forzar recarga
            invalidateCache()

            Log.d(TAG, "Colmado eliminado: $colmadoId")
            return@withContext Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar colmado: ${e.message}", e)
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
    suspend fun searchColmados(query: String): List<ColmadoWithOwner> =
        withContext(Dispatchers.IO) {
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

    /**
     * Invalida el caché para forzar recarga
     */
    private suspend fun invalidateCache() {
        try {
            val activeKey = CacheStrategy.generateKey("colmados", "active")
            val allKey = CacheStrategy.generateKey("colmados", "all")
            cacheMetadataDao.deleteMetadata(activeKey)
            cacheMetadataDao.deleteMetadata(allKey)
            Log.d(TAG, "🗑️ Caché invalidado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error invalidando caché: ${e.message}")
        }
    }
}