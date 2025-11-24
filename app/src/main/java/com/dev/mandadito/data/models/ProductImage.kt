package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductImage(
    val id: String? = null,
    val url: String,
    val order: Int = 0,
    @SerialName("is_primary")
    val isPrimary: Boolean = false
)