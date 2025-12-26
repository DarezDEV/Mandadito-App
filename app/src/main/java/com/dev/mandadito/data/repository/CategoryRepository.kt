package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.local.database.MandaditoDatabase
import com.dev.mandadito.data.local.entities.*
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CategoryRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "CategoryRepository"

    // Room Database y ConnectivityMonitor
    private val database = MandaditoDatabase.getDatabase(context)
    private val categoryDao = database.categoryDao()
    private val cacheMetadataDao = database.cacheMetadataDao()
    private val connectivityMonitor = ConnectivityMonitor(context)

    sealed class Result<out T> {
        data class Success<T>(
            val data: T,
            val isFromCache: Boolean = false,
            val cacheTimestamp: Long? = null
        ) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    @Serializable
    private data class CreateCategoryData(
        @SerialName("colmado_id")
        val colmadoId: String,
        val name: String,
        val description: String? = null,
        val icon: String? = null,
        val color: String? = null,
        @SerialName("is_active")
        val isActive: Boolean = true
    )

    @Serializable
    private data class UpdateCategoryData(
        val name: String,
        val description: String? = null,
        val icon: String? = null,
        val color: String? = null,
        @SerialName("is_active")
        val isActive: Boolean? = null
    )

    // ============================================
    // OBTENER TODAS LAS CATEGORÍAS
    // ============================================
    suspend fun getAllCategories(colmadoId: String? = null): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo todas las categorías${colmadoId?.let { " del colmado: $it" } ?: ""}...")

            val categories = if (colmadoId != null) {
                supabase.from("categories")
                    .select {
                        filter { eq("colmado_id", colmadoId) }
                    }
                    .decodeList<Category>()
            } else {
                supabase.from("categories")
                    .select()
                    .decodeList<Category>()
            }

            Log.d(TAG, "✅ ${categories.size} categorías obtenidas")
            Result.Success(categories)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo categorías: ${e.message}", e)
            Result.Error("Error al cargar categorías: ${e.message}")
        }
    }

    // ============================================
    // OBTENER CATEGORÍAS ACTIVAS CON CACHÉ
    // ============================================
    suspend fun getActiveCategories(colmadoId: String? = null): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            if (colmadoId == null) {
                return@withContext Result.Error("ID de colmado requerido")
            }

            val cacheKey = CacheStrategy.generateKey("categories", colmadoId)
            val isConnected = connectivityMonitor.isCurrentlyConnected()

            // 1. Verificar caché válido
            val cacheMetadata = cacheMetadataDao.getMetadata(cacheKey)
            val isCacheValid = CacheStrategy.isValid(cacheMetadata, CacheStrategy.CATEGORIES_TTL)

            Log.d(TAG, "🔍 Caché válido: $isCacheValid, Conectado: $isConnected")

            // 2. Si hay caché válido Y NO hay internet, retornar caché
            if (isCacheValid && !isConnected) {
                Log.d(TAG, "📦 Retornando desde caché (sin conexión)")
                val cachedCategories = loadFromCache(colmadoId)
                return@withContext Result.Success(
                    data = cachedCategories,
                    isFromCache = true,
                    cacheTimestamp = cacheMetadata?.timestamp
                )
            }

            // 3. Si HAY internet, intentar cargar de red
            if (isConnected) {
                try {
                    Log.d(TAG, "🌐 Cargando desde red...")
                    val categories = supabase.from("categories")
                        .select {
                            filter {
                                eq("is_active", true)
                                eq("colmado_id", colmadoId)
                            }
                        }
                        .decodeList<Category>()

                    Log.d(TAG, "✅ ${categories.size} categorías obtenidas de red")

                    // 4. Guardar en caché
                    saveToCache(colmadoId, categories)

                    return@withContext Result.Success(
                        data = categories,
                        isFromCache = false
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error de red: ${e.message}")

                    // Si falla la red pero hay caché, usarlo
                    if (cacheMetadata != null) {
                        Log.d(TAG, "📦 Retornando caché expirado como fallback")
                        val cachedCategories = loadFromCache(colmadoId)
                        return@withContext Result.Success(
                            data = cachedCategories,
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
            Log.e(TAG, "❌ Error obteniendo categorías: ${e.message}", e)
            Result.Error("Error al cargar categorías: ${e.message}")
        }
    }

    /**
     * Guarda categorías en caché local
     */
    private suspend fun saveToCache(colmadoId: String, categories: List<Category>) {
        try {
            Log.d(TAG, "💾 Guardando ${categories.size} categorías en caché...")

            val categoryEntities = categories.map { it.toEntity() }
            categoryDao.insertCategories(categoryEntities)

            val cacheKey = CacheStrategy.generateKey("categories", colmadoId)
            cacheMetadataDao.insertMetadata(
                CacheMetadata(
                    key = cacheKey,
                    timestamp = System.currentTimeMillis(),
                    dataType = "categories",
                    relatedId = colmadoId
                )
            )

            Log.d(TAG, "✅ Caché guardado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando caché: ${e.message}", e)
        }
    }

    /**
     * Carga categorías desde caché local
     */
    private suspend fun loadFromCache(colmadoId: String): List<Category> {
        return try {
            val categoryEntities = categoryDao.getActiveCategories(colmadoId)
            categoryEntities.map { it.toModel() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando desde caché: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Invalida el caché de categorías
     */
    suspend fun invalidateCache(colmadoId: String) {
        withContext(Dispatchers.IO) {
            try {
                val cacheKey = CacheStrategy.generateKey("categories", colmadoId)
                cacheMetadataDao.deleteMetadata(cacheKey)
                categoryDao.deleteCategoriesByColmado(colmadoId)
                Log.d(TAG, "🗑️ Caché invalidado")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error invalidando caché: ${e.message}")
            }
        }
    }

    // ============================================
    // OBTENER CATEGORÍA POR ID
    // ============================================
    suspend fun getCategoryById(categoryId: String): Result<Category> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Obteniendo categoría: $categoryId")

                val category = supabase.from("categories")
                    .select {
                        filter { eq("id", categoryId) }
                    }
                    .decodeSingle<Category>()

                Log.d(TAG, "✅ Categoría encontrada: ${category.name}")
                Result.Success(category)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo categoría: ${e.message}", e)
                Result.Error("Error al obtener categoría: ${e.message}")
            }
        }

    // ============================================
    // CREAR CATEGORÍA
    // ============================================
    suspend fun createCategory(
        colmadoId: String,
        name: String,
        description: String? = null,
        icon: String? = null,
        color: String? = null
    ): Result<Category> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creando categoría: $name para colmado: $colmadoId")

            val categoryData = CreateCategoryData(
                colmadoId = colmadoId,
                name = name,
                description = description,
                icon = icon,
                color = color,
                isActive = true
            )

            // Insertar la categoría
            supabase.from("categories")
                .insert(categoryData)

            Log.d(TAG, "✅ Categoría insertada exitosamente")

            // Obtener la categoría recién creada inmediatamente
            delay(200) // Delay para asegurar que el insert se complete

            // Obtener la categoría por nombre y colmado_id (la más reciente)
            val categories = supabase.from("categories")
                .select {
                    filter {
                        eq("name", name)
                        eq("colmado_id", colmadoId)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeList<Category>()

            val category = categories.firstOrNull()
                ?: throw Exception("No se pudo encontrar la categoría recién creada")

            Log.d(TAG, "✅ Categoría obtenida: ${category.id}")

            Result.Success(category)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando categoría: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("duplicate", ignoreCase = true) == true ||
                        e.message?.contains("unique", ignoreCase = true) == true ->
                    "Ya existe una categoría con ese nombre"

                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                else -> "Error al crear categoría: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    // ============================================
    // ACTUALIZAR CATEGORÍA
    // ============================================
    suspend fun updateCategory(
        categoryId: String,
        name: String,
        description: String? = null,
        icon: String? = null,
        color: String? = null,
        isActive: Boolean? = null
    ): Result<Category> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Actualizando categoría: $categoryId")

            val updateData = UpdateCategoryData(
                name = name,
                description = description,
                icon = icon,
                color = color,
                isActive = isActive
            )

            supabase.from("categories")
                .update(updateData) {
                    filter { eq("id", categoryId) }
                }

            // Obtener la categoría actualizada
            val category = supabase.from("categories")
                .select {
                    filter { eq("id", categoryId) }
                }
                .decodeSingle<Category>()

            Log.d(TAG, "✅ Categoría actualizada: ${category.name}")
            Result.Success(category)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando categoría: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("duplicate", ignoreCase = true) == true ||
                        e.message?.contains("unique", ignoreCase = true) == true ->
                    "Ya existe una categoría con ese nombre"

                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                else -> "Error al actualizar categoría: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    // ============================================
    // ELIMINAR CATEGORÍA
    // ============================================
    suspend fun deleteCategory(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Eliminando categoría: $categoryId")

            supabase.from("categories")
                .delete {
                    filter { eq("id", categoryId) }
                }

            Log.d(TAG, "✅ Categoría eliminada")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando categoría: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("foreign key", ignoreCase = true) == true ||
                        e.message?.contains("constraint", ignoreCase = true) == true ->
                    "No se puede eliminar: tiene productos asociados"

                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                else -> "Error al eliminar categoría: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    // ============================================
    // BUSCAR CATEGORÍAS
    // ============================================
    suspend fun searchCategories(query: String, colmadoId: String? = null): Result<List<Category>> =
        withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext getAllCategories(colmadoId)
                }

                Log.d(TAG, "Buscando categorías: $query${colmadoId?.let { " en colmado: $it" } ?: ""}")

                val allCategories = when (val result = getAllCategories(colmadoId)) {
                    is Result.Success -> result.data
                    is Result.Error -> return@withContext result
                }

                val searchLower = query.lowercase()
                val filtered = allCategories.filter { category ->
                    category.name.lowercase().contains(searchLower) ||
                            category.description?.lowercase()?.contains(searchLower) == true
                }

                Log.d(TAG, "✅ ${filtered.size} categorías encontradas")
                Result.Success(filtered)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error buscando categorías: ${e.message}", e)
                Result.Error("Error al buscar categorías: ${e.message}")
            }
        }
}