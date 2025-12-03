package com.dev.mandadito.data.network

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.Category
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CategoryRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "CategoryRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
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
    suspend fun getAllCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo todas las categorías...")

            val categories = supabase.from("categories")
                .select()
                .decodeList<Category>()

            Log.d(TAG, "✅ ${categories.size} categorías obtenidas")
            Result.Success(categories)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo categorías: ${e.message}", e)
            Result.Error("Error al cargar categorías: ${e.message}")
        }
    }

    // ============================================
    // OBTENER CATEGORÍAS ACTIVAS
    // ============================================
    suspend fun getActiveCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo categorías activas...")

            val categories = supabase.from("categories")
                .select {
                    filter { eq("is_active", true) }
                }
                .decodeList<Category>()

            Log.d(TAG, "✅ ${categories.size} categorías activas obtenidas")
            Result.Success(categories)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo categorías activas: ${e.message}", e)
            Result.Error("Error al cargar categorías: ${e.message}")
        }
    }

    // ============================================
    // OBTENER CATEGORÍA POR ID
    // ============================================
    suspend fun getCategoryById(categoryId: String): Result<Category> = withContext(Dispatchers.IO) {
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
            kotlinx.coroutines.delay(200) // Delay para asegurar que el insert se complete
            
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
    suspend fun searchCategories(query: String): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext getAllCategories()
            }

            Log.d(TAG, "Buscando categorías: $query")

            val allCategories = when (val result = getAllCategories()) {
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

