package com.dev.mandadito.data.local.dao

import androidx.room.*
import com.dev.mandadito.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    // ============ Cart ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCart(cart: CartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarts(carts: List<CartEntity>)

    @Query("SELECT * FROM carts WHERE id = :cartId")
    suspend fun getCartById(cartId: String): CartEntity?

    @Query("SELECT * FROM carts WHERE user_id = :userId AND colmado_id = :colmadoId")
    suspend fun getCartByUserAndColmado(userId: String, colmadoId: String): CartEntity?

    @Query("SELECT * FROM carts WHERE user_id = :userId")
    suspend fun getCartsByUser(userId: String): List<CartEntity>

    @Query("DELETE FROM carts WHERE id = :cartId")
    suspend fun deleteCart(cartId: String)

    @Query("DELETE FROM carts WHERE user_id = :userId")
    suspend fun deleteCartsByUser(userId: String)

    @Query("DELETE FROM carts")
    suspend fun clearAllCarts()

    // ============ CartItem ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItems(items: List<CartItemEntity>)

    @Query("SELECT * FROM cart_items WHERE id = :itemId")
    suspend fun getCartItemById(itemId: String): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE cart_id = :cartId")
    suspend fun getCartItemsByCartId(cartId: String): List<CartItemEntity>

    @Query("DELETE FROM cart_items WHERE id = :itemId")
    suspend fun deleteCartItem(itemId: String)

    @Query("DELETE FROM cart_items WHERE cart_id = :cartId")
    suspend fun deleteCartItemsByCartId(cartId: String)

    @Query("DELETE FROM cart_items WHERE product_id = :productId")
    suspend fun deleteCartItemsByProductId(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearAllCartItems()

    // ============ CartItemDetail ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItemDetail(detail: CartItemDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItemDetails(details: List<CartItemDetailEntity>)

    @Query("SELECT * FROM cart_item_details WHERE cart_id = :cartId")
    suspend fun getCartItemDetailsByCartId(cartId: String): List<CartItemDetailEntity>

    @Query("DELETE FROM cart_item_details WHERE cart_id = :cartId")
    suspend fun deleteCartItemDetailsByCartId(cartId: String)

    @Query("DELETE FROM cart_item_details")
    suspend fun clearAllCartItemDetails()

    // ============ CartSummary ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartSummary(summary: CartSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartSummaries(summaries: List<CartSummaryEntity>)

    @Query("SELECT * FROM cart_summaries WHERE cart_id = :cartId")
    suspend fun getCartSummaryByCartId(cartId: String): CartSummaryEntity?

    @Query("SELECT * FROM cart_summaries WHERE user_id = :userId")
    suspend fun getCartSummariesByUser(userId: String): List<CartSummaryEntity>

    @Query("DELETE FROM cart_summaries WHERE cart_id = :cartId")
    suspend fun deleteCartSummary(cartId: String)

    @Query("DELETE FROM cart_summaries WHERE user_id = :userId")
    suspend fun deleteCartSummariesByUser(userId: String)

    @Query("DELETE FROM cart_summaries")
    suspend fun clearAllCartSummaries()

    // ============ CartWithItems (JOIN) ============

    @Transaction
    @Query("SELECT * FROM cart_summaries WHERE cart_id = :cartId")
    suspend fun getCartWithItems(cartId: String): CartWithItemsEntity?

    @Transaction
    @Query("SELECT * FROM cart_summaries WHERE user_id = :userId")
    suspend fun getCartsWithItemsByUser(userId: String): List<CartWithItemsEntity>

    /**
     * Observar cambios en el carrito en tiempo real
     */
    @Transaction
    @Query("SELECT * FROM cart_summaries WHERE cart_id = :cartId")
    fun observeCartWithItems(cartId: String): Flow<CartWithItemsEntity?>

    /**
     * Transacción para eliminar un carrito completo
     */
    @Transaction
    suspend fun deleteCartCompletely(cartId: String) {
        deleteCartItemDetailsByCartId(cartId)
        deleteCartItemsByCartId(cartId)
        deleteCartSummary(cartId)
        deleteCart(cartId)
    }

    /**
     * Transacción para limpiar todo el caché de carritos
     */
    @Transaction
    suspend fun clearAllCartCache() {
        clearAllCartItemDetails()
        clearAllCartItems()
        clearAllCartSummaries()
        clearAllCarts()
    }
}
