package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.dev.mandadito.data.models.*

/**
 * Entidad local para Cart
 */
@Entity(
    tableName = "carts",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["colmado_id"])
    ]
)
data class CartEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "colmado_id")
    val colmadoId: String,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

/**
 * Entidad local para CartItem
 */
@Entity(
    tableName = "cart_items",
    indices = [
        Index(value = ["cart_id"]),
        Index(value = ["product_id"])
    ]
)
data class CartItemEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "cart_id")
    val cartId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    val quantity: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

/**
 * Entidad local para CartItemDetail (vista con información del producto)
 */
@Entity(
    tableName = "cart_item_details",
    indices = [
        Index(value = ["cart_id"]),
        Index(value = ["product_id"])
    ]
)
data class CartItemDetailEntity(
    @PrimaryKey
    @ColumnInfo(name = "cart_item_id")
    val cartItemId: String,

    @ColumnInfo(name = "cart_id")
    val cartId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "product_name")
    val productName: String,

    val price: Double,

    @ColumnInfo(name = "image_urls")
    val imageUrls: List<String>?,

    val quantity: Int,

    val total: Double
)

/**
 * Entidad local para CartSummary
 */
@Entity(
    tableName = "cart_summaries",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["colmado_id"])
    ]
)
data class CartSummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "cart_id")
    val cartId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "colmado_id")
    val colmadoId: String,

    @ColumnInfo(name = "colmado_name")
    val colmadoName: String,

    @ColumnInfo(name = "colmado_address")
    val colmadoAddress: String,

    @ColumnInfo(name = "colmado_description")
    val colmadoDescription: String?,

    @ColumnInfo(name = "total_products")
    val totalProducts: Int,

    val subtotal: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

/**
 * Relación para obtener el carrito completo con sus items
 */
data class CartWithItemsEntity(
    @Embedded val summary: CartSummaryEntity,
    @Relation(
        parentColumn = "cart_id",
        entityColumn = "cart_id"
    )
    val items: List<CartItemDetailEntity>
)

/**
 * Mappers: Entity ↔ Model
 */
fun CartEntity.toModel(): Cart {
    return Cart(
        id = id,
        userId = userId,
        colmadoId = colmadoId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Cart.toEntity(): CartEntity {
    return CartEntity(
        id = id,
        userId = userId,
        colmadoId = colmadoId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CartItemEntity.toModel(): CartItem {
    return CartItem(
        id = id,
        cartId = cartId,
        productId = productId,
        quantity = quantity,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CartItem.toEntity(): CartItemEntity {
    return CartItemEntity(
        id = id,
        cartId = cartId,
        productId = productId,
        quantity = quantity,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CartItemDetailEntity.toModel(): CartItemDetail {
    return CartItemDetail(
        cartItemId = cartItemId,
        cartId = cartId,
        productId = productId,
        productName = productName,
        price = price,
        imageUrls = imageUrls,
        quantity = quantity,
        total = total
    )
}

fun CartItemDetail.toEntity(): CartItemDetailEntity {
    return CartItemDetailEntity(
        cartItemId = cartItemId,
        cartId = cartId,
        productId = productId,
        productName = productName,
        price = price,
        imageUrls = imageUrls,
        quantity = quantity,
        total = total
    )
}

fun CartSummaryEntity.toModel(): CartSummary {
    return CartSummary(
        cartId = cartId,
        userId = userId,
        colmadoId = colmadoId,
        colmadoName = colmadoName,
        colmadoAddress = colmadoAddress,
        colmadoDescription = colmadoDescription,
        totalProducts = totalProducts,
        subtotal = subtotal,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CartSummary.toEntity(): CartSummaryEntity {
    return CartSummaryEntity(
        cartId = cartId,
        userId = userId,
        colmadoId = colmadoId,
        colmadoName = colmadoName,
        colmadoAddress = colmadoAddress,
        colmadoDescription = colmadoDescription,
        totalProducts = totalProducts,
        subtotal = subtotal,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CartWithItemsEntity.toModel(): CartWithItems {
    return CartWithItems(
        id = summary.cartId,
        summary = summary.toModel(),
        items = items.map { it.toModel() }
    )
}
