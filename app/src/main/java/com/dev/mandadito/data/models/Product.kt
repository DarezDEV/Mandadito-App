package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int = 0,
    @SerialName("min_stock")
    val minStock: Int = 0,
    @SerialName("colmado_id")
    val colmadoId: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    // Campos de reseñas
    @SerialName("average_rating")
    val averageRating: Double? = null,
    @SerialName("total_reviews")
    val totalReviews: Int = 0
)

/**
 * Extensión para obtener el rating formateado
 */
fun Product.getFormattedRating(): String {
    return if (averageRating != null && averageRating > 0) {
        String.format("%.1f", averageRating)
    } else {
        "Sin calificar"
    }
}

/**
 * Extensión para verificar si el producto tiene reseñas
 */
fun Product.hasReviews(): Boolean {
    return totalReviews > 0
}