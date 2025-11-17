package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable


@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val nombre: String,
    val activo: Boolean = true,
    val avatar_url: String? = null, // ✨ NUEVO CAMPO
    val role: Role? = null
)