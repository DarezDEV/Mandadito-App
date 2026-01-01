package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable

/**
 * Modelo de Order (Pedido)
 * Representa una orden en el sistema
 */
@Serializable
data class Order(
    val id: String,
    val orderNumber: String,
    val userId: String,
    val colmadoId: String,
    val addressId: String,
    val deliveryUserId: String? = null,

    // Estado
    val status: OrderStatus,

    // Montos
    val subtotal: Double,
    val deliveryFee: Double,
    val platformFee: Double,
    val total: Double,

    // Notas
    val customerNotes: String? = null,
    val deliveryNotes: String? = null,
    val cancellationReason: String? = null,

    // Código de verificación para entrega
    val verificationCode: String? = null,

    // Timestamps
    val createdAt: String,
    val updatedAt: String,
    val paidAt: String? = null,
    val deliveredAt: String? = null,
    val cancelledAt: String? = null
)

/**
 * Estados posibles de una orden
 */
enum class OrderStatus {
    PENDING,            // Esperando pago
    PAYMENT_PROCESSING, // Procesando pago
    PAID,              // Pagado
    PREPARING,         // Preparando
    READY_FOR_PICKUP,  // Listo para recoger
    IN_DELIVERY,       // En camino
    DELIVERED,         // Entregado
    CANCELLED,         // Cancelado
    REFUNDED;          // Reembolsado

    companion object {
        fun fromString(value: String): OrderStatus {
            return when (value.lowercase()) {
                "pending" -> PENDING
                "payment_processing" -> PAYMENT_PROCESSING
                "paid" -> PAID
                "preparing" -> PREPARING
                "ready_for_pickup" -> READY_FOR_PICKUP
                "in_delivery" -> IN_DELIVERY
                "delivered" -> DELIVERED
                "cancelled" -> CANCELLED
                "refunded" -> REFUNDED
                else -> PENDING
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            PENDING -> "Pendiente"
            PAYMENT_PROCESSING -> "Procesando pago"
            PAID -> "Pagado"
            PREPARING -> "Preparando"
            READY_FOR_PICKUP -> "Listo para recoger"
            IN_DELIVERY -> "En camino"
            DELIVERED -> "Entregado"
            CANCELLED -> "Cancelado"
            REFUNDED -> "Reembolsado"
        }
    }
}

/**
 * Item de una orden
 */
@Serializable
data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val productPrice: Double,
    val productImageUrl: String? = null,
    val quantity: Int,
    val subtotal: Double
)

/**
 * Orden con detalles completos (incluye items)
 */
@Serializable
data class OrderWithDetails(
    val order: Order,
    val items: List<OrderItem>,
    val colmado: ColmadoInfo? = null
)

/**
 * Información del colmado en una orden
 */
@Serializable
data class ColmadoInfo(
    val id: String,
    val name: String,
    val address: String,
    val phone: String
)
