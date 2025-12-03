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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ProductRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "ProductRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    @Serializable
    private data class CreateProductData(
        @SerialName("colmado_id")
        val colmadoId: String,
        val name: String,
        val description: String? = null,
        val price: Double,
        val stock: Int = 0,
        @SerialName("image_url")
        val imageUrl: String? = null,
        @SerialName("is_active")
        val isActive: Boolean = true
    )

    @Serializable
    private data class CreateProductCategoryData(
        @SerialName("product_id")
        val productId: String,
        @SerialName("category_id")
        val categoryId: String
    )

    @Serializable
    private data class UpdateProductData(
        val name: String,
        val description: String? = null,
        val price: Double,
        val stock: Int,
        @SerialName("image_url")
        val imageUrl: String? = null,
        @SerialName("is_active")
        val isActive: Boolean? = null
    )

    @Serializable
    private data class ProductCategoryData(
        @SerialName("product_id")
        val productId: String,
        @SerialName("category_id")
        val categoryId: String
    )

    @Serializable
    private data class ProductImageData(
        @SerialName("product_id")
        val productId: String,
        @SerialName("image_url")
        val imageUrl: String,
        @SerialName("display_order")
        val displayOrder: Int,
        @SerialName("is_primary")
        val isPrimary: Boolean = false
    )

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
    // CREAR PRODUCTO CON MÚLTIPLES IMÁGENES
    // ============================================
    suspend fun createProduct(
        colmadoId: String,
        name: String,
        description: String? = null,
        price: Double,
        stock: Int = 0,
        imageUris: List<Uri> = emptyList(),
        categoryIds: List<String> = emptyList()
    ): Result<ProductWithCategories> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creando producto: $name para colmado: $colmadoId")

            if (imageUris.isEmpty()) {
                return@withContext Result.Error("Debe agregar al menos 1 imagen")
            }
            if (imageUris.size > 5) {
                return@withContext Result.Error("Máximo 5 imágenes permitidas")
            }
            if (categoryIds.isEmpty()) {
                return@withContext Result.Error("Debe seleccionar al menos 1 categoría")
            }

            val productData = CreateProductData(
                colmadoId = colmadoId,
                name = name,
                description = description,
                price = price,
                stock = stock,
                isActive = true
            )

            val product = supabase.from("products")
                .insert(productData) {
                    select()
                }
                .decodeSingle<Product>()

            Log.d(TAG, "✅ Producto creado: ${product.id}")

            val imageUrls = mutableListOf<String>()
            imageUris.forEachIndexed { index, uri ->
                try {
                    val imageUrl = uploadProductImage(uri, product.id, index)
                    imageUrls.add(imageUrl)
                    Log.d(TAG, "✅ Imagen ${index + 1} subida")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo imagen ${index + 1}: ${e.message}")
                }
            }

            if (imageUrls.isNotEmpty()) {
                try {
                    val productImages = imageUrls.mapIndexed { index, url ->
                        ProductImageData(
                            productId = product.id,
                            imageUrl = url,
                            displayOrder = index,
                            isPrimary = index == 0
                        )
                    }
                    supabase.from("product_images")
                        .insert(productImages)
                    Log.d(TAG, "✅ ${imageUrls.size} imágenes guardadas en BD")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error guardando referencias de imágenes: ${e.message}")
                }
            }

            try {
                val productCategories = categoryIds.map { categoryId ->
                    ProductCategoryData(
                        productId = product.id,
                        categoryId = categoryId
                    )
                }

                supabase.from("product_categories")
                    .insert(productCategories)

                Log.d(TAG, "✅ ${categoryIds.size} categorías asignadas")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error asignando categorías: ${e.message}")
                deleteProduct(product.id)
                return@withContext Result.Error("Error al asignar categorías: ${e.message}")
            }

            return@withContext when (val result = getProductById(product.id)) {
                is Result.Success -> Result.Success(result.data)
                is Result.Error -> Result.Error(result.message)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando producto: ${e.message}", e)
            Result.Error("Error al crear producto: ${e.message}")
        }
    }

    // ============================================
    // ACTUALIZAR PRODUCTO CON MÚLTIPLES IMÁGENES
    // ============================================
    suspend fun updateProduct(
        productId: String,
        name: String,
        description: String? = null,
        price: Double,
        stock: Int,
        newImageUris: List<Uri> = emptyList(),
        existingImageUrls: List<String> = emptyList(),
        categoryIds: List<String> = emptyList(),
        isActive: Boolean? = null
    ): Result<ProductWithCategories> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Actualizando producto: $productId")

            val totalImages = existingImageUrls.size + newImageUris.size
            if (totalImages == 0) {
                return@withContext Result.Error("Debe tener al menos 1 imagen")
            }
            if (totalImages > 5) {
                return@withContext Result.Error("Máximo 5 imágenes permitidas")
            }
            if (categoryIds.isEmpty()) {
                return@withContext Result.Error("Debe seleccionar al menos 1 categoría")
            }

            val updateData = UpdateProductData(
                name = name,
                description = description,
                price = price,
                stock = stock,
                isActive = isActive
            )

            supabase.from("products")
                .update(updateData) {
                    filter { eq("id", productId) }
                }

            try {
                supabase.from("product_images")
                    .delete {
                        filter { eq("product_id", productId) }
                    }
                Log.d(TAG, "🗑️ Imágenes anteriores eliminadas")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error eliminando imágenes: ${e.message}")
            }

            val newImageUrls = mutableListOf<String>()
            newImageUris.forEachIndexed { index, uri ->
                try {
                    val imageUrl = uploadProductImage(uri, productId, existingImageUrls.size + index)
                    newImageUrls.add(imageUrl)
                    Log.d(TAG, "✅ Nueva imagen ${index + 1} subida")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo imagen: ${e.message}")
                }
            }

            val allImageUrls = existingImageUrls + newImageUrls
            if (allImageUrls.isNotEmpty()) {
                val productImages = allImageUrls.mapIndexed { index, url ->
                    ProductImageData(
                        productId = productId,
                        imageUrl = url,
                        displayOrder = index,
                        isPrimary = index == 0
                    )
                }
                supabase.from("product_images")
                    .insert(productImages)
                Log.d(TAG, "✅ ${allImageUrls.size} imágenes guardadas")
            }

            supabase.from("product_categories")
                .delete {
                    filter { eq("product_id", productId) }
                }

            val productCategories = categoryIds.map { categoryId ->
                ProductCategoryData(
                    productId = productId,
                    categoryId = categoryId
                )
            }

            supabase.from("product_categories")
                .insert(productCategories)

            Log.d(TAG, "✅ Categorías actualizadas")

            return@withContext when (val result = getProductById(productId)) {
                is Result.Success -> Result.Success(result.data)
                is Result.Error -> Result.Error(result.message)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando producto: ${e.message}", e)
            Result.Error("Error al actualizar producto: ${e.message}")
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
    private suspend fun uploadProductImage(
        imageUri: Uri,
        productId: String? = null,
        index: Int? = null
    ): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) {
            throw Exception("No se pudieron leer los bytes de la imagen")
        }

        val fileName = when {
            productId != null && index != null -> "$productId/image_$index.jpg"
            productId != null -> "$productId/product.jpg"
            else -> "temp/${System.currentTimeMillis()}.jpg"
        }

        supabase.storage.from("products")
            .upload(fileName, bytes, upsert = true)

        val publicUrl = "${AppConfig.SUPABASE_URL}/storage/v1/object/public/products/$fileName"
        return publicUrl
    }
}

