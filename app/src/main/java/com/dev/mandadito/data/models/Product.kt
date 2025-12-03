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
    @SerialName("colmado_id")  // ⬅️ SOLO AGREGA ESTO
    val colmadoId: String,      // ⬅️ Y ESTO
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)