package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pedido(
    val id: String,
    @SerialName("cliente_id")
    val clienteId: String,
    @SerialName("colmado_id")
    val colmadoId: String,
    val total: Double,
    val estado: String,
    @SerialName("direccion_entrega")
    val direccionEntrega: String,
    @SerialName("telefono_contacto")
    val telefonoContacto: String? = null,
    val notas: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)