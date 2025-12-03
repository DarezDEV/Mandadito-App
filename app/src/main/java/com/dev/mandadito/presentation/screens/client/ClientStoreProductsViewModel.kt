package com.dev.mandadito.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Colmado
import com.dev.mandadito.data.models.Product
import com.dev.mandadito.data.repository.ClientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClientStoreProductsViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _selectedColmado = MutableStateFlow<Colmado?>(null)
    val selectedColmado: StateFlow<Colmado?> = _selectedColmado.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProductsForColmado(colmadoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _selectedColmado.value = ClientRepository.getColmadoById(colmadoId)
                _products.value = ClientRepository.getProductsByColmado(colmadoId)
                    .filter { it.isActive }
            } catch (e: Exception) {
                _error.value = "Error cargando productos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}