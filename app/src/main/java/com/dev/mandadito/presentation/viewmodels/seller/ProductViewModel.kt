package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.data.network.CategoryRepository
import com.dev.mandadito.data.network.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductUiState(
    val products: List<ProductWithCategories> = emptyList(),
    val categories: List<com.dev.mandadito.data.models.Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val showActiveOnly: Boolean = false,
    val selectedCategoryFilter: String? = null
)

class ProductViewModel(context: Context) : ViewModel() {

    private val productRepository = ProductRepository(context)
    private val categoryRepository = CategoryRepository(context)
    private val TAG = "ProductViewModel"

    private val _uiState = MutableStateFlow(ProductUiState(isLoading = true))
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadCategories()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔥 Cargando productos...")

            when (val result = productRepository.getAllProducts()) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} productos cargados")
                    _uiState.update {
                        it.copy(
                            products = result.data,
                            isLoading = false,
                            successMessage = "Productos cargados"
                        )
                    }
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando productos: ${result.message}")
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

    fun loadCategories() {
        viewModelScope.launch {
            when (val result = categoryRepository.getActiveCategories()) {
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
        imageUris: List<Uri> = emptyList(), // 👈 Lista de imágenes
        categoryIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔷 Creando producto: $name con ${imageUris.size} imágenes")

            when (val result = productRepository.createProduct(
                name, description, price, stock, imageUris, categoryIds
            )) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto creado exitosamente")
                    loadProducts() // Recargar para obtener datos actualizados
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Producto creado: ${result.data.name}"
                        )
                    }
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error creando producto: ${result.message}")
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

    // ACTUALIZADO: Manejo de múltiples imágenes
    fun updateProduct(
        productId: String,
        name: String,
        description: String? = null,
        price: Double,
        stock: Int,
        newImageUris: List<Uri> = emptyList(), // 👈 Nuevas imágenes
        existingImageUrls: List<String> = emptyList(), // 👈 Imágenes existentes a mantener
        categoryIds: List<String> = emptyList(),
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔄 Actualizando producto: $productId")

            when (val result = productRepository.updateProduct(
                productId, name, description, price, stock,
                newImageUris, existingImageUrls, categoryIds, isActive
            )) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto actualizado exitosamente")
                    loadProducts()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Producto actualizado"
                        )
                    }
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error actualizando producto: ${result.message}")
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

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🗑️ Eliminando producto: $productId")

            when (val result = productRepository.deleteProduct(productId)) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ Producto eliminado exitosamente")
                    loadProducts()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Producto eliminado"
                        )
                    }
                }
                is ProductRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error eliminando producto: ${result.message}")
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
            val query = _uiState.value.searchQuery.lowercase()
            return _uiState.value.products.filter { product ->
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}