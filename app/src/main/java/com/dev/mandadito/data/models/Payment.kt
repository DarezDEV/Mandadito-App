package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable

/**
 * Modelo de Payment (Pago)
 */
@Serializable
data class Payment(
    val id: String,
    val orderId: String,
    val userId: String,
    val colmadoId: String,

    // Stripe IDs
    val stripePaymentIntentId: String,
    val stripeChargeId: String? = null,
    val stripeTransferId: String? = null,

    // Montos (en centavos)
    val amount: Int,
    val amountCaptured: Int,
    val platformFeeAmount: Int,
    val transferAmount: Int,

    // Moneda
    val currency: String,

    // Estado
    val status: PaymentStatus,

    // Método de pago
    val paymentMethodType: String? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,

    // Errores
    val errorCode: String? = null,
    val errorMessage: String? = null,

    // Client secret (para PaymentSheet)
    val clientSecret: String? = null,
    val receiptUrl: String? = null,

    // Timestamps
    val createdAt: String,
    val updatedAt: String,
    val succeededAt: String? = null,
    val failedAt: String? = null
)

/**
 * Estados posibles de un pago
 */
enum class PaymentStatus {
    PENDING,           // Esperando
    PROCESSING,        // Procesando
    REQUIRES_ACTION,   // Requiere acción (3D Secure)
    SUCCEEDED,         // Exitoso
    FAILED,            // Falló
    CANCELLED,         // Cancelado
    REFUNDED;          // Reembolsado

    companion object {
        fun fromString(value: String): PaymentStatus {
            return when (value.lowercase()) {
                "pending" -> PENDING
                "processing" -> PROCESSING
                "requires_action" -> REQUIRES_ACTION
                "succeeded" -> SUCCEEDED
                "failed" -> FAILED
                "cancelled" -> CANCELLED
                "refunded" -> REFUNDED
                else -> PENDING
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            PENDING -> "Pendiente"
            PROCESSING -> "Procesando"
            REQUIRES_ACTION -> "Requiere acción"
            SUCCEEDED -> "Exitoso"
            FAILED -> "Falló"
            CANCELLED -> "Cancelado"
            REFUNDED -> "Reembolsado"
        }
    }
}

/**
 * Response al crear una orden
 */
@Serializable
data class CreateOrderResponse(
    val success: Boolean,
    val orderId: String? = null,
    val orderNumber: String? = null,
    val clientSecret: String? = null,
    val amount: Double? = null,
    val message: String? = null
)

/**
 * Response de configuración de Stripe
 */
@Serializable
data class StripeConfigResponse(
    val publishableKey: String,
    val currency: String
)

/**
 * Request para crear una orden
 */
@Serializable
data class CreateOrderRequest(
    val userId: String,
    val cartId: String,
    val addressId: String,
    val deliveryFee: Double,
    val customerNotes: String? = null
)
