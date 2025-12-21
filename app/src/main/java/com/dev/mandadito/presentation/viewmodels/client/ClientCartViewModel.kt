package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.CartWithItems
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.RetryPolicy
import com.dev.mandadito.data.network.RetryState
import com.dev.mandadito.data.repository.CartRepository
import com.dev.mandadito.presentation.viewmodels.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientCartUiState(
    val cartsState: UiState<List<CartWithItems>> = UiState.Idle,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isConnected: Boolean = true
)

class ClientCartViewModel(context: Context) : ViewModel() {

    private val cartRepository = CartRepository(context)
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val TAG = "ClientCartViewModel"

    private val _uiState = MutableStateFlow(ClientCartUiState())
    val uiState: StateFlow<ClientCartUiState> = _uiState.asStateFlow()

    init {
        // Observar cambios de conectividad
        viewModelScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                Log.d(TAG, "🌐 Conexión: ${if (isConnected) "ONLINE" else "OFFLINE"}")
                _uiState.update { it.copy(isConnected = isConnected) }

                // Si vuelve la conexión y hay error, reintentar
                if (isConnected && _uiState.value.cartsState is UiState.Error) {
                    Log.d(TAG, "🔄 Conexión restaurada, recargando...")
                    loadCarts()
                }
            }
        }
        loadCarts()
    }

    /**
     * Carga todos los carritos del usuario
     */
    fun loadCarts() {
        viewModelScope.launch {
            Log.d(TAG, "🛒 Cargando carritos...")

            // Usar RetryPolicy para reintentos automáticos
            RetryPolicy.retryWithBackoff(
                isConnected = connectivityMonitor.isCurrentlyConnected(),
                operation = { cartRepository.getUserCarts() }
            ).collect { retryState ->
                when (retryState) {
                    is RetryState.Loading -> {
                        _uiState.update { it.copy(cartsState = UiState.Loading) }
                    }

                    is RetryState.Success -> {
                        when (val result = retryState.data) {
                            is CartRepository.Result.Success -> {
                                // Filtrar carritos vacíos
                                val nonEmptyCarts = result.data.filter { it.items.isNotEmpty() }
                                Log.d(TAG, "✅ ${nonEmptyCarts.size} carritos con items cargados")
                                _uiState.update {
                                    it.copy(
                                        cartsState = UiState.Success(
                                            data = nonEmptyCarts,
                                            isFromCache = result.isFromCache,
                                            cacheTimestamp = result.cacheTimestamp
                                        )
                                    )
                                }
                            }
                            is CartRepository.Result.Error -> {
                                Log.e(TAG, "❌ Error: ${result.message}")
                                _uiState.update {
                                    it.copy(cartsState = UiState.Error(result.message))
                                }
                            }
                        }
                    }

                    is RetryState.Retrying -> {
                        Log.d(TAG, "🔄 Reintentando... Intento #${retryState.attempt}")
                        _uiState.update {
                            it.copy(
                                cartsState = UiState.Retrying(
                                    attempt = retryState.attempt,
                                    nextRetryInSeconds = retryState.nextRetryInSeconds
                                )
                            )
                        }
                    }

                    is RetryState.Error -> {
                        Log.e(TAG, "❌ Error cargando carritos: ${retryState.message}")
                        _uiState.update {
                            it.copy(cartsState = UiState.Error(retryState.message))
                        }
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
            // Actualización optimista: actualizar UI inmediatamente
            val newQuantity = currentQuantity + 1
            updateItemQuantityLocally(itemId, newQuantity)

            // Luego actualizar en el servidor
            when (val result = cartRepository.incrementQuantity(itemId, currentQuantity)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Cantidad incrementada en servidor")
                }
                is CartRepository.Result.Error -> {
                    // Si falla, revertir y mostrar error
                    Log.e(TAG, "❌ Error incrementando: ${result.message}")
                    updateItemQuantityLocally(itemId, currentQuantity) // Revertir
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    /**
     * Decrementa la cantidad de un producto
     */
    fun decrementQuantity(itemId: String, currentQuantity: Int) {
        viewModelScope.launch {
            if (currentQuantity <= 1) {
                // Si es 1, eliminar el item
                removeItem(itemId)
                return@launch
            }

            // Actualización optimista: actualizar UI inmediatamente
            val newQuantity = currentQuantity - 1
            updateItemQuantityLocally(itemId, newQuantity)

            // Luego actualizar en el servidor
            when (val result = cartRepository.decrementQuantity(itemId, currentQuantity)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Cantidad decrementada en servidor")
                }
                is CartRepository.Result.Error -> {
                    // Si falla, revertir y mostrar error
                    Log.e(TAG, "❌ Error decrementando: ${result.message}")
                    updateItemQuantityLocally(itemId, currentQuantity) // Revertir
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    /**
     * Actualiza la cantidad de un item localmente (sin recargar desde BD)
     */
    private fun updateItemQuantityLocally(itemId: String, newQuantity: Int) {
        _uiState.update { state ->
            val cartsState = state.cartsState
            if (cartsState is UiState.Success) {
                val updatedCarts = cartsState.data.map { cart ->
                    val updatedItems = cart.items.map { item ->
                        if (item.cartItemId == itemId) {
                            item.copy(quantity = newQuantity)
                        } else {
                            item
                        }
                    }

                    // Recalcular subtotal y totalProducts basado en los items actualizados
                    val newSubtotal = updatedItems.sumOf { it.price * it.quantity }
                    val newTotalProducts = updatedItems.sumOf { it.quantity }

                    cart.copy(
                        items = updatedItems,
                        summary = cart.summary.copy(
                            subtotal = newSubtotal,
                            totalProducts = newTotalProducts
                        )
                    )
                }
                state.copy(
                    cartsState = UiState.Success(
                        data = updatedCarts,
                        isFromCache = cartsState.isFromCache,
                        cacheTimestamp = cartsState.cacheTimestamp
                    )
                )
            } else {
                state
            }
        }
    }

    /**
     * Elimina un producto del carrito
     */
    fun removeItem(itemId: String) {
        viewModelScope.launch {
            // Actualización optimista: eliminar del UI inmediatamente
            removeItemLocally(itemId)

            when (val result = cartRepository.removeFromCart(itemId)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto eliminado del servidor")
                    _uiState.update { it.copy(successMessage = "Producto eliminado del carrito") }
                }
                is CartRepository.Result.Error -> {
                    // Si falla, recargar para restaurar
                    Log.e(TAG, "❌ Error eliminando: ${result.message}")
                    _uiState.update { it.copy(errorMessage = result.message) }
                    loadCarts()
                }
            }
        }
    }

    /**
     * Elimina un item localmente y filtra carritos vacíos
     */
    private fun removeItemLocally(itemId: String) {
        _uiState.update { state ->
            val cartsState = state.cartsState
            if (cartsState is UiState.Success) {
                val updatedCarts = cartsState.data.mapNotNull { cart ->
                    val updatedItems = cart.items.filter { it.cartItemId != itemId }
                    if (updatedItems.isEmpty()) {
                        null // Eliminar carrito si quedó vacío
                    } else {
                        cart.copy(items = updatedItems)
                    }
                }
                state.copy(
                    cartsState = UiState.Success(
                        data = updatedCarts,
                        isFromCache = cartsState.isFromCache,
                        cacheTimestamp = cartsState.cacheTimestamp
                    )
                )
            } else {
                state
            }
        }
    }

    /**
     * Vacía un carrito completo
     */
    fun clearCart(cartId: String) {
        viewModelScope.launch {
            // Actualización optimista: eliminar carrito del UI inmediatamente
            _uiState.update { state ->
                val cartsState = state.cartsState
                if (cartsState is UiState.Success) {
                    state.copy(
                        cartsState = UiState.Success(
                            data = cartsState.data.filter { it.id != cartId },
                            isFromCache = cartsState.isFromCache,
                            cacheTimestamp = cartsState.cacheTimestamp
                        )
                    )
                } else {
                    state
                }
            }

            when (val result = cartRepository.clearCart(cartId)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Carrito vaciado del servidor")
                    _uiState.update { it.copy(successMessage = "Carrito vaciado") }
                }
                is CartRepository.Result.Error -> {
                    // Si falla, recargar para restaurar
                    Log.e(TAG, "❌ Error vaciando carrito: ${result.message}")
                    loadCarts()
                }
            }
        }
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
        val cartsState = _uiState.value.cartsState
        return if (cartsState is UiState.Success) {
            cartsState.data.sumOf { it.total }
        } else {
            0.0
        }
    }

    /**
     * Obtiene el número total de productos en todos los carritos
     */
    fun getTotalItemsCount(): Int {
        val cartsState = _uiState.value.cartsState
        return if (cartsState is UiState.Success) {
            cartsState.data.sumOf { it.items.sumOf { item -> item.quantity } }
        } else {
            0
        }
    }
}
