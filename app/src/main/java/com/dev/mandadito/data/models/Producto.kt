// Producto.kt
package com.dev.mandadito.data.models

data class Producto(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,  // Cambié a Double para consistencia
    val calificacion: Double? = null,
    val imageUrl: String? = null,
    val categoriaId: String? = null,  // Hice nullable para evitar errores de paso
    val colmadoId: String? = null,
    val stock: Int? = null,
    val descuento: Double? = null
)