package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductCategory(
    @SerialName("product_id")
    val productId: String,
    @SerialName("category_id")
    val categoryId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

