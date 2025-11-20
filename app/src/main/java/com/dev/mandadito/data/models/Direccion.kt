package com.dev.mandadito.data.models

data class Direccion(
    val id: String,
    val usuarioId: String,
    val calle: String,
    val numero: String,
    val ciudad: String,
    val provincia: String,
    val codigoPostal: String? = null,
    val referencia: String? = null,
    val esPrincipal: Boolean = false
)