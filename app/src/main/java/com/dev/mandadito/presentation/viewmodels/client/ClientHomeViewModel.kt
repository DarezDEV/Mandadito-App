package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.ColmadoWithOwner
import com.dev.mandadito.data.repository.ColmadosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientHomeUiState(
    val colmados: List<ColmadoWithOwner> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

class ClientHomeViewModel(context: Context) : ViewModel() {

    private val repository = ColmadosRepository(context)
    private val TAG = "ClientHomeViewModel"

    private val _uiState = MutableStateFlow(ClientHomeUiState(isLoading = true))
    val uiState: StateFlow<ClientHomeUiState> = _uiState.asStateFlow()

    init {
        loadColmados()
    }

    fun loadColmados() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔥 Cargando colmados activos...")

            try {
                val colmados = repository.getActiveColmados()

                Log.d(TAG, "✅ ${colmados.size} colmados cargados")
                _uiState.update {
                    it.copy(
                        colmados = colmados,
                        isLoading = false,
                        error = if (colmados.isEmpty()) "No hay colmados disponibles" else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando colmados: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al cargar colmados: ${e.message}"
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        Log.d(TAG, "🔍 Búsqueda actualizada: $query")
        _uiState.update { it.copy(searchQuery = query) }
    }

    val filteredColmados: List<ColmadoWithOwner>
        get() {
            val query = _uiState.value.searchQuery.lowercase()
            if (query.isBlank()) {
                return _uiState.value.colmados
            }

            return _uiState.value.colmados.filter { colmado ->
                colmado.name.lowercase().contains(query) ||
                        colmado.address.lowercase().contains(query) ||
                        colmado.description?.lowercase()?.contains(query) == true
            }
        }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}