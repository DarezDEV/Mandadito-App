package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.data.repository.CartRepository
import com.dev.mandadito.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientProductDetailUiState(
    val product: ProductWithCategories? = null,
    val isLoading: Boolean = true,
    val isAddingToCart: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val quantity: Int = 1
)

class ClientProductDetailViewModel(context: Context, productId: String) : ViewModel() {

    private val productRepository = ProductRepository(context)
    private val cartRepository = CartRepository(context)
    private val TAG = "ClientProductDetailVM"

    private val _uiState = MutableStateFlow(ClientProductDetailUiState())
    val uiState: StateFlow<ClientProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadProduct(productId)
    }

    private fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔥 Cargando producto: $productId")

            when (val result = productRepository.getProductById(productId)) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto cargado: ${result.data.name}")
                    _uiState.update {
                        it.copy(
                            product = result.data,
                            isLoading = false
                        )
                    }
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando producto: ${result.message}")
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
     * Agrega el producto al carrito
     */
    fun addToCart() {
        viewModelScope.launch {
            val productId = _uiState.value.product?.id ?: return@launch
            val quantity = _uiState.value.quantity

            _uiState.update { it.copy(isAddingToCart = true, error = null) }

            Log.d(TAG, "🛒 Agregando producto al carrito: $productId (cantidad: $quantity)")

            // Agregar la cantidad especificada al carrito
            repeat(quantity) {
                when (val result = cartRepository.addToCart(productId)) {
                    is CartRepository.Result.Success -> {
                        // Continuar
                    }
                    is CartRepository.Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isAddingToCart = false,
                                error = result.message
                            )
                        }
                        return@launch
                    }
                }
            }

            Log.d(TAG, "✅ Producto agregado exitosamente")
            _uiState.update {
                it.copy(
                    isAddingToCart = false,
                    successMessage = "Producto agregado al carrito",
                    quantity = 1 // Resetear cantidad
                )
            }
        }
    }

    fun setQuantity(quantity: Int) {
        if (quantity >= 1) {
            _uiState.update { it.copy(quantity = quantity) }
        }
    }

    fun incrementQuantity() {
        _uiState.update { it.copy(quantity = it.quantity + 1) }
    }

    fun decrementQuantity() {
        if (_uiState.value.quantity > 1) {
            _uiState.update { it.copy(quantity = it.quantity - 1) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
