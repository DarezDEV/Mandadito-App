// Categoria.kt (sin cambios mayores)
package com.dev.mandadito.data.models

data class Categoria(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    val icono: String? = null
)