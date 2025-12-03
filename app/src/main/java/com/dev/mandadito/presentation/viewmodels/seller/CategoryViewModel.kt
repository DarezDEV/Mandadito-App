package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.network.CategoryRepository
import com.dev.mandadito.data.network.SellerRepository
import com.dev.mandadito.utils.SharedPreferenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val showActiveOnly: Boolean = false
)

class CategoryViewModel(context: Context) : ViewModel() {

    private val repository = CategoryRepository(context)
    private val sellerRepository = SellerRepository(context)
    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val TAG = "CategoryViewModel"

    private val _uiState = MutableStateFlow(CategoryUiState(isLoading = true))
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                _uiState.update { it.copy(error = null) }
            }

            Log.d(TAG, "📥 Cargando categorías...")

            when (val result = repository.getAllCategories()) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} categorías cargadas")
                    _uiState.update {
                        it.copy(
                            categories = result.data,
                            isLoading = false,
                            successMessage = if (showLoading) "Categorías cargadas" else it.successMessage
                        )
                    }
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando categorías: ${result.message}")
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

    fun createCategory(
        name: String,
        description: String? = null,
        icon: String? = null,
        color: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔷 Creando categoría: $name")

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
                                    isLoading = false,
                                    error = "Error al obtener información del colmado: ${result.message}"
                                )
                            }
                            return@launch
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se pudo obtener el ID del usuario"
                        )
                    }
                    return@launch
                }
            }

            if (colmadoId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No tienes un colmado asignado. Contacta al administrador."
                    )
                }
                return@launch
            }

            when (val result = repository.createCategory(colmadoId, name, description, icon, color)) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ Categoría creada exitosamente")
                    // Agregar la categoría inmediatamente a la lista para que aparezca sin recargar
                    _uiState.update { currentState ->
                        currentState.copy(
                            categories = currentState.categories + result.data,
                            isLoading = false,
                            successMessage = "Categoría creada: ${result.data.name}"
                        )
                    }
                    // También recargar en background para asegurar sincronización
                    loadCategories(showLoading = false)
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error creando categoría: ${result.message}")
                    // Recargar categorías por si acaso se creó pero hubo error al obtenerla
                    loadCategories(showLoading = false)
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

    fun updateCategory(
        categoryId: String,
        name: String,
        description: String? = null,
        icon: String? = null,
        color: String? = null,
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔄 Actualizando categoría: $categoryId")

            when (val result = repository.updateCategory(categoryId, name, description, icon, color, isActive)) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ Categoría actualizada exitosamente")
                    _uiState.update {
                        it.copy(
                            categories = it.categories.map { category ->
                                if (category.id == categoryId) result.data else category
                            },
                            isLoading = false,
                            successMessage = "Categoría actualizada"
                        )
                    }
                    loadCategories(showLoading = false)
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error actualizando categoría: ${result.message}")
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

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🗑️ Eliminando categoría: $categoryId")

            when (val result = repository.deleteCategory(categoryId)) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ Categoría eliminada exitosamente")
                    _uiState.update {
                        it.copy(
                            categories = it.categories.filterNot { category -> category.id == categoryId },
                            isLoading = false,
                            successMessage = "Categoría eliminada"
                        )
                    }
                    loadCategories(showLoading = false)
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error eliminando categoría: ${result.message}")
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
        Log.d(TAG, "👁️ Mostrar solo activas: $show")
        _uiState.update { it.copy(showActiveOnly = show) }
    }

    val filteredCategories: List<Category>
        get() {
            val query = _uiState.value.searchQuery.lowercase()
            return _uiState.value.categories.filter { category ->
                val matchesSearch = category.name.lowercase().contains(query) ||
                        category.description?.lowercase()?.contains(query) == true

                val matchesActive = if (_uiState.value.showActiveOnly) {
                    category.isActive
                } else {
                    true
                }

                matchesSearch && matchesActive
            }
        }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

