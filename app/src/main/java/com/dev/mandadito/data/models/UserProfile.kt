package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val nombre: String,
    val role: Role? = null,
    val telefono: String?,
    val fotoUrl: String? = null,
    val activo: Boolean = true
)