package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.network.ApiService
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository(private val context: Context) {

    private val apiService = ApiService.create()
    private val TAG = "OrderRepository"

    /**
     * Obtiene todas las órdenes de un colmado (para el vendedor)
     */
    suspend fun getColmadoOrders(): Result<List<OrderWithDetails>> = withContext(Dispatchers.IO) {
        try {
            // Obtener colmado_id del vendedor actual
            val sellerRepository = SellerRepository(context)
            val colmadoResult = sellerRepository.getSellerColmadoId(
                SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("Usuario no autenticado"))
            )

            when (colmadoResult) {
                is SellerRepository.Result.Error -> {
                    return@withContext Result.failure(Exception(colmadoResult.message))
                }
                is SellerRepository.Result.Success -> {
                    val colmadoId = colmadoResult.data
                    Log.d(TAG, "📋 Obteniendo órdenes del colmado: $colmadoId")

                    // Llamar a la Edge Function
                    val response = apiService.getColmadoOrders(colmadoId)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true && body.orders != null) {
                            Log.d(TAG, "✅ ${body.orders.size} órdenes obtenidas")
                            return@withContext Result.success(body.orders)
                        } else {
                            val errorMsg = body?.message ?: "Error obteniendo órdenes"
                            Log.e(TAG, "❌ $errorMsg")
                            return@withContext Result.failure(Exception(errorMsg))
                        }
                    } else {
                        val errorMsg = "Error ${response.code()}: ${response.message()}"
                        Log.e(TAG, "❌ $errorMsg")
                        return@withContext Result.failure(Exception(errorMsg))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo órdenes: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al obtener órdenes: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Actualiza el estado de una orden
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Actualizando orden $orderId a estado ${newStatus.name}")

            // Usar Supabase directamente para actualizar el estado
            SupabaseClient.client.from("orders")
                .update(mapOf("status" to newStatus.name.lowercase())) {
                    filter {
                        eq("id", orderId)
                    }
                }

            Log.d(TAG, "✅ Estado actualizado correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando estado: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al actualizar estado: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Asigna un delivery a una orden y cambia el estado a IN_DELIVERY
     * Esto automáticamente generará un código de verificación mediante el trigger de la BD
     */
    suspend fun assignDeliveryToOrder(orderId: String, deliveryUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚚 Asignando delivery $deliveryUserId a orden $orderId")

            // Actualizar delivery_user_id y cambiar estado a IN_DELIVERY
            SupabaseClient.client.from("orders")
                .update(mapOf(
                    "delivery_user_id" to deliveryUserId,
                    "status" to "in_delivery"
                )) {
                    filter {
                        eq("id", orderId)
                    }
                }

            Log.d(TAG, "✅ Delivery asignado correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error asignando delivery: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al asignar delivery: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Verifica el código de entrega y marca la orden como DELIVERED
     */
    suspend fun verifyDeliveryCode(orderId: String, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔐 Verificando código para orden $orderId")

            // Primero obtener la orden para verificar el código
            val order = SupabaseClient.client.from("orders")
                .select() {
                    filter {
                        eq("id", orderId)
                    }
                }
                .decodeSingle<OrderRecord>()

            // Verificar que el código coincida
            if (order.verification_code != code) {
                Log.e(TAG, "❌ Código incorrecto")
                return@withContext Result.failure(Exception("Código de verificación incorrecto"))
            }

            // Actualizar estado a DELIVERED
            SupabaseClient.client.from("orders")
                .update(mapOf(
                    "status" to "delivered",
                    "delivered_at" to kotlinx.datetime.Clock.System.now().toString()
                )) {
                    filter {
                        eq("id", orderId)
                    }
                }

            Log.d(TAG, "✅ Entrega verificada correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando código: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                e.message?.contains("incorrecto") == true -> e.message ?: "Código incorrecto"
                else -> "Error al verificar código: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Obtiene todas las órdenes asignadas a un delivery user
     */
    suspend fun getDeliveryOrders(deliveryUserId: String): Result<List<OrderWithDetails>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 Obteniendo órdenes del delivery: $deliveryUserId")

            // Llamar a la Edge Function
            val response = apiService.getDeliveryOrders(deliveryUserId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.orders != null) {
                    Log.d(TAG, "✅ ${body.orders.size} órdenes obtenidas")
                    return@withContext Result.success(body.orders)
                } else {
                    val errorMsg = body?.message ?: "Error obteniendo órdenes"
                    Log.e(TAG, "❌ $errorMsg")
                    return@withContext Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Error ${response.code()}: ${response.message()}"
                Log.e(TAG, "❌ $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo órdenes: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> "Error al obtener órdenes: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Obtiene deliveries disponibles (sin pedidos activos asignados)
     */
    suspend fun getAvailableDeliveries(): Result<List<DeliveryUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚚 Obteniendo deliveries disponibles...")

            // Obtener deliveries usando la vista deliveries_view
            val deliveryUsers = SupabaseClient.client.from("deliveries_view")
                .select()
                .decodeList<DeliveryViewRecord>()

            Log.d(TAG, "📋 ${deliveryUsers.size} deliveries encontrados")

            // Obtener pedidos activos (in_delivery) para contar cuántos tiene cada delivery
            val activeOrders = SupabaseClient.client.from("orders")
                .select() {
                    filter {
                        eq("status", "in_delivery")
                    }
                }
                .decodeList<ActiveOrderRecord>()
                .filter { it.delivery_user_id != null } // Filtrar en Kotlin los que tienen delivery asignado

            // Contar pedidos activos por delivery
            val activeOrdersCount = activeOrders.groupingBy { it.delivery_user_id!! }.eachCount()

            // Mapear a DeliveryUser con conteo de pedidos activos
            val deliveryUsersWithCount = deliveryUsers.map { profile ->
                DeliveryUser(
                    id = profile.id,
                    name = profile.nombre,
                    phone = profile.email, // Usar email como phone por ahora
                    rating = 4.5f, // TODO: Implementar sistema de ratings
                    activeDeliveries = activeOrdersCount[profile.id] ?: 0
                )
            }

            // Ordenar: primero los disponibles (0 pedidos), luego por menos pedidos
            val sorted = deliveryUsersWithCount.sortedBy { it.activeDeliveries }

            Log.d(TAG, "✅ ${sorted.filter { it.activeDeliveries == 0 }.size} deliveries disponibles")

            Result.success(sorted)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo deliveries: ${e.message}", e)
            Result.failure(Exception("Error al obtener deliveries: ${e.message}"))
        }
    }

    @kotlinx.serialization.Serializable
    private data class OrderRecord(
        val id: String,
        val verification_code: String? = null
    )

    @kotlinx.serialization.Serializable
    private data class ProfileWithRoleRecord(
        val id: String,
        val nombre: String,
        val email: String,
        val activo: Boolean,
        val role: String
    )

    @kotlinx.serialization.Serializable
    private data class ActiveOrderRecord(
        val id: String,
        val delivery_user_id: String? = null,
        val status: String
    )

    @kotlinx.serialization.Serializable
    private data class DeliveryViewRecord(
        val id: String,
        val nombre: String,
        val email: String,
        val activo: Boolean,
        val role_in_colmado: String? = null
    )
}

data class DeliveryUser(
    val id: String,
    val name: String,
    val phone: String,
    val rating: Float,
    val activeDeliveries: Int
)
