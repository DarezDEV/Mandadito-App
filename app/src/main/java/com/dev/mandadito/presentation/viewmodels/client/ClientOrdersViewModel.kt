package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Order
import com.dev.mandadito.data.models.OrderItem
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.data.models.ColmadoInfo
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

data class ClientOrdersUiState(
    val orders: List<OrderWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ClientOrdersViewModel(context: Context) : ViewModel() {

    private val supabase = SupabaseClient.client

    private val _uiState = MutableStateFlow(ClientOrdersUiState())
    val uiState: StateFlow<ClientOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("Usuario no autenticado")

                // Modelos de datos locales para deserialización
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

                // Obtener todas las órdenes y filtrar por userId en Kotlin
                val allOrders = supabase.from("orders").select().decodeList<OrderRecord>()
                val userOrders = allOrders.filter { it.user_id == userId }

                // Obtener todos los items y colmados de una vez
                val allOrderItems = supabase.from("order_items").select().decodeList<OrderItemRecord>()
                val allColmados = supabase.from("colmados").select().decodeList<ColmadoRecord>()

                // Construir OrderWithDetails
                val ordersWithDetails = userOrders.map { orderRecord ->
                    // Filtrar items de esta orden
                    val orderItems = allOrderItems.filter { it.order_id == orderRecord.id }

                    // Buscar colmado
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

                // Filtrar solo pedidos válidos (excluir PENDING, PAYMENT_PROCESSING, CANCELLED y REFUNDED)
                val validOrders = ordersWithDetails.filter { orderWithDetails ->
                    orderWithDetails.order.status != OrderStatus.PENDING &&
                    orderWithDetails.order.status != OrderStatus.PAYMENT_PROCESSING &&
                    orderWithDetails.order.status != OrderStatus.CANCELLED &&
                    orderWithDetails.order.status != OrderStatus.REFUNDED
                }

                _uiState.update {
                    it.copy(
                        orders = validOrders.sortedByDescending { orderWithDetails ->
                            orderWithDetails.order.createdAt
                        },
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
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
}
