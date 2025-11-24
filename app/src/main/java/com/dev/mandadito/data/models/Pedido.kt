package com.dev.mandadito.data.models

data class Pedido(
    val id: String,
    val clienteId: String,
    val colmadoId: String,
    val items: List <ItemPedido>,
    val total: Double,
    val estado: String, // "pendiente", "confirmado", "en_camino", "entregado", "cancelado"
    val direccionEntrega: String,
    val fechaCreacion: String,
    val fechaActualizacion: String
)