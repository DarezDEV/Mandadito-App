package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductWithCategories(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int = 0,
    // Mantener imageUrl para compatibilidad (primera imagen)
    @SerialName("image_url")
    val imageUrl: String? = null,
    // Nuevo: array de imágenes
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
        get() = images.firstOrNull { it.isPrimary }?.url ?: images.firstOrNull()?.url ?: imageUrl

    val allImageUrls: List<String>
        get() = images.map { it.url }.ifEmpty { listOfNotNull(imageUrl) }
}

@Serializable
data class CategoryInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null
)

