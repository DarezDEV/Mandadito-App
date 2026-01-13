package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Order
import com.dev.mandadito.data.models.OrderItem
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.models.ColmadoInfo
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ClientOrdersUiState(
    val orders: List<OrderWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedOrder: OrderWithDetails? = null
)

class ClientOrdersViewModel(context: Context) : ViewModel() {

    private val TAG = "ClientOrdersViewModel"
    private val supabase = SupabaseClient.client

    private val _uiState = MutableStateFlow(ClientOrdersUiState())
    val uiState: StateFlow<ClientOrdersUiState> = _uiState.asStateFlow()

    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var currentUserId: String? = null

    init {
        loadOrders()
        startRealtimeSubscription()
    }

    private fun startRealtimeSubscription() {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId == null) {
                    Log.e(TAG, "❌ No hay usuario autenticado")
                    return@launch
                }

                currentUserId = userId
                Log.d(TAG, "🔴 Configurando Realtime para cliente: $userId")

                // Limpiar canal existente
                realtimeChannel?.let {
                    try {
                        it.unsubscribe()
                        supabase.realtime.removeChannel(it)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error limpiando canal: ${e.message}")
                    }
                }

                // Crear canal único
                val channelId = "client_orders_${System.currentTimeMillis()}"
                realtimeChannel = supabase.realtime.channel(channelId)

                // Escuchar TODOS los cambios en orders
                realtimeChannel!!.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "orders"
                }.catch { e ->
                    Log.e(TAG, "❌ Error en flow de Realtime: ${e.message}", e)
                }.onEach { action ->
                    Log.d(TAG, "📡 Evento Realtime: ${action::class.simpleName}")

                    try {
                        val orderId = extractOrderId(action)
                        if (orderId != null) {
                            val orderExists = _uiState.value.orders.any { it.order.id == orderId }
                            if (orderExists) {
                                Log.d(TAG, "  ✅ El pedido $orderId está en nuestra lista, recargando...")
                                loadOrders()
                            } else if (action is PostgresAction.Insert) {
                                val userId = extractUserId(action.record)
                                if (userId == currentUserId) {
                                    Log.d(TAG, "  ✅ Nuevo pedido para el cliente, recargando...")
                                    loadOrders()
                                } else {
                                    Log.d(TAG, "  ⏭️ Pedido de otro cliente")
                                }
                            } else {
                                Log.d(TAG, "  ⏭️ Pedido no pertenece al cliente actual")
                            }
                        } else {
                            Log.d(TAG, "  ⚠️ No se pudo extraer el ID del pedido")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error procesando evento: ${e.message}", e)
                    }
                }.launchIn(viewModelScope)

                // Suscribirse
                realtimeChannel!!.subscribe()
                Log.d(TAG, "✅ Realtime activo en canal: $channelId")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en startRealtimeSubscription: ${e.message}", e)
            }
        }
    }

    private fun extractUserId(record: Any?): String? {
        return try {
            when (record) {
                is Map<*, *> -> {
                    record["user_id"] as? String
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo user_id: ${e.message}")
            null
        }
    }

    private fun extractOrderId(action: PostgresAction): String? {
        return try {
            when (action) {
                is PostgresAction.Insert -> {
                    val record = action.record as? JsonObject
                    record?.get("id")?.jsonPrimitive?.content
                }
                is PostgresAction.Update -> {
                    val record = action.record as? JsonObject
                    val id = record?.get("id")?.jsonPrimitive?.content
                    if (id != null) return id

                    val oldRecord = action.oldRecord as? JsonObject
                    oldRecord?.get("id")?.jsonPrimitive?.content
                }
                is PostgresAction.Delete -> {
                    val oldRecord = action.oldRecord as? JsonObject
                    oldRecord?.get("id")?.jsonPrimitive?.content
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo order_id: ${e.message}", e)
            null
        }
    }

    private fun stopRealtimeSubscription() {
        viewModelScope.launch {
            try {
                realtimeChannel?.let { channel ->
                    Log.d(TAG, "🔴 Deteniendo Realtime")
                    channel.unsubscribe()
                    supabase.realtime.removeChannel(channel)
                    realtimeChannel = null
                    Log.d(TAG, "✅ Realtime detenido")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deteniendo Realtime: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeSubscription()
    }

    fun loadOrders() {
        viewModelScope.launch {
            // Solo loading en primera carga
            val isFirstLoad = _uiState.value.orders.isEmpty()
            if (isFirstLoad) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario no autenticado")

                @Serializable
                data class OrderRecord(
                    val id: String,
                    val order_number: String,
                    val user_id: String,
                    val colmado_id: String,
                    val address_id: String,
                    val delivery_user_id: String? = null,
                    val status: String,
                    val subtotal: Double,
                    val delivery_fee: Double,
                    val platform_fee: Double,
                    val total: Double,
                    val customer_notes: String? = null,
                    val delivery_notes: String? = null,
                    val cancellation_reason: String? = null,
                    val verification_code: String? = null,
                    val created_at: String,
                    val updated_at: String,
                    val paid_at: String? = null,
                    val delivered_at: String? = null,
                    val cancelled_at: String? = null
                )

                @Serializable
                data class OrderItemRecord(
                    val id: String,
                    val order_id: String,
                    val product_id: String,
                    val product_name: String,
                    val product_price: Double,
                    val product_image_url: String? = null,
                    val quantity: Int,
                    val subtotal: Double
                )

                @Serializable
                data class ColmadoRecord(
                    val id: String,
                    val name: String,
                    val address: String,
                    val phone: String
                )

                val allOrders = supabase.from("orders").select().decodeList<OrderRecord>()
                val userOrders = allOrders.filter { it.user_id == userId }

                Log.d(TAG, "📦 Total órdenes: ${allOrders.size}, del usuario: ${userOrders.size}")

                val allOrderItems = supabase.from("order_items").select().decodeList<OrderItemRecord>()
                val allColmados = supabase.from("colmados").select().decodeList<ColmadoRecord>()

                val ordersWithDetails = userOrders.map { orderRecord ->
                    val orderItems = allOrderItems.filter { it.order_id == orderRecord.id }
                    val colmado = allColmados.firstOrNull { it.id == orderRecord.colmado_id }

                    OrderWithDetails(
                        order = Order(
                            id = orderRecord.id,
                            orderNumber = orderRecord.order_number,
                            userId = orderRecord.user_id,
                            colmadoId = orderRecord.colmado_id,
                            addressId = orderRecord.address_id,
                            deliveryUserId = orderRecord.delivery_user_id,
                            status = OrderStatus.fromString(orderRecord.status),
                            subtotal = orderRecord.subtotal,
                            deliveryFee = orderRecord.delivery_fee,
                            platformFee = orderRecord.platform_fee,
                            total = orderRecord.total,
                            customerNotes = orderRecord.customer_notes,
                            deliveryNotes = orderRecord.delivery_notes,
                            cancellationReason = orderRecord.cancellation_reason,
                            verificationCode = orderRecord.verification_code,
                            createdAt = orderRecord.created_at,
                            updatedAt = orderRecord.updated_at,
                            paidAt = orderRecord.paid_at,
                            deliveredAt = orderRecord.delivered_at,
                            cancelledAt = orderRecord.cancelled_at
                        ),
                        items = orderItems.map { item ->
                            OrderItem(
                                id = item.id,
                                orderId = item.order_id,
                                productId = item.product_id,
                                productName = item.product_name,
                                productPrice = item.product_price,
                                productImageUrl = item.product_image_url,
                                quantity = item.quantity,
                                subtotal = item.subtotal
                            )
                        },
                        colmado = colmado?.let {
                            ColmadoInfo(
                                id = it.id,
                                name = it.name,
                                address = it.address,
                                phone = it.phone
                            )
                        }
                    )
                }

                val validOrders = ordersWithDetails.filter { orderWithDetails ->
                    orderWithDetails.order.status != OrderStatus.PENDING &&
                            orderWithDetails.order.status != OrderStatus.PAYMENT_PROCESSING &&
                            orderWithDetails.order.status != OrderStatus.CANCELLED &&
                            orderWithDetails.order.status != OrderStatus.REFUNDED
                }

                Log.d(TAG, "✅ Órdenes válidas: ${validOrders.size}")

                _uiState.update {
                    it.copy(
                        orders = validOrders.sortedByDescending { order ->
                            order.order.createdAt
                        },
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando órdenes: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar pedidos"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun selectOrder(orderId: String) {
        val order = _uiState.value.orders.find { it.order.id == orderId }
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun getOrderById(orderId: String): OrderWithDetails? {
        return _uiState.value.orders.find { it.order.id == orderId }
    }

    fun refresh() {
        loadOrders()
    }
}