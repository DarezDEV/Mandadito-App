package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FinanceReportResponse(
    val success: Boolean,
    val message: String? = null,
    val summary: FinanceSummaryData? = null,
    val dailyRevenue: List<DailyRevenueData>? = null,
    val revenueByColmado: List<RevenueByColmadoData>? = null,
    val recentPayments: List<PaymentData>? = null,
    val debugOrders: List<DebugOrderData>? = null
)

@Serializable
data class DebugOrderData(
    val id: String,
    val order_number: String,
    val status: String,
    val total: Double,
    val platform_fee: Double,
    val created_at: String,
    val colmado_name: String
)

@Serializable
data class FinanceSummaryData(
    val totalRevenue: Double,
    val totalPlatformFees: Double,
    val totalTransfers: Double,
    val totalOrders: Int,
    val successfulPayments: Int,
    val failedPayments: Int,
    val pendingPayments: Int,
    val averageOrderValue: Double,
    val activeColmados: Int
)

@Serializable
data class DailyRevenueData(
    val date: String,
    val revenue: Double,
    val platformFees: Double,
    val orders: Int,
    val successfulPayments: Int,
    val failedPayments: Int
)

@Serializable
data class RevenueByColmadoData(
    val colmadoId: String,
    val colmadoName: String,
    val totalRevenue: Double,
    val platformFees: Double,
    val netRevenue: Double,
    val orderCount: Int,
    val successfulPayments: Int,
    val failedPayments: Int
)

@Serializable
data class PaymentData(
    val id: String,
    val order_id: String,
    val order_number: String,
    val stripe_payment_intent_id: String,
    val stripe_charge_id: String?,
    val stripe_transfer_id: String?,
    val amount: Int,
    val amount_captured: Int?,
    val platform_fee_amount: Int,
    val transfer_amount: Int,
    val currency: String,
    val status: String,
    val payment_method_type: String?,
    val card_brand: String?,
    val card_last4: String?,
    val error_code: String?,
    val error_message: String?,
    val created_at: String,
    val succeeded_at: String?,
    val failed_at: String?,
    val colmado_id: String,
    val colmado_name: String
)

class FinanceRepository(private val context: Context) {

    private val TAG = "FinanceRepository"

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    private val currencyDivider = 100.0

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private fun convertCentsToDollars(cents: Int): Double = cents / currencyDivider

    private suspend fun getFinanceReport(): FinanceReportResponse = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClient.client
            val session = supabase.auth.currentSessionOrNull()
            val accessToken = session?.accessToken ?: throw Exception("No hay sesion activa")

            Log.d(TAG, "Llamando a finance-report...")
            Log.d(TAG, "URL: ${AppConfig.SUPABASE_URL}/functions/v1/finance-report")

            val response = httpClient.post("${AppConfig.SUPABASE_URL}/functions/v1/finance-report") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                    append("apikey", AppConfig.SUPABASE_ANON_KEY)
                }
                contentType(ContentType.Application.Json)
            }

            val jsonStr = response.body<String>()
            Log.d(TAG, "Response: $jsonStr")

            val result: FinanceReportResponse = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString(jsonStr)
            
            if (!result.success) {
                throw Exception(result.message ?: "Error desconocido")
            }

            // Log debug info
            result.debugOrders?.forEach { order ->
                Log.d(TAG, "Orden: ${order.order_number}, estado=${order.status}, total=${order.total}, colmado=${order.colmado_name}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error en getFinanceReport: ${e.message}", e)
            throw e
        }
    }

    suspend fun getFinancialSummary(): Result<FinancialSummary> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo resumen financiero...")
            val report = getFinanceReport()
            
            val summary = FinancialSummary(
                totalRevenue = report.summary?.totalRevenue ?: 0.0,
                totalPlatformFees = report.summary?.totalPlatformFees ?: 0.0,
                totalTransfers = report.summary?.totalTransfers ?: 0.0,
                totalOrders = report.summary?.totalOrders ?: 0,
                successfulPayments = report.summary?.successfulPayments ?: 0,
                failedPayments = report.summary?.failedPayments ?: 0,
                pendingPayments = report.summary?.pendingPayments ?: 0,
                averageOrderValue = report.summary?.averageOrderValue ?: 0.0,
                activeColmados = report.summary?.activeColmados ?: 0
            )

            Log.d(TAG, "Resumen: Ingresos=${summary.totalRevenue}, Comisiones=${summary.totalPlatformFees}")
            Result.Success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.Error("Error al cargar resumen: ${e.message}")
        }
    }

    suspend fun getDailyRevenue(): Result<List<DailyRevenue>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo ingresos diarios...")
            val report = getFinanceReport()
            
            val dailyRevenue = report.dailyRevenue?.map { data ->
                DailyRevenue(
                    date = data.date,
                    revenue = data.revenue,
                    platformFees = data.platformFees,
                    orders = data.orders,
                    successfulPayments = data.successfulPayments,
                    failedPayments = data.failedPayments
                )
            } ?: emptyList()

            Result.Success(dailyRevenue)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.Error("Error al cargar ingresos: ${e.message}")
        }
    }

    suspend fun getRevenueByColmado(): Result<List<RevenueByColmado>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo ingresos por colmado...")
            val report = getFinanceReport()
            
            val revenueByColmado = report.revenueByColmado?.map { data ->
                RevenueByColmado(
                    colmadoId = data.colmadoId,
                    colmadoName = data.colmadoName,
                    totalRevenue = data.totalRevenue,
                    platformFees = data.platformFees,
                    netRevenue = data.netRevenue,
                    orderCount = data.orderCount,
                    successfulPayments = data.successfulPayments,
                    failedPayments = data.failedPayments
                )
            } ?: emptyList()

            Result.Success(revenueByColmado)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.Error("Error al cargar ingresos: ${e.message}")
        }
    }

    suspend fun getRecentPayments(): Result<List<PaymentRecord>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo pagos recientes...")
            val report = getFinanceReport()
            
            val payments = report.recentPayments?.map { data ->
                PaymentRecord(
                    id = data.id,
                    orderId = data.order_id,
                    orderNumber = data.order_number,
                    colmadoId = data.colmado_id,
                    colmadoName = data.colmado_name,
                    stripePaymentIntentId = data.stripe_payment_intent_id,
                    stripeChargeId = data.stripe_charge_id,
                    amount = convertCentsToDollars(data.amount),
                    platformFeeAmount = convertCentsToDollars(data.platform_fee_amount),
                    transferAmount = convertCentsToDollars(data.transfer_amount),
                    currency = data.currency,
                    status = PaymentStatusRecord.fromString(data.status),
                    paymentMethodType = data.payment_method_type,
                    cardBrand = data.card_brand,
                    cardLast4 = data.card_last4,
                    errorCode = data.error_code,
                    errorMessage = data.error_message,
                    createdAt = data.created_at,
                    succeededAt = data.succeeded_at,
                    failedAt = data.failed_at
                )
            } ?: emptyList()

            Result.Success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.Error("Error al cargar pagos: ${e.message}")
        }
    }

    suspend fun getFullMetrics(): Result<FinanceMetrics> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo metricas completas...")
            val report = getFinanceReport()

            val summary = FinancialSummary(
                totalRevenue = report.summary?.totalRevenue ?: 0.0,
                totalPlatformFees = report.summary?.totalPlatformFees ?: 0.0,
                totalTransfers = report.summary?.totalTransfers ?: 0.0,
                totalOrders = report.summary?.totalOrders ?: 0,
                successfulPayments = report.summary?.successfulPayments ?: 0,
                failedPayments = report.summary?.failedPayments ?: 0,
                pendingPayments = report.summary?.pendingPayments ?: 0,
                averageOrderValue = report.summary?.averageOrderValue ?: 0.0,
                activeColmados = report.summary?.activeColmados ?: 0
            )

            val dailyRevenue = report.dailyRevenue?.map { data ->
                DailyRevenue(
                    date = data.date,
                    revenue = data.revenue,
                    platformFees = data.platformFees,
                    orders = data.orders,
                    successfulPayments = data.successfulPayments,
                    failedPayments = data.failedPayments
                )
            } ?: emptyList()

            val revenueByColmado = report.revenueByColmado?.map { data ->
                RevenueByColmado(
                    colmadoId = data.colmadoId,
                    colmadoName = data.colmadoName,
                    totalRevenue = data.totalRevenue,
                    platformFees = data.platformFees,
                    netRevenue = data.netRevenue,
                    orderCount = data.orderCount,
                    successfulPayments = data.successfulPayments,
                    failedPayments = data.failedPayments
                )
            } ?: emptyList()

            val recentPayments = report.recentPayments?.map { data ->
                PaymentRecord(
                    id = data.id,
                    orderId = data.order_id,
                    orderNumber = data.order_number,
                    colmadoId = data.colmado_id,
                    colmadoName = data.colmado_name,
                    stripePaymentIntentId = data.stripe_payment_intent_id,
                    stripeChargeId = data.stripe_charge_id,
                    amount = convertCentsToDollars(data.amount),
                    platformFeeAmount = convertCentsToDollars(data.platform_fee_amount),
                    transferAmount = convertCentsToDollars(data.transfer_amount),
                    currency = data.currency,
                    status = PaymentStatusRecord.fromString(data.status),
                    paymentMethodType = data.payment_method_type,
                    cardBrand = data.card_brand,
                    cardLast4 = data.card_last4,
                    errorCode = data.error_code,
                    errorMessage = data.error_message,
                    createdAt = data.created_at,
                    succeededAt = data.succeeded_at,
                    failedAt = data.failed_at
                )
            } ?: emptyList()

            Result.Success(
                FinanceMetrics(
                    summary = summary,
                    dailyRevenue = dailyRevenue,
                    revenueByColmado = revenueByColmado,
                    recentPayments = recentPayments
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.Error("Error al cargar metricas: ${e.message}")
        }
    }

    fun cleanup() {
        try {
            httpClient.close()
            Log.d(TAG, "HttpClient cerrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando HttpClient: ${e.message}")
        }
    }
}
