package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.local.database.MandaditoDatabase
import com.dev.mandadito.data.local.entities.*
import com.dev.mandadito.data.models.CartItemDetail
import com.dev.mandadito.data.models.CartSummary
import com.dev.mandadito.data.models.CartWithItems
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CartRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val TAG = "CartRepository"

    // Room Database para caché persistente
    private val database = MandaditoDatabase.getDatabase(context)
    private val cartDao = database.cartDao()
    private val cacheMetadataDao = database.cacheMetadataDao()

    companion object {
        private const val CACHE_TTL = 5 * 60 * 1000L // 5 minutos (carritos cambian con frecuencia)
    }

    sealed class Result<out T> {
        data class Success<T>(
            val data: T,
            val isFromCache: Boolean = false,
            val cacheTimestamp: Long? = null
        ) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Obtiene todos los carritos del usuario actual con sus items (con soporte offline)
     */
    suspend fun getUserCarts(): Result<List<CartWithItems>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.Error("Usuario no autenticado")

            val cacheKey = CacheStrategy.generateKey("carts", userId)
            val isConnected = connectivityMonitor.isCurrentlyConnected()

            // 1. Verificar si el caché es válido
            val cacheMetadata = cacheMetadataDao.getMetadata(cacheKey)
            val isCacheValid = CacheStrategy.isValid(cacheMetadata, CACHE_TTL)

            Log.d(TAG, "🔍 Caché válido: $isCacheValid, Conectado: $isConnected")

            // 2. Si hay caché válido Y NO hay internet, retornar caché
            if (isCacheValid && !isConnected) {
                Log.d(TAG, "📦 Retornando carritos desde caché (sin conexión)")
                val cachedCarts = loadFromCache(userId)
                return@withContext Result.Success(
                    data = cachedCarts,
                    isFromCache = true,
                    cacheTimestamp = cacheMetadata?.timestamp
                )
            }

            // 3. Si HAY internet, intentar cargar de red
            if (isConnected) {
                try {
                    Log.d(TAG, "🌐 Obteniendo carritos desde servidor para usuario: $userId")

                    // Obtener resúmenes de carritos
                    val summaries = supabase.from("view_cart_summary")
                        .select {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<CartSummary>()

                    Log.d(TAG, "✅ ${summaries.size} carritos encontrados")

                    // Para cada carrito, obtener sus items
                    val cartsWithItems = summaries.map { summary ->
                        val items = supabase.from("view_cart_items")
                            .select {
                                filter { eq("cart_id", summary.cartId) }
                            }
                            .decodeList<CartItemDetail>()

                        Log.d(TAG, "📦 Carrito ${summary.colmadoName}: ${items.size} productos")

                        CartWithItems(
                            summary = summary,
                            items = items
                        )
                    }

                    // 4. Guardar en caché persistente
                    saveToCache(userId, cartsWithItems)

                    return@withContext Result.Success(
                        data = cartsWithItems,
                        isFromCache = false
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error de red: ${e.message}")

                    // Si falla la red pero hay caché (aunque expirado), usarlo
                    if (cacheMetadata != null) {
                        Log.d(TAG, "📦 Retornando caché expirado como fallback")
                        val cachedCarts = loadFromCache(userId)
                        return@withContext Result.Success(
                            data = cachedCarts,
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
            Log.e(TAG, "❌ Error obteniendo carritos: ${e.message}", e)
            Result.Error("Error al cargar el carrito: ${e.message}")
        }
    }

    /**
     * Guarda carritos en caché local
     */
    private suspend fun saveToCache(userId: String, carts: List<CartWithItems>) {
        try {
            Log.d(TAG, "💾 Guardando ${carts.size} carritos en caché...")

            // 1. Limpiar caché anterior del usuario
            cartDao.deleteCartSummariesByUser(userId)
            cartDao.deleteCartItemDetailsByCartId("") // Se limpiará por cascada

            // 2. Guardar cada carrito con sus items
            carts.forEach { cart ->
                // Guardar summary
                cartDao.insertCartSummary(cart.summary.toEntity())

                // Guardar items
                val itemEntities = cart.items.map { it.toEntity() }
                cartDao.insertCartItemDetails(itemEntities)
            }

            // 3. Actualizar metadata de caché
            val cacheKey = CacheStrategy.generateKey("carts", userId)
            cacheMetadataDao.insertMetadata(
                CacheMetadata(
                    key = cacheKey,
                    timestamp = System.currentTimeMillis(),
                    dataType = "carts",
                    relatedId = userId
                )
            )

            Log.d(TAG, "✅ Caché guardado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando en caché: ${e.message}", e)
        }
    }

    /**
     * Carga carritos desde caché local
     */
    private suspend fun loadFromCache(userId: String): List<CartWithItems> {
        return try {
            Log.d(TAG, "📂 Cargando carritos desde caché local...")

            val cartsWithItems = cartDao.getCartsWithItemsByUser(userId)
            val result = cartsWithItems.map { it.toModel() }

            Log.d(TAG, "✅ ${result.size} carritos cargados desde caché")
            result

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando desde caché: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Agrega un producto al carrito
     * Usa la función de PostgreSQL add_to_cart
     */
    suspend fun addToCart(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.Error("Usuario no autenticado")

            Log.d(TAG, "➕ Agregando producto $productId al carrito")

            // Llamar a la función RPC de Supabase
            supabase.postgrest.rpc("add_to_cart", buildJsonObject {
                put("product_uuid", productId)
                put("user_uuid", userId)
            })

            // Invalidar caché para forzar recarga
            invalidateCache(userId)

            Log.d(TAG, "✅ Producto agregado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error agregando al carrito: ${e.message}", e)
            Result.Error("Error al agregar producto: ${e.message}")
        }
    }

    /**
     * Actualiza la cantidad de un item en el carrito
     */
    suspend fun updateCartItemQuantity(itemId: String, quantity: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Actualizando cantidad del item $itemId a $quantity")

                val userId = getCurrentUserId()

                supabase.postgrest.rpc("update_cart_quantity", buildJsonObject {
                    put("item_uuid", itemId)
                    put("new_qty", quantity)
                })

                // Invalidar caché para forzar recarga
                userId?.let { invalidateCache(it) }

                Log.d(TAG, "✅ Cantidad actualizada")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error actualizando cantidad: ${e.message}", e)
                Result.Error("Error al actualizar cantidad: ${e.message}")
            }
        }

    /**
     * Elimina un item del carrito
     */
    suspend fun removeFromCart(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Eliminando item $itemId del carrito")

            val userId = getCurrentUserId()

            supabase.postgrest.rpc("remove_from_cart", buildJsonObject {
                put("item_uuid", itemId)
            })

            // Invalidar caché para forzar recarga
            userId?.let { invalidateCache(it) }

            Log.d(TAG, "✅ Item eliminado")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando del carrito: ${e.message}", e)
            Result.Error("Error al eliminar producto: ${e.message}")
        }
    }

    /**
     * Vacía un carrito completo (elimina todos los items)
     */
    suspend fun clearCart(cartId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Vaciando carrito $cartId")

            val userId = getCurrentUserId()

            supabase.from("cart_items")
                .delete {
                    filter { eq("cart_id", cartId) }
                }

            // Invalidar caché para forzar recarga
            userId?.let { invalidateCache(it) }

            Log.d(TAG, "✅ Carrito vaciado")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error vaciando carrito: ${e.message}", e)
            Result.Error("Error al vaciar carrito: ${e.message}")
        }
    }

    /**
     * Obtiene el ID del usuario actual
     */
    private suspend fun getCurrentUserId(): String? {
        return try {
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo usuario actual: ${e.message}")
            null
        }
    }

    /**
     * Incrementa la cantidad de un producto en 1
     */
    suspend fun incrementQuantity(itemId: String, currentQuantity: Int): Result<Unit> {
        return updateCartItemQuantity(itemId, currentQuantity + 1)
    }

    /**
     * Decrementa la cantidad de un producto en 1
     * Si llega a 0, elimina el item
     */
    suspend fun decrementQuantity(itemId: String, currentQuantity: Int): Result<Unit> {
        return if (currentQuantity <= 1) {
            removeFromCart(itemId)
        } else {
            updateCartItemQuantity(itemId, currentQuantity - 1)
        }
    }

    /**
     * Invalida el caché para forzar recarga
     */
    private suspend fun invalidateCache(userId: String) {
        try {
            val cacheKey = CacheStrategy.generateKey("carts", userId)
            cacheMetadataDao.deleteMetadata(cacheKey)
            Log.d(TAG, "🗑️ Caché invalidado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error invalidando caché: ${e.message}")
        }
    }
}
