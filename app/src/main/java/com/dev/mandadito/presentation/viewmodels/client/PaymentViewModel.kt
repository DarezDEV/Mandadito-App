package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.CreateOrderResponse
import com.dev.mandadito.data.repository.PaymentRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar el flujo de pagos
 */
class PaymentViewModel(context: Context) : ViewModel() {

    private val repository = PaymentRepository(context)
    private val TAG = "PaymentViewModel"

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        // Inicializar Stripe al crear el ViewModel
        initializeStripe()
    }

    /**
     * Inicializa Stripe SDK
     */
    private fun initializeStripe() {
        viewModelScope.launch {
            Log.d(TAG, "🔧 Inicializando Stripe...")

            repository.initializeStripe()
                .onSuccess {
                    Log.d(TAG, "✅ Stripe inicializado")
                    _uiState.update { it.copy(isStripeInitialized = true) }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error inicializando Stripe: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isStripeInitialized = false,
                            errorMessage = "Error inicializando pagos: ${error.message}"
                        )
                    }
                }
        }
    }

    /**
     * Crea una orden y prepara el pago
     *
     * @param userId ID del usuario
     * @param cartId ID del carrito
     * @param addressId ID de la dirección
     * @param deliveryFee Costo de delivery
     * @param customerNotes Notas del cliente
     */
    fun createOrder(
        userId: String,
        cartId: String,
        addressId: String,
        deliveryFee: Double,
        customerNotes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            Log.d(TAG, "📦 Creando orden...")

            repository.createOrder(
                userId = userId,
                cartId = cartId,
                addressId = addressId,
                deliveryFee = deliveryFee,
                customerNotes = customerNotes
            )
                .onSuccess { orderResponse ->
                    Log.d(TAG, "✅ Orden creada: ${orderResponse.orderNumber}")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            orderResponse = orderResponse,
                            clientSecret = orderResponse.clientSecret,
                            isReadyForPayment = true
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error creando orden: ${error.message}")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error creando orden"
                        )
                    }
                }
        }
    }

    /**
     * Obtiene la configuración de PaymentSheet
     */
    fun getPaymentSheetConfiguration(): PaymentSheet.Configuration {
        return repository.createPaymentSheetConfiguration(
            clientSecret = _uiState.value.clientSecret ?: "",
            merchantDisplayName = "Mandadito"
        )
    }

    /**
     * Maneja el resultado del PaymentSheet
     */
    fun handlePaymentResult(result: PaymentSheetResult) {
        repository.handlePaymentSheetResult(
            result = result,
            onSuccess = {
                Log.d(TAG, "✅ Pago exitoso")
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.SUCCESS,
                        successMessage = "¡Pago exitoso! Tu pedido está siendo procesado."
                    )
                }
            },
            onFailure = { errorMessage ->
                Log.e(TAG, "❌ Pago falló: $errorMessage")
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.FAILED,
                        errorMessage = "Pago falló: $errorMessage"
                    )
                }
            },
            onCancelled = {
                Log.d(TAG, "❌ Pago cancelado")
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.CANCELLED,
                        errorMessage = "Pago cancelado"
                    )
                }
            }
        )
    }

    /**
     * Limpia los mensajes de error
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Limpia los mensajes de éxito
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Resetea el estado para un nuevo pago
     */
    fun resetPaymentState() {
        _uiState.update {
            PaymentUiState(isStripeInitialized = it.isStripeInitialized)
        }
    }
}

/**
 * Estado de la UI de pagos
 */
data class PaymentUiState(
    val isStripeInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val isReadyForPayment: Boolean = false,

    // Datos de la orden
    val orderResponse: CreateOrderResponse? = null,
    val clientSecret: String? = null,

    // Estado del pago
    val paymentStatus: PaymentStatus = PaymentStatus.IDLE,

    // Mensajes
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Estados del proceso de pago
 */
enum class PaymentStatus {
    IDLE,       // Sin actividad
    PROCESSING, // Procesando
    SUCCESS,    // Exitoso
    FAILED,     // Falló
    CANCELLED   // Cancelado
}
