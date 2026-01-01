package com.dev.mandadito.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.models.Product
import com.dev.mandadito.data.models.ProductImage
import com.dev.mandadito.data.models.ProductWithCategories
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
        @SerialName("min_stock")
        val minStock: Int = 0,
        @SerialName("is_active")
        val isActive: Boolean = true
    )

    @Serializable
    private data class UpdateProductData(
        val name: String,
        val description: String? = null,
        val price: Double,
        val stock: Int,
        @SerialName("min_stock")
        val minStock: Int = 0,
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

    // Convertir Product a ProductWithCategories
    private suspend fun convertToProductWithCategories(product: Product): ProductWithCategories {
        // Obtener categorías
        val categoryIds = try {
            supabase.from("product_categories")
                .select()
                .decodeList<ProductCategoryData>()
                .filter { it.productId == product.id }
                .map { it.categoryId }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error obteniendo categorías: ${e.message}")
            emptyList()
        }

        val categories = if (categoryIds.isNotEmpty()) {
            try {
                supabase.from("categories")
                    .select()
                    .decodeList<Category>()
                    .filter { it.id in categoryIds }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error obteniendo datos de categorías: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        // Obtener imágenes y convertir a ProductImage
        val images = try {
            supabase.from("product_images")
                .select()
                .decodeList<ProductImageData>()
                .filter { it.productId == product.id }
                .sortedBy { it.displayOrder }
                .map { imageData ->
                    ProductImage(
                        id = null,
                        url = imageData.imageUrl,
                        order = imageData.displayOrder,
                        isPrimary = imageData.isPrimary
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error obteniendo imágenes: ${e.message}")
            emptyList()
        }

        return ProductWithCategories(
            id = product.id,
            colmadoId = product.colmadoId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            minStock = product.minStock,
            imageUrl = images.firstOrNull()?.url,
            images = images,
            isActive = product.isActive,
            categories = categories,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt
        )
    }

    suspend fun getAllProducts(): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📥 Obteniendo productos...")

                val products = supabase.from("products")
                    .select()
                    .decodeList<Product>()

                Log.d(TAG, "✅ ${products.size} productos base obtenidos")

                val productsWithCategories = products.map { convertToProductWithCategories(it) }

                Log.d(TAG, "✅ Productos convertidos con categorías e imágenes")
                Result.Success(productsWithCategories)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                Result.Error("Error al cargar productos: ${e.message}")
            }
        }

    suspend fun getActiveProducts(): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                val allProducts = when (val result = getAllProducts()) {
                    is Result.Success -> result.data
                    is Result.Error -> return@withContext result
                }
                Result.Success(allProducts.filter { it.isActive })
            } catch (e: Exception) {
                Result.Error("Error: ${e.message}")
            }
        }

    suspend fun getProductById(productId: String): Result<ProductWithCategories> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📥 Obteniendo producto: $productId")

                val product = supabase.from("products")
                    .select()
                    .decodeList<Product>()
                    .first { it.id == productId }

                val productWithCategories = convertToProductWithCategories(product)

                Log.d(TAG, "✅ Producto encontrado: ${productWithCategories.name}")
                Result.Success(productWithCategories)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                Result.Error("Error: ${e.message}")
            }
        }

    suspend fun getProductsByCategory(categoryId: String): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                val allProducts = when (val result = getAllProducts()) {
                    is Result.Success -> result.data
                    is Result.Error -> return@withContext result
                }
                Result.Success(allProducts.filter { it.categories.any { cat -> cat.id == categoryId } })
            } catch (e: Exception) {
                Result.Error("Error: ${e.message}")
            }
        }

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
            Log.d(TAG, "📝 Creando producto: $name")

            if (imageUris.isEmpty()) return@withContext Result.Error("Debe agregar al menos 1 imagen")
            if (categoryIds.isEmpty()) return@withContext Result.Error("Debe seleccionar al menos 1 categoría")

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
                .insert(productData) { select() }
                .decodeSingle<Product>()

            Log.d(TAG, "✅ Producto creado con ID: ${product.id}")

            // Subir imágenes
            val imageUrls = imageUris.mapIndexedNotNull { index, uri ->
                try {
                    val url = uploadProductImage(uri, product.id, index)
                    Log.d(TAG, "✅ Imagen ${index + 1} subida")
                    url
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo imagen ${index + 1}: ${e.message}")
                    null
                }
            }

            if (imageUrls.isNotEmpty()) {
                supabase.from("product_images").insert(
                    imageUrls.mapIndexed { index, url ->
                        ProductImageData(product.id, url, index, index == 0)
                    }
                )
                Log.d(TAG, "✅ ${imageUrls.size} imágenes guardadas en BD")
            }

            // Asignar categorías
            supabase.from("product_categories").insert(
                categoryIds.map { ProductCategoryData(product.id, it) }
            )
            Log.d(TAG, "✅ ${categoryIds.size} categorías asignadas")

            getProductById(product.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando producto: ${e.message}", e)
            Result.Error("Error al crear producto: ${e.message}")
        }
    }

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
            Log.d(TAG, "📝 Actualizando producto: $productId")

            supabase.from("products").update(
                UpdateProductData(name, description, price, stock, minStock, isActive)
            ) { filter { eq("id", productId) } }

            // Actualizar imágenes
            supabase.from("product_images").delete { filter { eq("product_id", productId) } }

            val newUrls = newImageUris.mapIndexedNotNull { index, uri ->
                try {
                    uploadProductImage(uri, productId, existingImageUrls.size + index)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo imagen: ${e.message}")
                    null
                }
            }

            val allUrls = existingImageUrls + newUrls
            if (allUrls.isNotEmpty()) {
                supabase.from("product_images").insert(
                    allUrls.mapIndexed { index, url ->
                        ProductImageData(productId, url, index, index == 0)
                    }
                )
            }

            // Actualizar categorías
            supabase.from("product_categories").delete { filter { eq("product_id", productId) } }
            supabase.from("product_categories").insert(
                categoryIds.map { ProductCategoryData(productId, it) }
            )

            Log.d(TAG, "✅ Producto actualizado")
            getProductById(productId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}", e)
            Result.Error("Error: ${e.message}")
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Eliminando producto: $productId")
            supabase.from("products").delete { filter { eq("id", productId) } }
            Log.d(TAG, "✅ Producto eliminado")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}", e)
            Result.Error("Error: ${e.message}")
        }
    }

    suspend fun searchProducts(query: String): Result<List<ProductWithCategories>> =
        withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) return@withContext getAllProducts()

                val allProducts = when (val result = getAllProducts()) {
                    is Result.Success -> result.data
                    is Result.Error -> return@withContext result
                }

                val searchLower = query.lowercase()
                Result.Success(
                    allProducts.filter {
                        it.name.lowercase().contains(searchLower) ||
                                it.description?.lowercase()?.contains(searchLower) == true ||
                                it.categories.any { cat -> cat.name.lowercase().contains(searchLower) }
                    }
                )
            } catch (e: Exception) {
                Result.Error("Error: ${e.message}")
            }
        }

    private suspend fun uploadProductImage(imageUri: Uri, productId: String, index: Int): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes() ?: throw Exception("No se pudieron leer los bytes")
        inputStream.close()

        val fileName = "$productId/image_$index.jpg"
        supabase.storage.from("products").upload(fileName, bytes, upsert = true)

        return "${AppConfig.SUPABASE_URL}/storage/v1/object/public/products/$fileName"
    }
}