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
import kotlinx.coroutines.launch

class ClientSearchStoresViewModel(context: Context) : ViewModel() {

    private val colmadosRepository = ColmadosRepository(context)
    private val TAG = "ClientSearchStoresVM"

    private val _allColmados = MutableStateFlow<List<ColmadoWithOwner>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filteredColmados = MutableStateFlow<List<ColmadoWithOwner>>(emptyList())
    val filteredColmados: StateFlow<List<ColmadoWithOwner>> = _filteredColmados.asStateFlow()

    init {
        loadColmados()
    }

    private fun loadColmados() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "🔥 Cargando colmados activos...")
                val colmados = colmadosRepository.getActiveColmados()

                Log.d(TAG, "✅ ${colmados.size} colmados cargados")
                _allColmados.value = colmados
                _filteredColmados.value = colmados
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando colmados: ${e.message}", e)
                _allColmados.value = emptyList()
                _filteredColmados.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        filterColmados(query)
    }

    private fun filterColmados(query: String) {
        val trimmedQuery = query.trim().lowercase()

        _filteredColmados.value = if (trimmedQuery.isEmpty()) {
            _allColmados.value
        } else {
            _allColmados.value.filter { colmado ->
                colmado.name.lowercase().contains(trimmedQuery) ||
                colmado.address.lowercase().contains(trimmedQuery) ||
                colmado.phone.contains(trimmedQuery)
            }
        }

        Log.d(TAG, "🔍 Búsqueda '$query': ${_filteredColmados.value.size} resultados")
    }

    fun refresh() {
        loadColmados()
    }
}
