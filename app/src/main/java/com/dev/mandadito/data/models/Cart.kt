package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo para la tabla carts
 */
@Serializable
data class Cart(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("colmado_id")
    val colmadoId: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * Modelo para la tabla cart_items
 */
@Serializable
data class CartItem(
    val id: String,
    @SerialName("cart_id")
    val cartId: String,
    @SerialName("product_id")
    val productId: String,
    val quantity: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * Modelo para la vista view_cart_summary
 */
@Serializable
data class CartSummary(
    @SerialName("cart_id")
    val cartId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("colmado_id")
    val colmadoId: String,
    @SerialName("colmado_name")
    val colmadoName: String,
    @SerialName("colmado_address")
    val colmadoAddress: String,
    @SerialName("colmado_description")
    val colmadoDescription: String? = null,
    @SerialName("total_products")
    val totalProducts: Int,
    val subtotal: Double,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * Modelo para la vista view_cart_items
 */
@Serializable
data class CartItemDetail(
    @SerialName("cart_item_id")
    val cartItemId: String,
    @SerialName("cart_id")
    val cartId: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("product_name")
    val productName: String,
    val price: Double,
    @SerialName("image_urls")
    val imageUrls: List<String>? = null,
    val quantity: Int,
    val total: Double
)

/**
 * Modelo combinado para mostrar en la UI
 * Representa un carrito completo con sus productos
 */
data class CartWithItems(
    val summary: CartSummary,
    val items: List<CartItemDetail>
) {
    val total: Double
        get() = subtotal + deliveryFee - discount

    val subtotal: Double
        get() = summary.subtotal

    val deliveryFee: Double = 0.0 // Puede ser dinámico en el futuro

    val discount: Double = 0.0 // Puede ser dinámico en el futuro
}
