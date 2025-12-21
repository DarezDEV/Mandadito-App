package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductWithCategories(
    val id: String,
    @SerialName("colmado_id")
    val colmadoId: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int = 0,
    @SerialName("min_stock")
    val minStock: Int = 0,
    // Array de URLs de imágenes de la base de datos
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    // Mantener imageUrl para compatibilidad con código antiguo
    @SerialName("image_url")
    val imageUrl: String? = null,
    // Lista de imágenes con metadata (de product_images)
    val images: List<ProductImage> = emptyList(),
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val categories: List<Category> = emptyList()
) {
    // Helpers para acceder a las imágenes
    val primaryImage: String?
        get() = images.firstOrNull { it.isPrimary }?.url
            ?: images.firstOrNull()?.url
            ?: imageUrls.firstOrNull()
            ?: imageUrl

    val allImageUrls: List<String>
        get() = when {
            images.isNotEmpty() -> images.map { it.url }
            imageUrls.isNotEmpty() -> imageUrls
            imageUrl != null -> listOf(imageUrl)
            else -> emptyList()
        }
}

@Serializable
data class CategoryInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null
)

