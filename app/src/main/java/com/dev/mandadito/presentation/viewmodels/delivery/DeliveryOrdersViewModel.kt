package com.dev.mandadito.presentation.viewmodels.delivery

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.data.repository.OrderRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive

data class DeliveryOrdersUiState(
    val orders: List<OrderWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isVerificationSuccess: Boolean = false,
    val selectedOrder: OrderWithDetails? = null
)

class DeliveryOrdersViewModel(context: Context) : ViewModel() {

    private val orderRepository = OrderRepository(context)
    private val supabase = SupabaseClient.client
    private val TAG = "DeliveryOrdersViewModel"

    private val _uiState = MutableStateFlow(DeliveryOrdersUiState())
    val uiState: StateFlow<DeliveryOrdersUiState> = _uiState.asStateFlow()

    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var currentUserId: String? = null

    init {
        loadOrders()
        startRealtimeSubscription()
    }

    /**
     * Suscribe a cambios en tiempo real de la tabla orders
     * CORRECCIÓN: Sin filtro en el canal, filtramos en Kotlin
     */
    private fun startRealtimeSubscription() {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId == null) {
                    Log.e(TAG, "❌ No hay usuario autenticado para Realtime")
                    return@launch
                }

                currentUserId = userId
                val channelName = "orders_delivery_$userId"
                Log.d(TAG, "🔴 Iniciando suscripción Realtime para delivery: $userId")

                // Limpiar canal existente si lo hay
                realtimeChannel?.let {
                    supabase.realtime.removeChannel(it)
                }

                // Crear canal de Realtime SIN FILTRO
                realtimeChannel = supabase.realtime.channel(channelName)

                // Suscribirse a TODOS los cambios en la tabla orders
                realtimeChannel!!.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "orders"
                    // SIN FILTRO AQUÍ - Filtramos en Kotlin
                }.onEach { action ->
                    Log.d(TAG, "📡 Cambio detectado en orders: ${action::class.simpleName}")

                    try {
                        val orderId = extractOrderId(action)

                        if (orderId != null) {
                            when (action) {
                                is PostgresAction.Insert -> {
                                    val record = action.record as? JsonObject
                                    val deliveryUserId = record?.get("delivery_user_id")?.jsonPrimitive?.content
                                    val statusStr = record?.get("status")?.jsonPrimitive?.content
                                    Log.d(TAG, "  🔍 INSERT - delivery_user_id: $deliveryUserId, status: $statusStr, currentUserId: $currentUserId")

                                    if (deliveryUserId == currentUserId) {
                                        Log.d(TAG, "  ✅ Nuevo pedido asignado al delivery, recargando...")
                                        loadOrders()
                                    } else {
                                        Log.d(TAG, "  ⏭️ Pedido asignado a otro delivery")
                                    }
                                }
                                is PostgresAction.Update -> {
                                    val record = action.record as? JsonObject
                                    val newDeliveryUserId = record?.get("delivery_user_id")?.jsonPrimitive?.content
                                    val newStatusStr = record?.get("status")?.jsonPrimitive?.content

                                    val oldRecord = action.oldRecord as? JsonObject
                                    val oldDeliveryUserId = oldRecord?.get("delivery_user_id")?.jsonPrimitive?.content
                                    val oldStatusStr = oldRecord?.get("status")?.jsonPrimitive?.content

                                    val orderExists = _uiState.value.orders.any { it.order.id == orderId }
                                    Log.d(TAG, "  🔍 UPDATE - order_id: $orderId, newDeliveryUserId: $newDeliveryUserId, newStatus: $newStatusStr, oldDeliveryUserId: $oldDeliveryUserId, oldStatus: $oldStatusStr, currentUserId: $currentUserId, orderExists: $orderExists")

                                    // Verificar si pertenece al delivery actual (por delivery_user_id) O si ya existe en nuestra lista
                                    val belongsToDelivery = newDeliveryUserId == currentUserId || oldDeliveryUserId == currentUserId

                                    if (orderExists || belongsToDelivery) {
                                        // Si cambió el delivery_user_id O el estado, recargar
                                        val deliveryUserIdChanged = newDeliveryUserId != oldDeliveryUserId
                                        val statusChanged = newStatusStr != oldStatusStr

                                        if (deliveryUserIdChanged || statusChanged) {
                                            Log.d(TAG, "  ✅ El pedido cambió, recargando... (orderExists: $orderExists, deliveryUserId: $deliveryUserIdChanged, status: $statusChanged)")
                                            loadOrders()
                                        } else {
                                            Log.d(TAG, "  ⏭️ Pedido asignado al delivery pero sin cambios relevantes")
                                        }
                                    } else {
                                        Log.d(TAG, "  ⏭️ Pedido no asignado al delivery actual y no está en nuestra lista")
                                    }
                                }
                                is PostgresAction.Delete -> {
                                    val oldRecord = action.oldRecord as? JsonObject
                                    val oldDeliveryUserId = oldRecord?.get("delivery_user_id")?.jsonPrimitive?.content
                                    Log.d(TAG, "  🔍 DELETE - oldDeliveryUserId: $oldDeliveryUserId, currentUserId: $currentUserId")

                                    if (oldDeliveryUserId == currentUserId) {
                                        Log.d(TAG, "  ✅ Pedido asignado al delivery fue eliminado, recargando...")
                                        loadOrders()
                                    } else {
                                        Log.d(TAG, "  ⏭️ Pedido no asignado al delivery actual")
                                    }
                                }
                                else -> {
                                    Log.d(TAG, "  ⏭️ No implementado")
                                }
                            }
                        } else {
                            Log.d(TAG, "  ⚠️ No se pudo extraer el ID del pedido")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error procesando evento: ${e.message}", e)
                    }
                }.launchIn(viewModelScope)

                // Suscribirse al canal
                realtimeChannel!!.subscribe()
                Log.d(TAG, "✅ Suscripción Realtime activa en canal: $channelName")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error configurando Realtime: ${e.message}", e)
            }
        }
    }

    /**
     * Detiene la suscripción de Realtime correctamente
     */
    private fun stopRealtimeSubscription() {
        viewModelScope.launch {
            try {
                realtimeChannel?.let { channel ->
                    Log.d(TAG, "🔴 Deteniendo suscripción Realtime")
                    // Primero unsubscribe
                    channel.unsubscribe()
                    // Luego remueve el canal
                    supabase.realtime.removeChannel(channel)
                    realtimeChannel = null
                    Log.d(TAG, "✅ Suscripción Realtime detenida")
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

    fun loadOrders() {
        viewModelScope.launch {
            // Solo mostrar loading si no hay órdenes cargadas
            if (_uiState.value.orders.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario no autenticado")

                Log.d(TAG, "📦 Cargando órdenes del delivery: $userId")

                orderRepository.getDeliveryOrders(userId).fold(
                    onSuccess = { orders ->
                        Log.d(TAG, "📋 Total órdenes recibidas: ${orders.size}")
                        orders.forEach { orderWithDetails ->
                            Log.d(TAG, "  📦 Pedido ${orderWithDetails.order.id} (${orderWithDetails.order.orderNumber}) - Estado: ${orderWithDetails.order.status.name}")
                        }

                        // Filtrar solo pedidos en delivery o entregados
                        val deliveryOrders = orders.filter { orderWithDetails ->
                            orderWithDetails.order.status == com.dev.mandadito.data.models.OrderStatus.IN_DELIVERY ||
                                    orderWithDetails.order.status == com.dev.mandadito.data.models.OrderStatus.DELIVERED
                        }

                        Log.d(TAG, "✅ Órdenes del delivery (filtradas): ${deliveryOrders.size}")

                        _uiState.update {
                            it.copy(
                                orders = deliveryOrders.sortedByDescending { order ->
                                    order.order.createdAt
                                },
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error cargando órdenes: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al cargar pedidos"
                            )
                        }
                    }
                )
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

    fun verifyDeliveryCode(orderId: String, code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                orderRepository.verifyDeliveryCode(orderId, code).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Entrega confirmada correctamente",
                                isVerificationSuccess = true
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al verificar código"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error inesperado"
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

    fun resetVerificationSuccess() {
        _uiState.update { it.copy(isVerificationSuccess = false) }
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