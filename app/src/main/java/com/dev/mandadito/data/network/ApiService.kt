package com.dev.mandadito.data.network

import com.dev.mandadito.data.models.CreateOrderRequest
import com.dev.mandadito.data.models.CreateOrderResponse
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderStatusDeserializer
import com.dev.mandadito.data.models.StripeConfigResponse
import com.dev.mandadito.data.models.CreateStripeAccountRequest
import com.dev.mandadito.data.models.StripeAccountResponse
import com.dev.mandadito.data.models.CheckStripeStatusRequest
import com.dev.mandadito.data.models.StripeStatusResponse
import com.google.gson.GsonBuilder
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * API Service para comunicarse con el backend Flask
 */
interface ApiService {

    // =====================================================
    // STRIPE
    // =====================================================

    /**
     * Obtiene la configuración de Stripe (publishable key)
     */
    @GET("stripe-config")
    suspend fun getStripeConfig(): Response<StripeConfigResponse>

    /**
     * Crea una cuenta Stripe Connect para un colmado
     */
    @POST("stripe-connect-create")
    suspend fun createStripeAccount(
        @Body request: CreateStripeAccountRequest
    ): Response<StripeAccountResponse>

    /**
     * Verifica el estado de la cuenta Stripe de un colmado
     */
    @POST("stripe-connect-status")
    suspend fun checkStripeStatus(
        @Body request: CheckStripeStatusRequest
    ): Response<StripeStatusResponse>

    /**
     * Refresca el link de onboarding de Stripe
     */
    @POST("stripe-connect-refresh")
    suspend fun refreshStripeOnboardingLink(
        @Body request: CheckStripeStatusRequest
    ): Response<StripeAccountResponse>

    // =====================================================
    // ORDERS
    // =====================================================

    /**
     * Crea una orden desde un carrito
     */
    @POST("orders-create")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<CreateOrderResponse>

    /**
     * Obtiene los detalles de una orden
     */
    @GET("orders-get/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: String,
        @Query("userId") userId: String
    ): Response<OrderDetailsResponse>

    /**
     * Confirma el pago de una orden (después de PaymentSheet exitoso)
     */
    @POST("orders-confirm-payment/{orderId}")
    suspend fun confirmPayment(
        @Path("orderId") orderId: String,
        @Body request: ConfirmPaymentRequest
    ): Response<GenericResponse>

    /**
     * Cancela una orden
     */
    @POST("orders-cancel/{orderId}")
    suspend fun cancelOrder(
        @Path("orderId") orderId: String,
        @Body request: CancelOrderRequest
    ): Response<GenericResponse>

    /**
     * Obtiene todas las órdenes de un colmado (para el vendedor)
     */
    @GET("orders-get-colmado")
    suspend fun getColmadoOrders(
        @Query("colmadoId") colmadoId: String
    ): Response<ColmadoOrdersResponse>

    /**
     * Obtiene todas las órdenes asignadas a un delivery
     */
    @GET("orders-get-delivery")
    suspend fun getDeliveryOrders(
        @Query("deliveryUserId") deliveryUserId: String
    ): Response<DeliveryOrdersResponse>

    companion object {
        /**
         * URL base - Usa Supabase Edge Functions
         * Funciona en emulador Y dispositivo real
         */
        private val BASE_URL: String
            get() {
                val supabaseUrl = com.dev.mandadito.config.AppConfig.SUPABASE_URL
                // Edge Functions están en /functions/v1/
                return "$supabaseUrl/functions/v1/"
            }

        /**
         * Crea una instancia del servicio
         */
        fun create(): ApiService {
            // Configurar OkHttpClient con timeouts más largos y auth interceptor
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)    // Timeout para conectar
                .readTimeout(30, TimeUnit.SECONDS)       // Timeout para leer respuesta
                .writeTimeout(30, TimeUnit.SECONDS)      // Timeout para escribir request
                .addInterceptor { chain ->
                    val originalRequest = chain.request()

                    // Obtener el token de sesión de Supabase
                    val token = try {
                        runBlocking {
                            SupabaseClient.client.auth.currentSessionOrNull()?.accessToken
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ApiService", "Error obteniendo token: ${e.message}")
                        null
                    }

                    // Agregar header de autorización si hay token
                    val newRequest = if (token != null) {
                        originalRequest.newBuilder()
                            .header("Authorization", "Bearer $token")
                            .header("apikey", com.dev.mandadito.config.AppConfig.SUPABASE_ANON_KEY)
                            .build()
                    } else {
                        originalRequest.newBuilder()
                            .header("apikey", com.dev.mandadito.config.AppConfig.SUPABASE_ANON_KEY)
                            .build()
                    }

                    android.util.Log.d("ApiService", "🔑 Request con token: ${token != null}")

                    chain.proceed(newRequest)
                }
                .build()

            // Configurar Gson con deserializadores personalizados
            val gson = GsonBuilder()
                .registerTypeAdapter(OrderStatus::class.java, OrderStatusDeserializer())
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)  // Agregar el cliente configurado
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}

/**
 * Response genérica del backend
 */
data class GenericResponse(
    val success: Boolean,
    val message: String? = null
)

/**
 * Response de detalles de orden
 */
data class OrderDetailsResponse(
    val success: Boolean,
    val order: OrderWithDetails? = null,
    val message: String? = null
)

/**
 * Request para confirmar pago
 */
data class ConfirmPaymentRequest(
    val userId: String,
    val cartId: String? = null,
    val paymentIntentId: String? = null
)

/**
 * Request para cancelar orden
 */
data class CancelOrderRequest(
    val userId: String,
    val reason: String? = null
)

/**
 * Response de órdenes del colmado
 */
data class ColmadoOrdersResponse(
    val success: Boolean,
    val orders: List<OrderWithDetails>? = null,
    val message: String? = null
)

/**
 * Response de órdenes del delivery
 */
data class DeliveryOrdersResponse(
    val success: Boolean,
    val orders: List<OrderWithDetails>? = null,
    val message: String? = null
)
