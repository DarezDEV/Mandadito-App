package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.repository.OrderRepository
import com.dev.mandadito.data.repository.SellerRepository
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.auth.auth
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SellerOrdersUiState(
    val orders: List<OrderWithDetails> = emptyList(),
    val availableDeliveries: List<com.dev.mandadito.data.repository.DeliveryUser> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingDeliveries: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedOrder: OrderWithDetails? = null,
    val isLoadingDetail: Boolean = false
)

class SellerOrdersViewModel(context: Context) : ViewModel() {

    private val TAG = "SellerOrdersViewModel"
    private val orderRepository = OrderRepository(context)
    private val sellerRepository = SellerRepository(context)

    private val _uiState = MutableStateFlow(SellerOrdersUiState())
    val uiState: StateFlow<SellerOrdersUiState> = _uiState.asStateFlow()

    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var currentColmadoId: String? = null

    init {
        loadOrders()
        startRealtimeSubscription()
    }

    private fun startRealtimeSubscription() {
        viewModelScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    Log.e(TAG, "❌ No hay usuario autenticado")
                    return@launch
                }

                when (val colmadoResult = sellerRepository.getSellerColmadoId(userId)) {
                    is SellerRepository.Result.Success -> {
                        val colmadoId = colmadoResult.data
                        currentColmadoId = colmadoId

                        Log.d(TAG, "🔴 Configurando Realtime para colmado: $colmadoId")

                        // Limpiar canal existente
                        realtimeChannel?.let {
                            try {
                                it.unsubscribe()
                                SupabaseClient.client.realtime.removeChannel(it)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error limpiando canal: ${e.message}")
                            }
                        }

                        // Crear canal único
                        val channelId = "seller_orders_${System.currentTimeMillis()}"
                        realtimeChannel = SupabaseClient.client.realtime.channel(channelId)

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
                                    when (action) {
                                        is PostgresAction.Insert -> {
                                            val record = action.record as? JsonObject
                                            val colmadoId = record?.get("colmado_id")?.jsonPrimitive?.content
                                            val orderNumber = record?.get("order_number")?.jsonPrimitive?.content
                                            Log.d(TAG, "  🔍 INSERT - order_id: $orderId, colmado_id: $colmadoId, order_number: $orderNumber, esperado: $currentColmadoId")
                                            
                                            if (colmadoId == currentColmadoId) {
                                                Log.d(TAG, "  ✅ Nuevo pedido para el colmado, recargando...")
                                                loadOrders()
                                            } else {
                                                Log.d(TAG, "  ⏭️ Pedido de otro colmado")
                                            }
                                        }
                                        is PostgresAction.Update -> {
                                            val record = action.record as? JsonObject
                                            val newColmadoId = record?.get("colmado_id")?.jsonPrimitive?.content

                                            val orderExists = _uiState.value.orders.any { it.order.id == orderId }
                                            Log.d(TAG, "  🔍 UPDATE - order_id: $orderId, colmado_id: $newColmadoId, existeEnLista: $orderExists, esperado: $currentColmadoId")

                                            // Verificar si pertenece al colmado actual (por el colmado_id en el evento)
                                            val belongsToColmado = newColmadoId == currentColmadoId

                                            if (belongsToColmado) {
                                                Log.d(TAG, "  ✅ El pedido $orderId pertenece al colmado (por colmado_id), recargando...")
                                                loadOrders()
                                            } else if (orderExists) {
                                                Log.d(TAG, "  ✅ El pedido $orderId está en nuestra lista, recargando...")
                                                loadOrders()
                                            } else {
                                                Log.d(TAG, "  ⏭️ Pedido no pertenece al colmado actual")
                                            }
                                        }
                                        is PostgresAction.Delete -> {
                                            val orderExists = _uiState.value.orders.any { it.order.id == orderId }
                                            if (orderExists) {
                                                Log.d(TAG, "  ✅ El pedido $orderId fue eliminado, recargando...")
                                                loadOrders()
                                            } else {
                                                Log.d(TAG, "  ⏭️ Pedido no pertenece al colmado actual")
                                            }
                                        }
                                        else -> {
                                            Log.d(TAG, "  ⏭️ Pedido no pertenece al colmado actual")
                                        }
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
                    }
                    is SellerRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error obteniendo colmado: ${colmadoResult.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en startRealtimeSubscription: ${e.message}", e)
            }
        }
    }

    private fun extractColmadoId(record: Any?): String? {
        return try {
            when (record) {
                is JsonObject -> {
                    record["colmado_id"]?.jsonPrimitive?.content
                }
                is Map<*, *> -> {
                    record["colmado_id"] as? String
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo colmado_id: ${e.message}")
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
                    SupabaseClient.client.realtime.removeChannel(channel)
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

    fun loadAvailableDeliveries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDeliveries = true) }

            try {
                orderRepository.getAvailableDeliveries().fold(
                    onSuccess = { deliveries ->
                        _uiState.update {
                            it.copy(
                                availableDeliveries = deliveries,
                                isLoadingDeliveries = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoadingDeliveries = false,
                                errorMessage = error.message ?: "Error al cargar deliveries"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDeliveries = false,
                        errorMessage = e.message ?: "Error inesperado"
                    )
                }
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.orders.isEmpty()
            if (isFirstLoad) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                Log.d(TAG, "📦 Cargando pedidos del colmado...")

                orderRepository.getColmadoOrders().fold(
                    onSuccess = { orders ->
                        val validOrders = orders.filter { orderWithDetails ->
                            orderWithDetails.order.status != OrderStatus.PENDING &&
                                    orderWithDetails.order.status != OrderStatus.PAYMENT_PROCESSING &&
                                    orderWithDetails.order.status != OrderStatus.CANCELLED &&
                                    orderWithDetails.order.status != OrderStatus.REFUNDED
                        }

                        Log.d(TAG, "✅ Órdenes válidas del colmado: ${validOrders.size}")

                        _uiState.update {
                            it.copy(
                                orders = validOrders.sortedByDescending { order ->
                                    order.order.createdAt
                                },
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error cargando pedidos: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al cargar pedidos"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando pedidos: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error inesperado"
                    )
                }
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            try {
                orderRepository.updateOrderStatus(orderId, newStatus).fold(
                    onSuccess = {
                        _uiState.update { state ->
                            state.copy(
                                orders = state.orders.map { orderWithDetails ->
                                    if (orderWithDetails.order.id == orderId) {
                                        orderWithDetails.copy(
                                            order = orderWithDetails.order.copy(status = newStatus)
                                        )
                                    } else {
                                        orderWithDetails
                                    }
                                },
                                successMessage = "Estado actualizado a ${newStatus.toDisplayString()}"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                errorMessage = error.message ?: "Error al actualizar estado"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Error inesperado"
                    )
                }
            }
        }
    }

    fun assignDeliveryToOrder(orderId: String, deliveryUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                orderRepository.assignDeliveryToOrder(orderId, deliveryUserId).fold(
                    onSuccess = {
                        loadOrders()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Delivery asignado correctamente"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al asignar delivery"
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

    fun selectOrder(orderId: String) {
        val order = _uiState.value.orders.find { it.order.id == orderId }
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun getOrderById(orderId: String): OrderWithDetails? {
        return _uiState.value.orders.find { it.order.id == orderId }
    }

    fun refreshOrder(orderId: String) {
        loadOrders()
    }
}