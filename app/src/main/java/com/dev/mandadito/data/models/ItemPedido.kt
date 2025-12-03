package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemPedido(
    val id: String,
    @SerialName("pedido_id")
    val pedidoId: String,
    @SerialName("producto_id")
    val productoId: String,
    val nombre: String,
    val cantidad: Int,
    @SerialName("precio_unitario")
    val precioUnitario: Double,
    val subtotal: Double
)