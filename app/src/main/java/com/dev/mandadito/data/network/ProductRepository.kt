package com.dev.mandadito.data.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.models.Product
import com.dev.mandadito.data.models.ProductCategory
import com.dev.mandadito.data.models.ProductWithCategories
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "ProductRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    // ============================================
    // OBTENER TODOS LOS PRODUCTOS CON CATEGORÍAS
    // ============================================
    suspend fun getAllProducts(): Result<List<ProductWithCategories>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo todos los productos...")

            val products = supabase.from("products_with_categories")
                .select()
                .decodeList<ProductWithCategories>()

            Log.d(TAG, "✅ ${products.size} productos obtenidos")
            Result.Success(products)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo productos: ${e.message}", e)
            Result.Error("Error al cargar productos: ${e.message}")
        }
    }

    // ============================================
    // OBTENER PRODUCTOS ACTIVOS
    // ============================================
    suspend fun getActiveProducts(): Result<List<ProductWithCategories>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo productos activos...")

            val products = supabase.from("products_with_categories")
                .select {
                    filter { eq("is_active", true) }
                }
                .decodeList<ProductWithCategories>()

            Log.d(TAG, "✅ ${products.size} productos activos obtenidos")
            Result.Success(products)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo productos activos: ${e.message}", e)
            Result.Error("Error al cargar productos: ${e.message}")
        }
    }

    // ============================================
    // OBTENER PRODUCTO POR ID
    // ============================================
    suspend fun getProductById(productId: String): Result<ProductWithCategories> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo producto: $productId")

            val product = supabase.from("products_with_categories")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingle<ProductWithCategories>()

            Log.d(TAG, "✅ Producto encontrado: ${product.name}")
            Result.Success(product)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo producto: ${e.message}", e)
            Result.Error("Error al obtener producto: ${e.message}")
        }
    }

    // ============================================
    // OBTENER PRODUCTOS POR CATEGORÍA
    // ============================================
    suspend fun getProductsByCategory(categoryId: String): Result<List<ProductWithCategories>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo productos de categoría: $categoryId")

            // Obtener todos los productos y filtrar por categoría
            val allProducts = when (val result = getAllProducts()) {
                is Result.Success -> result.data
                is Result.Error -> return@withContext result
            }

            val filtered = allProducts.filter { product ->
                product.categories.any { it.id == categoryId }
            }

            Log.d(TAG, "✅ ${filtered.size} productos encontrados en la categoría")
            Result.Success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo productos por categoría: ${e.message}", e)
            Result.Error("Error al obtener productos: ${e.message}")
        }
    }

    // ============================================
    // CREAR PRODUCTO
    // ============================================
    suspend fun createProduct(
        name: String,
        description: String? = null,
        price: Double,
        stock: Int = 0,
        imageUri: Uri? = null,
        categoryIds: List<String> = emptyList()
    ): Result<ProductWithCategories> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creando producto: $name")

            // 1. Subir imagen si existe
            var imageUrl: String? = null
            if (imageUri != null) {
                try {
                    imageUrl = uploadProductImage(imageUri)
                    Log.d(TAG, "✅ Imagen subida: $imageUrl")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo imagen (continuando sin imagen): ${e.message}")
                }
            }

            // 2. Crear producto
            val productData = mapOf(
                "name" to name,
                "description" to description,
                "price" to price,
                "stock" to stock,
                "image_url" to imageUrl,
                "is_active" to true
            )

            val product = supabase.from("products")
                .insert(productData)
                .decodeSingle<Product>()

            Log.d(TAG, "✅ Producto creado: ${product.id}")

            // 3. Asignar categorías
            if (categoryIds.isNotEmpty()) {
                try {
                    val productCategories = categoryIds.map { categoryId ->
                        mapOf(
                            "product_id" to product.id,
                            "category_id" to categoryId
                        )
                    }

                    supabase.from("product_categories")
                        .insert(productCategories)

                    Log.d(TAG, "✅ ${categoryIds.size} categorías asignadas")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error asignando categorías: ${e.message}")
                }
            }

            // 4. Obtener producto completo con categorías
            val productWithCategories = when (val result = getProductById(product.id)) {
                is Result.Success -> result.data
                is Result.Error -> {
                    // Si falla, crear un ProductWithCategories básico
                    ProductWithCategories(
                        id = product.id,
                        name = product.name,
                        description = product.description,
                        price = product.price,
                        stock = product.stock,
                        imageUrl = product.imageUrl,
                        isActive = product.isActive,
                        createdAt = product.createdAt,
                        updatedAt = product.updatedAt,
                        categories = emptyList()
                    )
                }
            }

            Result.Success(productWithCategories)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando producto: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al crear producto: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    // ============================================
    // ACTUALIZAR PRODUCTO
    // ============================================
    suspend fun updateProduct(
        productId: String,
        name: String,
        description: String? = null,
        price: Double,
        stock: Int,
        imageUri: Uri? = null,
        categoryIds: List<String> = emptyList(),
        isActive: Boolean? = null
    ): Result<ProductWithCategories> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Actualizando producto: $productId")

            // 1. Subir nueva imagen si existe
            var imageUrl: String? = null
            if (imageUri != null) {
                try {
                    imageUrl = uploadProductImage(imageUri, productId)
                    Log.d(TAG, "✅ Imagen actualizada: $imageUrl")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo imagen: ${e.message}")
                    // Obtener la URL actual si existe
                    val currentProduct = when (val result = getProductById(productId)) {
                        is Result.Success -> result.data
                        is Result.Error -> null
                    }
                    imageUrl = currentProduct?.imageUrl
                }
            } else {
                // Mantener la imagen actual
                val currentProduct = when (val result = getProductById(productId)) {
                    is Result.Success -> result.data
                    is Result.Error -> null
                }
                imageUrl = currentProduct?.imageUrl
            }

            // 2. Actualizar producto
            val updateData = buildMap<String, Any?> {
                put("name", name)
                put("description", description)
                put("price", price)
                put("stock", stock)
                put("image_url", imageUrl)
                isActive?.let { put("is_active", it) }
            }

            supabase.from("products")
                .update(updateData) {
                    filter { eq("id", productId) }
                }

            // 3. Actualizar categorías
            if (categoryIds.isNotEmpty()) {
                try {
                    // Eliminar categorías existentes
                    supabase.from("product_categories")
                        .delete {
                            filter { eq("product_id", productId) }
                        }

                    // Insertar nuevas categorías
                    val productCategories = categoryIds.map { categoryId ->
                        mapOf(
                            "product_id" to productId,
                            "category_id" to categoryId
                        )
                    }

                    supabase.from("product_categories")
                        .insert(productCategories)

                    Log.d(TAG, "✅ Categorías actualizadas")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error actualizando categorías: ${e.message}")
                }
            }

            // 4. Obtener producto actualizado
            val productWithCategories = when (val result = getProductById(productId)) {
                is Result.Success -> result.data
                is Result.Error -> return@withContext result
            }

            Log.d(TAG, "✅ Producto actualizado: ${productWithCategories.name}")
            Result.Success(productWithCategories)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando producto: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al actualizar producto: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    // ============================================
    // ELIMINAR PRODUCTO
    // ============================================
    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Eliminando producto: $productId")

            supabase.from("products")
                .delete {
                    filter { eq("id", productId) }
                }

            Log.d(TAG, "✅ Producto eliminado")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando producto: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al eliminar producto: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    // ============================================
    // BUSCAR PRODUCTOS
    // ============================================
    suspend fun searchProducts(query: String): Result<List<ProductWithCategories>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext getAllProducts()
            }

            Log.d(TAG, "Buscando productos: $query")

            val allProducts = when (val result = getAllProducts()) {
                is Result.Success -> result.data
                is Result.Error -> return@withContext result
            }

            val searchLower = query.lowercase()
            val filtered = allProducts.filter { product ->
                product.name.lowercase().contains(searchLower) ||
                        product.description?.lowercase()?.contains(searchLower) == true ||
                        product.categories.any { it.name.lowercase().contains(searchLower) }
            }

            Log.d(TAG, "✅ ${filtered.size} productos encontrados")
            Result.Success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error buscando productos: ${e.message}", e)
            Result.Error("Error al buscar productos: ${e.message}")
        }
    }

    // ============================================
    // SUBIR IMAGEN DE PRODUCTO
    // ============================================
    private suspend fun uploadProductImage(imageUri: Uri, productId: String? = null): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) {
            throw Exception("No se pudieron leer los bytes de la imagen")
        }

        val fileName = if (productId != null) {
            "$productId/product.jpg"
        } else {
            "temp/${System.currentTimeMillis()}.jpg"
        }

        supabase.storage.from("products")
            .upload(fileName, bytes, upsert = true)

        val publicUrl = "${AppConfig.SUPABASE_URL}/storage/v1/object/public/products/$fileName"
        return publicUrl
    }
}

