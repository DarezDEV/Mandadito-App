package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.RetryPolicy
import com.dev.mandadito.data.network.RetryState
import com.dev.mandadito.data.repository.CategoryRepository
import com.dev.mandadito.data.repository.SellerRepository
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.presentation.viewmodels.common.dataOrNull
import com.dev.mandadito.utils.SharedPreferenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUiState(
    val categoriesState: UiState<List<Category>> = UiState.Idle,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val showActiveOnly: Boolean = false,
    val isConnected: Boolean = true
)

class CategoryViewModel(context: Context) : ViewModel() {

    private val repository = CategoryRepository(context)
    private val sellerRepository = SellerRepository(context)
    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val TAG = "CategoryViewModel"

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        // Observar cambios de conectividad
        viewModelScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                Log.d(TAG, "🌐 Conexión: ${if (isConnected) "ONLINE" else "OFFLINE"}")
                _uiState.update { it.copy(isConnected = isConnected) }

                // Si vuelve la conexión y hay error, reintentar
                if (isConnected && _uiState.value.categoriesState is UiState.Error) {
                    Log.d(TAG, "🔄 Conexión restaurada, recargando...")
                    loadCategories()
                }
            }
        }
        loadCategories()
    }

    fun loadCategories(showLoading: Boolean = true) {
        viewModelScope.launch {
            Log.d(TAG, "📥 Cargando categorías...")

            // Obtener colmado_id
            val colmadoId = getColmadoId() ?: run {
                _uiState.update {
                    it.copy(categoriesState = UiState.Error("No se pudo obtener el colmado"))
                }
                return@launch
            }

            // Usar RetryPolicy para reintentos automáticos
            RetryPolicy.retryWithBackoff(
                isConnected = connectivityMonitor.isCurrentlyConnected(),
                operation = { repository.getAllCategories(colmadoId) }
            ).collect { retryState ->
                when (retryState) {
                    is RetryState.Loading -> {
                        _uiState.update { it.copy(categoriesState = UiState.Loading) }
                    }

                    is RetryState.Success -> {
                        when (val result = retryState.data) {
                            is CategoryRepository.Result.Success -> {
                                Log.d(TAG, "✅ ${result.data.size} categorías cargadas")
                                _uiState.update {
                                    it.copy(
                                        categoriesState = UiState.Success(
                                            data = result.data,
                                            isFromCache = result.isFromCache,
                                            cacheTimestamp = result.cacheTimestamp
                                        ),
                                        successMessage = if (!result.isFromCache) "Categorías cargadas" else null
                                    )
                                }
                            }
                            is CategoryRepository.Result.Error -> {
                                Log.e(TAG, "❌ Error: ${result.message}")
                                _uiState.update {
                                    it.copy(categoriesState = UiState.Error(result.message))
                                }
                            }
                        }
                    }

                    is RetryState.Retrying -> {
                        Log.d(TAG, "🔄 Reintentando... Intento #${retryState.attempt}")
                        _uiState.update {
                            it.copy(
                                categoriesState = UiState.Retrying(
                                    attempt = retryState.attempt,
                                    nextRetryInSeconds = retryState.nextRetryInSeconds
                                )
                            )
                        }
                    }

                    is RetryState.Error -> {
                        Log.e(TAG, "❌ Error cargando categorías: ${retryState.message}")
                        _uiState.update {
                            it.copy(categoriesState = UiState.Error(retryState.message))
                        }
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
            _uiState.update { it.copy(categoriesState = UiState.Loading) }

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
                                    categoriesState = UiState.Error("Error al obtener información del colmado: ${result.message}")
                                )
                            }
                            return@launch
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            categoriesState = UiState.Error("No se pudo obtener el ID del usuario")
                        )
                    }
                    return@launch
                }
            }

            if (colmadoId == null) {
                _uiState.update {
                    it.copy(
                        categoriesState = UiState.Error("No tienes un colmado asignado. Contacta al administrador.")
                    )
                }
                return@launch
            }

            when (val result = repository.createCategory(colmadoId, name, description, icon, color)) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ Categoría creada exitosamente")
                    _uiState.update {
                        it.copy(
                            categoriesState = UiState.Loading,
                            successMessage = "Categoría creada: ${result.data.name}"
                        )
                    }
                    loadCategories(showLoading = false)
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error creando categoría: ${result.message}")
                    // Recargar categorías por si acaso se creó pero hubo error al obtenerla
                    loadCategories(showLoading = false)
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
            _uiState.update { it.copy(categoriesState = UiState.Loading) }

            Log.d(TAG, "🔄 Actualizando categoría: $categoryId")

            when (val result = repository.updateCategory(categoryId, name, description, icon, color, isActive)) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ Categoría actualizada exitosamente")
                    _uiState.update {
                        it.copy(
                            categoriesState = UiState.Loading,
                            successMessage = "Categoría actualizada"
                        )
                    }
                    loadCategories(showLoading = false)
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error actualizando categoría: ${result.message}")
                    loadCategories(showLoading = false)
                }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(categoriesState = UiState.Loading) }

            Log.d(TAG, "🗑️ Eliminando categoría: $categoryId")

            when (val result = repository.deleteCategory(categoryId)) {
                is CategoryRepository.Result.Success -> {
                    Log.d(TAG, "✅ Categoría eliminada exitosamente")
                    _uiState.update {
                        it.copy(
                            categoriesState = UiState.Loading,
                            successMessage = "Categoría eliminada"
                        )
                    }
                    loadCategories(showLoading = false)
                }
                is CategoryRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error eliminando categoría: ${result.message}")
                    loadCategories(showLoading = false)
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
            val categoriesState = _uiState.value.categoriesState
            val categories = categoriesState.dataOrNull() ?: emptyList()

            val query = _uiState.value.searchQuery.lowercase()
            return categories.filter { category ->
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

