package com.dev.mandadito.presentation.viewmodels.seller


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SellerOrdersUiState(
    val orders: List<OrderWithDetails> = emptyList(),
    val availableDeliveries: List<com.dev.mandadito.data.repository.DeliveryUser> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingDeliveries: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SellerOrdersViewModel(context: Context) : ViewModel() {

    private val orderRepository = OrderRepository(context)

    private val _uiState = MutableStateFlow(SellerOrdersUiState())
    val uiState: StateFlow<SellerOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Cargar pedidos del colmado
                orderRepository.getColmadoOrders().fold(
                    onSuccess = { orders ->
                        // Filtrar solo pedidos válidos (excluir PENDING, PAYMENT_PROCESSING, CANCELLED, REFUNDED)
                        val validOrders = orders.filter { orderWithDetails ->
                            orderWithDetails.order.status != OrderStatus.PENDING &&
                            orderWithDetails.order.status != OrderStatus.PAYMENT_PROCESSING &&
                            orderWithDetails.order.status != OrderStatus.CANCELLED &&
                            orderWithDetails.order.status != OrderStatus.REFUNDED
                        }

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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al cargar pedidos"
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
                        // Recargar órdenes para obtener los datos actualizados con el código de verificación
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
}