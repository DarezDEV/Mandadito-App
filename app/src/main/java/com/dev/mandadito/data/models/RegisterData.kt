package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterData(
    val email: String,
    val password: String,
    val nombre: String
)