package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Notificacion(
    val id: Int,
    val titulo: String,
    val mensaje: String,
    val tiempo: String,
    val leida: Boolean = false
)