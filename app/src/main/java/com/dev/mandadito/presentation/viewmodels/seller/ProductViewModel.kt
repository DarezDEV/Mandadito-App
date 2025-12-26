package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.RetryPolicy
import com.dev.mandadito.data.network.RetryState
import com.dev.mandadito.data.repository.CategoryRepository
import com.dev.mandadito.data.repository.ProductRepository
import com.dev.mandadito.data.repository.SellerRepository
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.utils.SharedPreferenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductUiState(
    val productsState: UiState<List<ProductWithCategories>> = UiState.Idle,
    val categories: List<com.dev.mandadito.data.models.Category> = emptyList(),
    val successMessage: String? = null,
    val searchQuery: String = "",
    val showActiveOnly: Boolean = false,
    val selectedCategoryFilter: String? = null,
    val isConnected: Boolean = true
)

class ProductViewModel(context: Context) : ViewModel() {

    private val productRepository = ProductRepository(context)
    private val categoryRepository = CategoryRepository(context)
    private val sellerRepository = SellerRepository(context)
    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val TAG = "ProductViewModel"

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        // Observar cambios de conectividad
        viewModelScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                Log.d(TAG, "🌐 Conexión: ${if (isConnected) "ONLINE" else "OFFLINE"}")
                _uiState.update { it.copy(isConnected = isConnected) }

                // Si vuelve la conexión y hay error, reintentar
                if (isConnected && _uiState.value.productsState is UiState.Error) {
                    Log.d(TAG, "🔄 Conexión restaurada, recargando...")
                    loadProducts()
                }
            }
        }
        loadProducts()
        loadCategories()
    }

    fun loadProducts(showLoading: Boolean = true) {
        viewModelScope.launch {
            Log.d(TAG, "🔥 Cargando productos...")

            // Obtener colmado_id
            val colmadoId = getColmadoId() ?: run {
                _uiState.update {
                    it.copy(productsState = UiState.Error("No se pudo obtener el colmado"))
                }
                return@launch
            }

            // Usar RetryPolicy para reintentos automáticos
            RetryPolicy.retryWithBackoff(
                isConnected = connectivityMonitor.isCurrentlyConnected(),
                operation = { productRepository.getActiveProducts(colmadoId) }
            ).collect { retryState ->
                when (retryState) {
                    is RetryState.Loading -> {
                        _uiState.update { it.copy(productsState = UiState.Loading) }
                    }

                    is RetryState.Success -> {
                        when (val result = retryState.data) {
                            is ProductRepository.Result.Success -> {
                                Log.d(TAG, "✅ ${result.data.size} productos cargados")
                                _uiState.update {
                                    it.copy(
                                        productsState = UiState.Success(
                                            data = result.data,
                                            isFromCache = result.isFromCache,
                                            cacheTimestamp = result.cacheTimestamp
                                        ),
                                        successMessage = if (!result.isFromCache) "Productos cargados" else null
                                    )
                                }
                            }
                            is ProductRepository.Result.Error -> {
                                Log.e(TAG, "❌ Error: ${result.message}")
                                _uiState.update {
                                    it.copy(productsState = UiState.Error(result.message))
                                }
                            }
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

                    is RetryState.Error -> {
                        Log.e(TAG, "❌ Error cargando productos: ${retryState.message}")
                        _uiState.update {
                            it.copy(productsState = UiState.Error(retryState.message))
                        }
                    }
                }
            }
        }
    }

    fun loadCategories(showLoading: Boolean = true) {
        viewModelScope.launch {
            // Obtener colmado_id del vendedor
            var colmadoId = sharedPrefsHelper.getColmadoId()

            // Si no está en SharedPreferences, obtener desde la base de datos
            if (colmadoId == null) {
                val userId = sharedPrefsHelper.getUserId()
                if (userId != null) {
                    when (val result = sellerRepository.getSellerColmadoId(userId)) {
                        is SellerRepository.Result.Success -> {
                            colmadoId = result.data
                            sharedPrefsHelper.saveColmadoId(colmadoId)
                        }
                        is SellerRepository.Result.Error -> {
                            Log.e(TAG, "❌ Error obteniendo colmado_id para categorías: ${result.message}")
                        }
                    }
                }
            }

            when (val result = categoryRepository.getActiveCategories(colmadoId)) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} categorías cargadas")
                    _uiState.update {
                        it.copy(categories = result.data)
                    }
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando categorías: ${result.message}")
                }
            }
        }
    }

    // ACTUALIZADO: Ahora recibe List<Uri> en lugar de Uri?
    fun createProduct(
        name: String,
        description: String? = null,
        price: Double,
        stock: Int = 0,
        minStock: Int = 0,
        imageUris: List<Uri> = emptyList(), // 👈 Lista de imágenes
        categoryIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(productsState = UiState.Loading) }

            Log.d(TAG, "🔷 Creando producto: $name con ${imageUris.size} imágenes")

            // Obtener colmado_id
            var colmadoId = sharedPrefsHelper.getColmadoId()

            // Si no está en SharedPreferences, obtener desde la base de datos
            if (colmadoId == null) {
                Log.d(TAG, "📦 Colmado_id no encontrado en SharedPreferences, obteniendo desde BD...")
                val userId = sharedPrefsHelper.getUserId()
                if (userId != null) {
                    when (val result = sellerRepository.getSellerColmadoId(userId)) {
                        is SellerRepository.Result.Success -> {
                            colmadoId = result.data
                            sharedPrefsHelper.saveColmadoId(colmadoId)
                            Log.d(TAG, "✅ Colmado_id obtenido y guardado: $colmadoId")
                        }
                        is SellerRepository.Result.Error -> {
                            Log.e(TAG, "❌ Error obteniendo colmado_id: ${result.message}")
                            _uiState.update {
                                it.copy(
                                    productsState = UiState.Error(
                                        "Error al obtener información del colmado: ${result.message}"
                                    )
                                )
                            }
                            return@launch
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            productsState = UiState.Error("No se pudo obtener el ID del usuario")
                        )
                    }
                    return@launch
                }
            }

            if (colmadoId == null) {
                _uiState.update {
                    it.copy(
                        productsState = UiState.Error("No tienes un colmado asignado. Contacta al administrador.")
                    )
                }
                return@launch
            }

            when (val result = productRepository.createProduct(
                colmadoId, name, description, price, stock, minStock, imageUris, categoryIds
            )) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto creado exitosamente")
                    _uiState.update {
                        it.copy(
                            productsState = UiState.Loading,
                            successMessage = "Producto creado: ${result.data.name}"
                        )
                    }
                    // Recargar para obtener datos actualizados
                    loadProducts(showLoading = false)
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error creando producto: ${result.message}")
                    // Recargar por si acaso se creó pero hubo error al obtenerlo
                    loadProducts(showLoading = false)
                }
            }
        }
    }

    // ACTUALIZADO: Manejo de múltiples imágenes
    fun updateProduct(
        productId: String,
        name: String,
        description: String? = null,
        price: Double,
        stock: Int,
        minStock: Int = 0,
        newImageUris: List<Uri> = emptyList(), // 👈 Nuevas imágenes
        existingImageUrls: List<String> = emptyList(), // 👈 Imágenes existentes a mantener
        categoryIds: List<String> = emptyList(),
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(productsState = UiState.Loading) }

            Log.d(TAG, "🔄 Actualizando producto: $productId")

            when (val result = productRepository.updateProduct(
                productId, name, description, price, stock, minStock,
                newImageUris, existingImageUrls, categoryIds, isActive
            )) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto actualizado exitosamente")
                    _uiState.update {
                        it.copy(
                            productsState = UiState.Loading,
                            successMessage = "Producto actualizado"
                        )
                    }
                    loadProducts(showLoading = false)
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error actualizando producto: ${result.message}")
                    loadProducts(showLoading = false)
                }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(productsState = UiState.Loading) }

            Log.d(TAG, "🗑️ Eliminando producto: $productId")

            when (val result = productRepository.deleteProduct(productId)) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto eliminado exitosamente")
                    _uiState.update {
                        it.copy(
                            productsState = UiState.Loading,
                            successMessage = "Producto eliminado"
                        )
                    }
                    loadProducts(showLoading = false)
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error eliminando producto: ${result.message}")
                    loadProducts(showLoading = false)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        Log.d(TAG, "🔍 Búsqueda actualizada: $query")
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowActiveOnly(show: Boolean) {
        Log.d(TAG, "👁️ Mostrar solo activos: $show")
        _uiState.update { it.copy(showActiveOnly = show) }
    }

    fun setCategoryFilter(categoryId: String?) {
        Log.d(TAG, "🎭 Filtro de categoría: ${categoryId ?: "Todas"}")
        _uiState.update { it.copy(selectedCategoryFilter = categoryId) }
    }

    val filteredProducts: List<ProductWithCategories>
        get() {
            val productsState = _uiState.value.productsState
            val products = when (productsState) {
                is UiState.Success -> productsState.data
                else -> emptyList()
            }

            val query = _uiState.value.searchQuery.lowercase()
            return products.filter { product ->
                val matchesSearch = product.name.lowercase().contains(query) ||
                        product.description?.lowercase()?.contains(query) == true ||
                        product.categories.any { it.name.lowercase().contains(query) }

                val matchesActive = if (_uiState.value.showActiveOnly) {
                    product.isActive
                } else {
                    true
                }

                val matchesCategory = _uiState.value.selectedCategoryFilter?.let { categoryId ->
                    product.categories.any { it.id == categoryId }
                } ?: true

                matchesSearch && matchesActive && matchesCategory
            }
        }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Método helper para obtener colmado_id
     */
    private suspend fun getColmadoId(): String? {
        var colmadoId = sharedPrefsHelper.getColmadoId()

        if (colmadoId == null) {
            Log.d(TAG, "📦 Colmado_id no encontrado, obteniendo desde BD...")
            val userId = sharedPrefsHelper.getUserId()
            if (userId != null) {
                when (val result = sellerRepository.getSellerColmadoId(userId)) {
                    is SellerRepository.Result.Success -> {
                        colmadoId = result.data
                        sharedPrefsHelper.saveColmadoId(colmadoId)
                        Log.d(TAG, "✅ Colmado_id obtenido: $colmadoId")
                    }
                    is SellerRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error obteniendo colmado_id: ${result.message}")
                        return null
                    }
                }
            }
        }

        return colmadoId
    }
}