package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.repository.DeliveryUser
import com.dev.mandadito.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SellerOrdersUiState(
    val orders: List<OrderWithDetails> = emptyList(),
    val availableDeliveries: List<DeliveryUser> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingDeliveries: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class SellerOrdersViewModel(context: Context) : ViewModel() {

    private val orderRepository = OrderRepository(context)
    private val TAG = "SellerOrdersViewModel"

    private val _uiState = MutableStateFlow(SellerOrdersUiState())
    val uiState: StateFlow<SellerOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            Log.d(TAG, "📋 Cargando órdenes del colmado")

            orderRepository.getColmadoOrders()
                .onSuccess { orders ->
                    Log.d(TAG, "✅ ${orders.size} órdenes cargadas")
                    _uiState.update {
                        it.copy(
                            orders = orders,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error: ${error.message}")
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Error al cargar órdenes",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            Log.d(TAG, "🔄 Actualizando estado de orden $orderId a ${newStatus.name}")

            orderRepository.updateOrderStatus(orderId, newStatus)
                .onSuccess {
                    Log.d(TAG, "✅ Estado actualizado correctamente")
                    _uiState.update {
                        it.copy(
                            successMessage = "Estado actualizado a ${newStatus.toDisplayString()}",
                            isLoading = false
                        )
                    }
                    // Recargar órdenes para reflejar el cambio
                    loadOrders()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error: ${error.message}")
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Error al actualizar estado",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun loadAvailableDeliveries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDeliveries = true) }

            Log.d(TAG, "🚚 Cargando deliveries disponibles")

            orderRepository.getAvailableDeliveries()
                .onSuccess { deliveries ->
                    Log.d(TAG, "✅ ${deliveries.size} deliveries cargados")
                    _uiState.update {
                        it.copy(
                            availableDeliveries = deliveries,
                            isLoadingDeliveries = false
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error: ${error.message}")
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Error al cargar deliveries",
                            isLoadingDeliveries = false
                        )
                    }
                }
        }
    }

    fun assignDeliveryToOrder(orderId: String, deliveryUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            Log.d(TAG, "🚚 Asignando delivery a orden $orderId")

            orderRepository.assignDeliveryToOrder(orderId, deliveryUserId)
                .onSuccess {
                    Log.d(TAG, "✅ Delivery asignado correctamente")
                    _uiState.update {
                        it.copy(
                            successMessage = "Delivery asignado correctamente",
                            isLoading = false
                        )
                    }
                    // Recargar órdenes para reflejar el cambio
                    loadOrders()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error: ${error.message}")
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Error al asignar delivery",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
