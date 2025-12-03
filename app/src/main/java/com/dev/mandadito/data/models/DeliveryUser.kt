package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable

@Serializable
data class DeliveryUser(
    val id: String,
    val email: String,
    val nombre: String,
    val activo: Boolean = true,
    val avatar_url: String? = null,
    val colmado_id: String? = null,
    val role_in_colmado: String? = null
)