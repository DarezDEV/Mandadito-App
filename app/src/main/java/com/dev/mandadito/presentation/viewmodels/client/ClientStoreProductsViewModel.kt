package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.data.repository.CategoryRepository
import com.dev.mandadito.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientStoreProductsUiState(
    val products: List<ProductWithCategories> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val colmadoName: String = ""
)

class ClientStoreProductsViewModel(context: Context) : ViewModel() {

    private val productRepository = ProductRepository(context)
    private val categoryRepository = CategoryRepository(context)
    private val TAG = "ClientStoreProductsVM"

    private val _uiState = MutableStateFlow(ClientStoreProductsUiState(isLoading = true))
    val uiState: StateFlow<ClientStoreProductsUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadCategories()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔥 Cargando productos activos...")

            when (val result = productRepository.getActiveProducts()) {
                is ProductRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} productos cargados")
                    _uiState.update {
                        it.copy(
                            products = result.data,
                            isLoading = false,
                            error = if (result.data.isEmpty()) "No hay productos disponibles" else null
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
            val query = _uiState.value.searchQuery.lowercase()
            val categoryId = _uiState.value.selectedCategoryId

            return _uiState.value.products.filter { product ->
                // Filtro de búsqueda
                val matchesSearch = query.isBlank() ||
                        product.name.lowercase().contains(query) ||
                        product.description?.lowercase()?.contains(query) == true

                // Filtro de categoría
                val matchesCategory = categoryId == null ||
                        product.categories.any { it.id == categoryId }

                matchesSearch && matchesCategory
            }
        }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}