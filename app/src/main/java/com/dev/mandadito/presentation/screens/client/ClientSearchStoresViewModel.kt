package com.dev.mandadito.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Colmado
import com.dev.mandadito.data.repository.ClientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClientSearchStoresViewModel : ViewModel() {
    private val _allColmados = MutableStateFlow<List<Colmado>>(emptyList())
    private val _filteredColmados = MutableStateFlow<List<Colmado>>(emptyList())
    val filteredColmados: StateFlow<List<Colmado>> = _filteredColmados.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadColmados()
    }

    private fun loadColmados() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val colmados = ClientRepository.getColmados()
                    .filter { it.isActive }
                _allColmados.value = colmados
                _filteredColmados.value = colmados
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _filteredColmados.value = if (query.isBlank()) {
            _allColmados.value
        } else {
            _allColmados.value.filter { colmado ->
                colmado.name.contains(query, ignoreCase = true) ||
                        colmado.address.contains(query, ignoreCase = true) ||
                        colmado.description?.contains(query, ignoreCase = true) == true
            }
        }
    }
}