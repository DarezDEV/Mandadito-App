package com.dev.mandadito.presentation.viewmodels.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.ColmadoWithOwner
import com.dev.mandadito.data.network.ColmadosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminColmadosViewModel(context: Context) : ViewModel() {

    private val repository = ColmadosRepository(context)
    private val TAG = "AdminColmadosViewModel"

    private val _uiState = MutableStateFlow(AdminColmadosUiState())
    val uiState: StateFlow<AdminColmadosUiState> = _uiState.asStateFlow()

    // Lista filtrada de colmados
    val filteredColmados: List<ColmadoWithOwner>
        get() = filterColmados(
            allColmados = _uiState.value.colmados,
            searchQuery = _uiState.value.searchQuery,
            showInactiveOnly = _uiState.value.showInactiveOnly
        )

    init {
        loadColmados()
    }

    /**
     * Cargar todos los colmados
     */
    fun loadColmados() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                val colmados = repository.getAllColmados()

                _uiState.value = _uiState.value.copy(
                    colmados = colmados,
                    isLoading = false,
                    error = if (colmados.isEmpty()) "No hay colmados registrados" else null
                )

                Log.d(TAG, "✅ Colmados cargados: ${colmados.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando colmados: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No se pudieron cargar los colmados. Verifique su conexión a internet."
                )
            }
        }
    }

    /**
     * Desactivar un colmado
     */
    fun deactivateColmado(colmadoId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                when (val result = repository.deactivateColmado(colmadoId)) {
                    is ColmadosRepository.Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Colmado desactivado exitosamente",
                            isLoading = false
                        )
                        loadColmados()
                        Log.d(TAG, "✅ Colmado desactivado: $colmadoId")
                    }
                    is ColmadosRepository.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                        Log.e(TAG, "❌ Error desactivando colmado: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error desactivando colmado: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al desactivar el colmado",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Activar un colmado
     */
    fun activateColmado(colmadoId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                when (val result = repository.activateColmado(colmadoId)) {
                    is ColmadosRepository.Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Colmado activado exitosamente",
                            isLoading = false
                        )
                        loadColmados()
                        Log.d(TAG, "✅ Colmado activado: $colmadoId")
                    }
                    is ColmadosRepository.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                        Log.e(TAG, "❌ Error activando colmado: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error activando colmado: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al activar el colmado",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Eliminar un colmado
     */
    fun deleteColmado(colmadoId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                when (val result = repository.deleteColmado(colmadoId)) {
                    is ColmadosRepository.Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Colmado eliminado exitosamente",
                            isLoading = false
                        )
                        loadColmados()
                        Log.d(TAG, "✅ Colmado eliminado: $colmadoId")
                    }
                    is ColmadosRepository.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                        Log.e(TAG, "❌ Error eliminando colmado: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error eliminando colmado: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar el colmado",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Establecer la consulta de búsqueda
     */
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /**
     * Cambiar filtro de inactivos
     */
    fun setShowInactiveOnly(showInactive: Boolean) {
        _uiState.value = _uiState.value.copy(showInactiveOnly = showInactive)
    }

    /**
     * Limpiar mensaje de éxito
     */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Filtrar colmados
     */
    private fun filterColmados(
        allColmados: List<ColmadoWithOwner>,
        searchQuery: String,
        showInactiveOnly: Boolean
    ): List<ColmadoWithOwner> {
        var filtered = allColmados

        // Filtrar por estado activo/inactivo
        filtered = if (showInactiveOnly) {
            filtered.filter { !it.isActive }
        } else {
            filtered.filter { it.isActive }
        }

        // Filtrar por búsqueda
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            filtered = filtered.filter { colmado ->
                colmado.name.lowercase().contains(query) ||
                        colmado.address.lowercase().contains(query) ||
                        colmado.phone.contains(query) ||
                        colmado.ownerName?.lowercase()?.contains(query) == true ||
                        colmado.ownerEmail?.lowercase()?.contains(query) == true
            }
        }

        return filtered.sortedByDescending { it.createdAt }
    }
}

/**
 * Estado de UI para la pantalla de gestión de colmados
 */
data class AdminColmadosUiState(
    val colmados: List<ColmadoWithOwner> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val showInactiveOnly: Boolean = false
)