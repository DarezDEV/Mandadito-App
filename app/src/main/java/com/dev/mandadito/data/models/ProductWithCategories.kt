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
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    val categories: List<Category> = emptyList()
) {
    // Helper para obtener la primera imagen
    val primaryImage: String?
        get() = imageUrls.firstOrNull() ?: imageUrl

    // Helper para obtener todas las URLs
    val allImageUrls: List<String>
        get() = if (imageUrls.isNotEmpty()) imageUrls else listOfNotNull(imageUrl)
}