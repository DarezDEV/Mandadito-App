package com.dev.mandadito.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.local.database.MandaditoDatabase
import com.dev.mandadito.data.local.entities.*
import com.dev.mandadito.data.models.Product
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ProductRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "ProductRepository"

    // NUEVOS: Room Database y ConnectivityMonitor
    private val database = MandaditoDatabase.getDatabase(context)
    private val productDao = database.productDao()
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
    private data class CreateProductData(
        @SerialName("colmado_id")
        val colmadoId: String,
        val name: String,
        val description: String? = null,
        val price: Double,
        val stock: Int = 0,
        @SerialName("min_stock")
        val minStock: Int = 0,
        @SerialName("image_urls")
        val imageUrls: List<String> = emptyList(),
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
        @SerialName("min_stock")
        val minStock: Int = 0,
        @SerialName("image_urls")
        val imageUrls: List<String>? = null,
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
    suspend fun getAllProducts(colmadoId: String? = null): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Obteniendo todos los productos${colmadoId?.let { " del colmado: $it" } ?: ""}...")

                val products = if (colmadoId != null) {
                    supabase.from("products_with_categories")
                        .select {
                            filter { eq("colmado_id", colmadoId) }
                        }
                        .decodeList<ProductWithCategories>()
                } else {
                    supabase.from("products_with_categories")
                        .select()
                        .decodeList<ProductWithCategories>()
                }

                Log.d(TAG, "✅ ${products.size} productos obtenidos")
                Result.Success(products)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo productos: ${e.message}", e)
                Result.Error("Error al cargar productos: ${e.message}")
            }
        }

    // ============================================
    // OBTENER PRODUCTOS ACTIVOS CON CACHÉ
    // ============================================
    suspend fun getActiveProducts(colmadoId: String? = null): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                if (colmadoId == null) {
                    return@withContext Result.Error("ID de colmado requerido")
                }

                val cacheKey = CacheStrategy.generateKey("products", colmadoId)
                val isConnected = connectivityMonitor.isCurrentlyConnected()

                // 1. Verificar si el caché es válido
                val cacheMetadata = cacheMetadataDao.getMetadata(cacheKey)
                val isCacheValid = CacheStrategy.isValid(cacheMetadata, CacheStrategy.PRODUCTS_TTL)

                Log.d(TAG, "🔍 Caché válido: $isCacheValid, Conectado: $isConnected")

                // 2. Si hay caché válido Y NO hay internet, retornar caché
                if (isCacheValid && !isConnected) {
                    Log.d(TAG, "📦 Retornando desde caché (sin conexión)")
                    val cachedProducts = loadFromCache(colmadoId)
                    return@withContext Result.Success(
                        data = cachedProducts,
                        isFromCache = true,
                        cacheTimestamp = cacheMetadata?.timestamp
                    )
                }

                // 3. Si HAY internet, intentar cargar de red
                if (isConnected) {
                    try {
                        Log.d(TAG, "🌐 Cargando desde red...")
                        val products = supabase.from("products_with_categories")
                            .select {
                                filter {
                                    eq("is_active", true)
                                    eq("colmado_id", colmadoId)
                                }
                            }
                            .decodeList<ProductWithCategories>()

                        Log.d(TAG, "✅ ${products.size} productos obtenidos de red")

                        // 4. Guardar en caché
                        saveToCache(colmadoId, products)

                        return@withContext Result.Success(
                            data = products,
                            isFromCache = false
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error de red: ${e.message}")

                        // Si falla la red pero hay caché (aunque expirado), usarlo
                        if (cacheMetadata != null) {
                            Log.d(TAG, "📦 Retornando caché expirado como fallback")
                            val cachedProducts = loadFromCache(colmadoId)
                            return@withContext Result.Success(
                                data = cachedProducts,
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
                Log.e(TAG, "❌ Error obteniendo productos: ${e.message}", e)
                Result.Error("Error al cargar productos: ${e.message}")
            }
        }

    /**
     * Guarda productos en caché local
     */
    private suspend fun saveToCache(colmadoId: String, products: List<ProductWithCategories>) {
        try {
            Log.d(TAG, "💾 Guardando ${products.size} productos en caché...")

            // 1. Guardar productos
            val productEntities = products.map { it.toEntity() }
            productDao.insertProducts(productEntities)

            // 2. Guardar categorías únicas
            val allCategories = products.flatMap { it.categories }.distinctBy { it.id }
            val categoryEntities = allCategories.map { it.toEntity() }
            categoryDao.insertCategories(categoryEntities)

            // 3. Guardar relaciones producto-categoría
            val crossRefs = products.flatMap { product ->
                product.categories.map { category ->
                    ProductCategoryCrossRef(
                        productId = product.id,
                        categoryId = category.id
                    )
                }
            }
            productDao.insertProductCategoryRefs(crossRefs)

            // 4. Actualizar metadata de caché
            val cacheKey = CacheStrategy.generateKey("products", colmadoId)
            cacheMetadataDao.insertMetadata(
                CacheMetadata(
                    key = cacheKey,
                    timestamp = System.currentTimeMillis(),
                    dataType = "products",
                    relatedId = colmadoId
                )
            )

            Log.d(TAG, "✅ Caché guardado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando caché: ${e.message}", e)
        }
    }

    /**
     * Carga productos desde caché local
     */
    private suspend fun loadFromCache(colmadoId: String): List<ProductWithCategories> {
        return try {
            val productsWithCategories = productDao.getActiveProducts(colmadoId)

            productsWithCategories.map { productWithCategoriesEntity ->
                productWithCategoriesEntity.product.toModel(
                    categories = productWithCategoriesEntity.categories.map { it.toModel() }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando desde caché: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Invalida el caché de productos
     */
    suspend fun invalidateCache(colmadoId: String) {
        withContext(Dispatchers.IO) {
            try {
                val cacheKey = CacheStrategy.generateKey("products", colmadoId)
                cacheMetadataDao.deleteMetadata(cacheKey)
                productDao.deleteProductsByColmado(colmadoId)
                Log.d(TAG, "🗑️ Caché invalidado")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error invalidando caché: ${e.message}")
            }
        }
    }

    // ============================================
    // OBTENER PRODUCTO POR ID
    // ============================================
    suspend fun getProductById(productId: String): Result<ProductWithCategories> =
        withContext(Dispatchers.IO) {
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
    suspend fun getProductsByCategory(categoryId: String, colmadoId: String? = null): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Obteniendo productos de categoría: $categoryId${colmadoId?.let { " del colmado: $it" } ?: ""}")

                // Obtener productos del colmado específico (si se proporciona) y filtrar por categoría
                val allProducts = when (val result = getAllProducts(colmadoId)) {
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
        minStock: Int = 0,
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
                minStock = minStock,
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
                    // Actualizar el producto con las URLs de las imágenes
                    supabase.from("products")
                        .update(mapOf("image_urls" to imageUrls)) {
                            filter { eq("id", product.id) }
                        }
                    Log.d(TAG, "✅ ${imageUrls.size} URLs de imágenes actualizadas en producto")

                    // También guardar en product_images para metadata adicional
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
                    Log.d(TAG, "✅ ${imageUrls.size} imágenes guardadas en product_images")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error guardando imágenes: ${e.message}")
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
        minStock: Int = 0,
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
                minStock = minStock,
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
                    val imageUrl =
                        uploadProductImage(uri, productId, existingImageUrls.size + index)
                    newImageUrls.add(imageUrl)
                    Log.d(TAG, "✅ Nueva imagen ${index + 1} subida")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo imagen: ${e.message}")
                }
            }

            val allImageUrls = existingImageUrls + newImageUrls
            if (allImageUrls.isNotEmpty()) {
                // Actualizar el campo image_urls en el producto
                supabase.from("products")
                    .update(mapOf("image_urls" to allImageUrls)) {
                        filter { eq("id", productId) }
                    }
                Log.d(TAG, "✅ ${allImageUrls.size} URLs actualizadas en el producto")

                // También guardar en product_images para metadata
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
                Log.d(TAG, "✅ ${allImageUrls.size} imágenes guardadas en product_images")
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
    suspend fun searchProducts(query: String, colmadoId: String? = null): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext getAllProducts(colmadoId)
                }

                Log.d(TAG, "Buscando productos: $query${colmadoId?.let { " en colmado: $it" } ?: ""}")

                val allProducts = when (val result = getAllProducts(colmadoId)) {
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