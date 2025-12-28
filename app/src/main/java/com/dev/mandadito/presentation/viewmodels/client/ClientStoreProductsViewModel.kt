package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.RetryPolicy
import com.dev.mandadito.data.network.RetryState
import com.dev.mandadito.data.repository.CartRepository
import com.dev.mandadito.data.repository.CategoryRepository
import com.dev.mandadito.data.repository.ProductRepository
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.presentation.viewmodels.common.dataOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientStoreProductsUiState(
    val productsState: UiState<List<ProductWithCategories>> = UiState.Idle,
    val categoriesState: UiState<List<Category>> = UiState.Idle,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val colmadoName: String = "",
    val colmadoId: String? = null,
    val successMessage: String? = null,
    val isConnected: Boolean = true
)

class ClientStoreProductsViewModel(context: Context) : ViewModel() {

    private val productRepository = ProductRepository(context)
    private val categoryRepository = CategoryRepository(context)
    private val cartRepository = CartRepository(context)
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val TAG = "ClientStoreProductsVM"

    private val _uiState = MutableStateFlow(ClientStoreProductsUiState())
    val uiState: StateFlow<ClientStoreProductsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                Log.d(TAG, "🌐 Conexión: ${if (isConnected) "ONLINE" else "OFFLINE"}")
                _uiState.update { it.copy(isConnected = isConnected) }

                if (isConnected && _uiState.value.productsState is UiState.Error) {
                    Log.d(TAG, "🔄 Conexión restaurada, recargando datos...")
                    loadProducts()
                }
            }
        }
    }

    fun initialize(colmadoId: String, colmadoName: String = "") {
        Log.d(TAG, "🪙 Inicializando con colmado: $colmadoId")
        _uiState.update { it.copy(colmadoId = colmadoId, colmadoName = colmadoName) }
        loadProducts()
        loadCategories()
    }

    fun loadProducts() {
        viewModelScope.launch {
            val colmadoId = _uiState.value.colmadoId
            if (colmadoId == null) {
                Log.w(TAG, "⚠️ No se ha inicializado el colmadoId")
                return@launch
            }

            Log.d(TAG, "🔥 Cargando productos del colmado: $colmadoId")

            // Usar RetryPolicy para reintentos automáticos
            RetryPolicy.retryWithBackoff(
                isConnected = connectivityMonitor.isCurrentlyConnected(),
                operation = {
                    // Obtener todos los productos y filtrar por colmadoId
                    when (val result = productRepository.getActiveProducts()) {
                        is ProductRepository.Result.Success -> {
                            val filteredProducts = result.data.filter { it.colmadoId == colmadoId }
                            ProductRepository.Result.Success(filteredProducts)
                        }
                        is ProductRepository.Result.Error -> result
                    }
                }
            ).collect { retryState ->
                when (retryState) {
                    is RetryState.Loading -> {
                        _uiState.update {
                            it.copy(productsState = UiState.Loading)
                        }
                    }

                    is RetryState.Success -> {
                        val result = retryState.data
                        when (result) {
                            is ProductRepository.Result.Success -> {
                                Log.d(TAG, "✅ ${result.data.size} productos cargados")

                                val state = UiState.Success(
                                    data = result.data,
                                    isFromCache = false,
                                    cacheTimestamp = null
                                )

                                _uiState.update { it.copy(productsState = state) }
                            }

                            is ProductRepository.Result.Error -> {
                                Log.e(TAG, "❌ Error: ${result.message}")
                                _uiState.update {
                                    it.copy(
                                        productsState = UiState.Error(
                                            message = result.message,
                                            canRetry = true
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is RetryState.Error -> {
                        Log.e(TAG, "❌ Error definitivo: ${retryState.message}")
                        _uiState.update {
                            it.copy(
                                productsState = UiState.Error(
                                    message = retryState.message,
                                    canRetry = false
                                )
                            )
                        }
                    }

                    is RetryState.Retrying -> {
                        Log.d(TAG, "🔄 Reintentando... Intento #${retryState.attempt}")
                        _uiState.update {
                            it.copy(
                                productsState = UiState.Retrying(
                                    attempt = retryState.attempt,
                                    nextRetryInSeconds = retryState.nextRetryInSeconds
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val colmadoId = _uiState.value.colmadoId
            if (colmadoId == null) {
                Log.w(TAG, "⚠️ No se ha inicializado el colmadoId")
                return@launch
            }

            Log.d(TAG, "🎭 Cargando categorías del colmado: $colmadoId")

            // Obtener todas las categorías y filtrar por colmadoId
            when (val result = categoryRepository.getActiveCategories()) {
                is CategoryRepository.Result.Success -> {
                    val filteredCategories = result.data.filter { it.colmadoId == colmadoId }
                    Log.d(TAG, "✅ ${filteredCategories.size} categorías cargadas")
                    _uiState.update {
                        it.copy(
                            categoriesState = UiState.Success(
                                data = filteredCategories,
                                isFromCache = false,
                                cacheTimestamp = null
                            )
                        )
                    }
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando categorías: ${result.message}")
                    _uiState.update {
                        it.copy(categoriesState = UiState.Error(result.message))
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        Log.d(TAG, "🔍 Búsqueda actualizada: $query")
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(categoryId: String?) {
        Log.d(TAG, "🎭 Categoría seleccionada: ${categoryId ?: "Todas"}")
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    val filteredProducts: List<ProductWithCategories>
        get() {
            val productsState = _uiState.value.productsState
            val products = productsState.dataOrNull() ?: emptyList()

            val query = _uiState.value.searchQuery.lowercase()
            val categoryId = _uiState.value.selectedCategoryId

            return products.filter { product ->
                val matchesSearch = query.isBlank() ||
                        product.name.lowercase().contains(query) ||
                        product.description?.lowercase()?.contains(query) == true

                val matchesCategory = categoryId == null ||
                        product.categories.any { it.id == categoryId }

                matchesSearch && matchesCategory
            }
        }

    fun addToCart(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(successMessage = "Producto agregado al carrito") }

            Log.d(TAG, "🛒 Agregando producto al carrito: $productId")

            when (val result = cartRepository.addToCart(productId)) {
                is CartRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto agregado al servidor exitosamente")
                }
                is CartRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error agregando al carrito: ${result.message}")
                    _uiState.update {
                        it.copy(successMessage = null)
                    }
                }
            }
        }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}