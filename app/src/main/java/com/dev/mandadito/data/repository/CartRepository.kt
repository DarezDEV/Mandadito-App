package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.CartItemDetail
import com.dev.mandadito.data.models.CartSummary
import com.dev.mandadito.data.models.CartWithItems
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CartRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "CartRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Obtiene todos los carritos del usuario actual con sus items
     */
    suspend fun getUserCarts(): Result<List<CartWithItems>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.Error("Usuario no autenticado")

            Log.d(TAG, "🛒 Obteniendo carritos para usuario: $userId")

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

            Result.Success(cartsWithItems)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo carritos: ${e.message}", e)
            Result.Error("Error al cargar el carrito: ${e.message}")
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

                supabase.postgrest.rpc("update_cart_quantity", buildJsonObject {
                    put("item_uuid", itemId)
                    put("new_qty", quantity)
                })

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

            supabase.postgrest.rpc("remove_from_cart", buildJsonObject {
                put("item_uuid", itemId)
            })

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

            supabase.from("cart_items")
                .delete {
                    filter { eq("cart_id", cartId) }
                }

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
}
