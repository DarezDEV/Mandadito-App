package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserColmado(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("colmado_id")
    val colmadoId: String,
    @SerialName("role_in_colmado")
    val roleInColmado: String, // "owner" o "delivery"
    @SerialName("created_at")
    val createdAt: String? = null
)