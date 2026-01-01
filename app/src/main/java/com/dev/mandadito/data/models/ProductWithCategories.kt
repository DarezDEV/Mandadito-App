package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductWithCategories(
    val id: String,
    @SerialName("colmado_id")
    val colmadoId: String,  // ← AGREGAR ESTA LÍNEA
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int = 0,
    @SerialName("min_stock")
    val minStock: Int = 0,
    @SerialName("image_url")
    val imageUrl: String? = null,
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