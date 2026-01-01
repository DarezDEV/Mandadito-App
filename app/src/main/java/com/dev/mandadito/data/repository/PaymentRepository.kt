package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.CreateOrderRequest
import com.dev.mandadito.data.models.CreateOrderResponse
import com.dev.mandadito.data.models.StripeConfigResponse
import com.dev.mandadito.data.network.ApiService
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar pagos con Stripe
 */
class PaymentRepository(private val context: Context) {

    private val apiService = ApiService.create()
    private val TAG = "PaymentRepository"

    /**
     * Inicializa Stripe con la publishable key del backend
     */
    suspend fun initializeStripe(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔧 Inicializando Stripe...")

            val response = apiService.getStripeConfig()

            Log.d(TAG, "📡 Response code: ${response.code()}")
            Log.d(TAG, "📡 Response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val config = response.body()!!
                val publishableKey = config.publishableKey

                Log.d(TAG, "📦 Config recibida: $config")
                Log.d(TAG, "🔑 Publishable key: ${if (publishableKey.isNullOrBlank()) "NULL o VACÍO" else "OK (${publishableKey.take(20)}...)"}")

                // Validar que la key no esté vacía
                if (publishableKey.isNullOrBlank()) {
                    val error = "Publishable key está vacía o es nula"
                    Log.e(TAG, "❌ $error")
                    return@withContext Result.failure(Exception(error))
                }

                // Configurar Stripe SDK
                PaymentConfiguration.init(context, publishableKey)

                Log.d(TAG, "✅ Stripe inicializado correctamente")
                Result.success(publishableKey)
            } else {
                val errorBody = response.errorBody()?.string()
                val error = "Error obteniendo configuración de Stripe: ${response.code()} - $errorBody"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando Stripe: ${e.message}")
            Log.e(TAG, "❌ Stack trace: ", e)
            Result.failure(e)
        }
    }

    /**
     * Crea una orden y obtiene el client_secret para pago
     *
     * @param userId ID del usuario
     * @param cartId ID del carrito
     * @param addressId ID de la dirección de entrega
     * @param deliveryFee Costo de delivery
     * @param customerNotes Notas del cliente
     * @return Result con CreateOrderResponse
     */
    suspend fun createOrder(
        userId: String,
        cartId: String,
        addressId: String,
        deliveryFee: Double,
        customerNotes: String? = null
    ): Result<CreateOrderResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 Creando orden...")
            Log.d(TAG, "  - userId: $userId")
            Log.d(TAG, "  - cartId: $cartId")
            Log.d(TAG, "  - addressId: $addressId")
            Log.d(TAG, "  - deliveryFee: $deliveryFee")

            val request = CreateOrderRequest(
                userId = userId,
                cartId = cartId,
                addressId = addressId,
                deliveryFee = deliveryFee,
                customerNotes = customerNotes
            )

            val response = apiService.createOrder(request)

            Log.d(TAG, "📡 Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!

                Log.d(TAG, "📦 Order response: $orderResponse")

                if (orderResponse.success) {
                    Log.d(TAG, "✅ Orden creada: ${orderResponse.orderNumber}")
                    Result.success(orderResponse)
                } else {
                    val error = orderResponse.message ?: "Error desconocido"
                    Log.e(TAG, "❌ $error")
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val error = "Error creando orden: ${response.code()} - $errorBody"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando orden: ${e.message}")
            Log.e(TAG, "❌ Stack trace: ", e)
            Result.failure(e)
        }
    }

    /**
     * Confirma el pago de una orden en el backend
     *
     * @param orderId ID de la orden
     * @param userId ID del usuario
     * @param paymentIntentId ID del PaymentIntent (opcional)
     * @return Result con respuesta genérica
     */
    suspend fun confirmPayment(
        orderId: String,
        userId: String,
        cartId: String? = null,
        paymentIntentId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "✅ Confirmando pago en backend...")
            Log.d(TAG, "  - orderId: $orderId")
            Log.d(TAG, "  - userId: $userId")
            Log.d(TAG, "  - cartId: $cartId")
            Log.d(TAG, "  - paymentIntentId: $paymentIntentId")

            val request = com.dev.mandadito.data.network.ConfirmPaymentRequest(
                userId = userId,
                cartId = cartId,
                paymentIntentId = paymentIntentId
            )

            val response = apiService.confirmPayment(orderId, request)

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                if (result.success) {
                    Log.d(TAG, "✅ Pago confirmado en backend")
                    Result.success(Unit)
                } else {
                    val error = result.message ?: "Error desconocido"
                    Log.e(TAG, "❌ Error confirmando pago: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val error = "Error confirmando pago: ${response.code()} - $errorBody"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error confirmando pago: ${e.message}")
            Log.e(TAG, "❌ Stack trace: ", e)
            Result.failure(e)
        }
    }

    /**
     * Crea la configuración de PaymentSheet
     *
     * @param clientSecret Client secret del PaymentIntent
     * @param merchantDisplayName Nombre del comercio (tu app)
     * @return PaymentSheet.Configuration
     */
    fun createPaymentSheetConfiguration(
        clientSecret: String,
        merchantDisplayName: String = "Mandadito"
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = merchantDisplayName,
            // Configuración de cliente (opcionales)
            customer = null,  // Si tienes Customer ID de Stripe
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "DO",  // República Dominicana
                currencyCode = "DOP"
            ),
            // Permitir guardar métodos de pago para uso futuro
            allowsDelayedPaymentMethods = false,
            // Estilo visual
            appearance = PaymentSheet.Appearance(
                colorsLight = PaymentSheet.Colors.defaultLight.copy(
                    primary = android.graphics.Color.parseColor("#4CAF50")
                )
            )
        )
    }

    /**
     * Maneja el resultado del PaymentSheet
     *
     * @param result Resultado de PaymentSheet
     * @param onSuccess Callback cuando el pago es exitoso
     * @param onFailure Callback cuando el pago falla
     * @param onCancelled Callback cuando el usuario cancela
     */
    fun handlePaymentSheetResult(
        result: PaymentSheetResult,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Log.d(TAG, "✅ Pago completado exitosamente")
                onSuccess()
            }
            is PaymentSheetResult.Failed -> {
                val error = result.error.message ?: "Error desconocido"
                Log.e(TAG, "❌ Pago falló: $error")
                onFailure(error)
            }
            is PaymentSheetResult.Canceled -> {
                Log.d(TAG, "❌ Pago cancelado por el usuario")
                onCancelled()
            }
        }
    }

    /**
     * Sealed class para resultados de operaciones
     */
    sealed class PaymentResult<out T> {
        data class Success<T>(val data: T) : PaymentResult<T>()
        data class Error(val message: String) : PaymentResult<Nothing>()
        object Loading : PaymentResult<Nothing>()
    }
}