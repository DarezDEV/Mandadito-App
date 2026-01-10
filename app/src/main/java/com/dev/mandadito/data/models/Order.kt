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
    val colmado: ColmadoInfo? = null,
    val deliveryAddress: Address? = null
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

/**
 * Registro de la vista orders_full (para mapeo desde Supabase)
 * Este modelo coincide exactamente con la estructura de la vista SQL
 */
@Serializable
data class OrdersFullViewRecord(
    val id: String,
    val order_number: String,
    val status: String,
    val subtotal: Double,
    val delivery_fee: Double,
    val platform_fee: Double,
    val total: Double,
    val created_at: String,
    val paid_at: String? = null,
    val delivered_at: String? = null,
    val user_id: String,
    val customer_name: String? = null,
    val customer_email: String? = null,
    val customer_phone: String? = null,
    val colmado_id: String,
    val colmado_name: String? = null,
    val colmado_address: String? = null,
    val colmado_phone: String? = null,
    val address_id: String? = null,
    val delivery_address: String? = null,
    val delivery_city: String? = null,
    val delivery_latitude: Double? = null,
    val delivery_longitude: Double? = null,
    val delivery_formatted_address: String? = null,
    val delivery_user_id: String? = null,
    val delivery_name: String? = null,
    val stripe_payment_intent_id: String? = null,
    val payment_status: String? = null,
    val card_brand: String? = null,
    val card_last4: String? = null
)
