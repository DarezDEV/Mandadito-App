package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.CartWithItems
import com.dev.mandadito.data.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientCartUiState(
    val carts: List<CartWithItems> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ClientCartViewModel(context: Context) : ViewModel() {

    private val cartRepository = CartRepository(context)
    private val TAG = "ClientCartViewModel"

    private val _uiState = MutableStateFlow(ClientCartUiState())
    val uiState: StateFlow<ClientCartUiState> = _uiState.asStateFlow()

    init {
        loadCarts()
    }

    /**
     * Carga todos los carritos del usuario
     */
    fun loadCarts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🛒 Cargando carritos...")

            when (val result = cartRepository.getUserCarts()) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} carritos cargados")
                    _uiState.update {
                        it.copy(
                            carts = result.data,
                            isLoading = false
                        )
                    }
                }
                is CartRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando carritos: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Incrementa la cantidad de un producto
     */
    fun incrementQuantity(itemId: String, currentQuantity: Int) {
        viewModelScope.launch {
            when (val result = cartRepository.incrementQuantity(itemId, currentQuantity)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Cantidad incrementada")
                    loadCarts() // Recargar carritos
                }
                is CartRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    /**
     * Decrementa la cantidad de un producto
     */
    fun decrementQuantity(itemId: String, currentQuantity: Int) {
        viewModelScope.launch {
            when (val result = cartRepository.decrementQuantity(itemId, currentQuantity)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Cantidad decrementada")
                    loadCarts() // Recargar carritos
                }
                is CartRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    /**
     * Elimina un producto del carrito
     */
    fun removeItem(itemId: String) {
        viewModelScope.launch {
            when (val result = cartRepository.removeFromCart(itemId)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto eliminado")
                    _uiState.update { it.copy(successMessage = "Producto eliminado del carrito") }
                    loadCarts() // Recargar carritos
                }
                is CartRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    /**
     * Vacía un carrito completo
     */
    fun clearCart(cartId: String) {
        viewModelScope.launch {
            when (val result = cartRepository.clearCart(cartId)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Carrito vaciado")
                    _uiState.update { it.copy(successMessage = "Carrito vaciado") }
                    loadCarts() // Recargar carritos
                }
                is CartRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Limpia el mensaje de éxito
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Obtiene el total general de todos los carritos
     */
    fun getTotalAmount(): Double {
        return _uiState.value.carts.sumOf { it.total }
    }

    /**
     * Obtiene el número total de productos en todos los carritos
     */
    fun getTotalItemsCount(): Int {
        return _uiState.value.carts.sumOf { it.items.sumOf { item -> item.quantity } }
    }
}
