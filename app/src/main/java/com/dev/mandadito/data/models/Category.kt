package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("colmado_id")
    val colmadoId: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)