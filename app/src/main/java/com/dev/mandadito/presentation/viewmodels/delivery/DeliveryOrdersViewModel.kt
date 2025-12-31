package com.dev.mandadito.presentation.viewmodels.delivery

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.data.repository.OrderRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeliveryOrdersUiState(
    val orders: List<OrderWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isVerificationSuccess: Boolean = false
)

class DeliveryOrdersViewModel(context: Context) : ViewModel() {

    private val orderRepository = OrderRepository(context)
    private val supabase = SupabaseClient.client
    private val TAG = "DeliveryOrdersViewModel"

    private val _uiState = MutableStateFlow(DeliveryOrdersUiState())
    val uiState: StateFlow<DeliveryOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario no autenticado")

                Log.d(TAG, "📦 Cargando órdenes del delivery: $userId")

                orderRepository.getDeliveryOrders(userId).fold(
                    onSuccess = { orders ->
                        // Filtrar solo pedidos en delivery o entregados
                        // No mostrar pedidos pendientes o en preparación
                        val deliveryOrders = orders.filter { orderWithDetails ->
                            orderWithDetails.order.status == com.dev.mandadito.data.models.OrderStatus.IN_DELIVERY ||
                            orderWithDetails.order.status == com.dev.mandadito.data.models.OrderStatus.DELIVERED
                        }

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
}
