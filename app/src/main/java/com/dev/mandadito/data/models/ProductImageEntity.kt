package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductImageEntity(
    val id: String? = null,
    @SerialName("product_id")
    val productId: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("display_order")
    val displayOrder: Int = 0,
    @SerialName("is_primary")
    val isPrimary: Boolean? = false,
    @SerialName("created_at")
    val createdAt: String? = null
)