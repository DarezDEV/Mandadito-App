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
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val categories: List<CategoryInfo> = emptyList()
)

@Serializable
data class CategoryInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null
)

