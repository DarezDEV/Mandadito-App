package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ColmadoWithOwner(
    val id: String,
    @SerialName("seller_id")
    val sellerId: String,
    val name: String,
    val address: String,
    val phone: String,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("owner_name")
    val ownerName: String? = null,
    @SerialName("owner_email")
    val ownerEmail: String? = null,
    @SerialName("owner_activo")
    val ownerActivo: Boolean? = null,
    @SerialName("total_users")
    val totalUsers: Int? = 0
)
