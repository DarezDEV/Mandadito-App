package com.dev.mandadito.data.models

import kotlinx.serialization.Serializable

@Serializable
data class FinancialSummary(
    val totalRevenue: Double = 0.0,
    val totalPlatformFees: Double = 0.0,
    val totalTransfers: Double = 0.0,
    val totalOrders: Int = 0,
    val successfulPayments: Int = 0,
    val failedPayments: Int = 0,
    val pendingPayments: Int = 0,
    val averageOrderValue: Double = 0.0,
    val activeColmados: Int = 0
)

@Serializable
data class DailyRevenue(
    val date: String,
    val revenue: Double,
    val platformFees: Double = 0.0,
    val orders: Int = 0,
    val successfulPayments: Int = 0,
    val failedPayments: Int = 0
)

@Serializable
data class PaymentRecord(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val colmadoId: String,
    val colmadoName: String,
    val stripePaymentIntentId: String,
    val stripeChargeId: String? = null,
    val amount: Double,
    val platformFeeAmount: Double,
    val transferAmount: Double,
    val currency: String,
    val status: PaymentStatusRecord,
    val paymentMethodType: String? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val createdAt: String,
    val succeededAt: String? = null,
    val failedAt: String? = null
)

enum class PaymentStatusRecord {
    PENDING,
    PROCESSING,
    REQUIRES_ACTION,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED;

    fun toDisplayString(): String = when (this) {
        PENDING -> "Pendiente"
        PROCESSING -> "Procesando"
        REQUIRES_ACTION -> "Requiere Acción"
        SUCCEEDED -> "Exitoso"
        FAILED -> "Fallido"
        CANCELLED -> "Cancelado"
        REFUNDED -> "Reembolsado"
    }

    fun isSuccessful(): Boolean = this == SUCCEEDED
    fun isFailed(): Boolean = this == FAILED
    fun isPending(): Boolean = this in listOf(PENDING, PROCESSING, REQUIRES_ACTION)

    companion object {
        fun fromString(value: String): PaymentStatusRecord {
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
}

@Serializable
data class RevenueByColmado(
    val colmadoId: String,
    val colmadoName: String,
    val totalRevenue: Double,
    val platformFees: Double = 0.0,
    val netRevenue: Double = 0.0,
    val orderCount: Int = 0,
    val successfulPayments: Int = 0,
    val failedPayments: Int = 0
)

@Serializable
data class FinanceMetrics(
    val summary: FinancialSummary,
    val dailyRevenue: List<DailyRevenue>,
    val revenueByColmado: List<RevenueByColmado>,
    val recentPayments: List<PaymentRecord>
)

@Serializable
data class DateRangeFilter(
    val startDate: String,
    val endDate: String,
    val period: DatePeriod
)

enum class DatePeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR,
    CUSTOM
}

@Serializable
data class PaymentFilter(
    val status: PaymentStatusRecord? = null,
    val colmadoId: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)
