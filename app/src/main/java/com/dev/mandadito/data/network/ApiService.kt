package com.dev.mandadito.data.network

import com.dev.mandadito.data.models.CreateOrderRequest
import com.dev.mandadito.data.models.CreateOrderResponse
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.models.StripeConfigResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

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
    @GET("stripe/config")
    suspend fun getStripeConfig(): Response<StripeConfigResponse>

    // =====================================================
    // ORDERS
    // =====================================================

    /**
     * Crea una orden desde un carrito
     */
    @POST("orders/create")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<CreateOrderResponse>

    /**
     * Obtiene los detalles de una orden
     */
    @GET("orders/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: String,
        @Query("user_id") userId: String
    ): Response<OrderDetailsResponse>

    /**
     * Cancela una orden
     */
    @POST("orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: String,
        @Body request: CancelOrderRequest
    ): Response<GenericResponse>

    companion object {
        /**
         * URL base de tu backend
         * CAMBIAR EN PRODUCCIÓN
         */
        private const val BASE_URL = "http://10.0.2.2:5000/"  // Emulador Android
        // private const val BASE_URL = "http://192.168.1.10:5000/"  // Dispositivo real
        // private const val BASE_URL = "https://tu-dominio.com/"  // Producción

        /**
         * Crea una instancia del servicio
         */
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
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
 * Request para cancelar orden
 */
data class CancelOrderRequest(
    val userId: String,
    val reason: String? = null
)
