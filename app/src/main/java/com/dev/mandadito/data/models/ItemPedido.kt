package com.dev.mandadito.data.models

data class ItemPedido(
    val productoId: String,
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)