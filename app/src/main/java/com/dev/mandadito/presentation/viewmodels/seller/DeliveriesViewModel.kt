package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.DeliveryUser
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.RetryPolicy
import com.dev.mandadito.data.network.RetryState
import com.dev.mandadito.data.repository.DeliveriesRepository
import com.dev.mandadito.data.repository.SellerRepository
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.utils.SharedPreferenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeliveriesUiState(
    val deliveriesState: UiState<List<DeliveryUser>> = UiState.Idle,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val showInactiveOnly: Boolean = false,
    val colmadoId: String? = null,
    val isConnected: Boolean = true
)

class DeliveriesViewModel(context: Context) : ViewModel() {

    private val repository = DeliveriesRepository(context)
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val appContext = context.applicationContext
    private val TAG = "DeliveriesViewModel"

    private val _uiState = MutableStateFlow(DeliveriesUiState())
    val uiState: StateFlow<DeliveriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Primero intentar obtener desde SharedPreferences
                var colmadoId = sharedPrefsHelper.getColmadoId()
                
                // Si no está en SharedPreferences, obtener desde la base de datos
                if (colmadoId == null) {
                    Log.d(TAG, "📦 Colmado_id no encontrado en SharedPreferences, obteniendo desde BD...")
                    val userId = sharedPrefsHelper.getUserId()
                    if (userId != null) {
                        val sellerRepo = SellerRepository(appContext)
                        when (val result = sellerRepo.getSellerColmadoId(userId)) {
                            is SellerRepository.Result.Success -> {
                                colmadoId = result.data
                                // Guardar en SharedPreferences para futuras consultas
                                sharedPrefsHelper.saveColmadoId(colmadoId)
                                Log.d(TAG, "✅ Colmado_id obtenido y guardado: $colmadoId")
                            }
                            is SellerRepository.Result.Error -> {
                                Log.e(TAG, "❌ Error obteniendo colmado_id: ${result.message}")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "✅ Colmado_id encontrado en SharedPreferences: $colmadoId")
                }

                _uiState.update { it.copy(colmadoId = colmadoId) }

                if (colmadoId != null) {
                    loadDeliveries()
                } else {
                    Log.w(TAG, "⚠️ No se encontró colmado_id para el seller")
                    _uiState.update {
                        it.copy(
                            deliveriesState = UiState.Error("No se pudo obtener el ID del colmado. Por favor, contacta al administrador.")
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inicializando DeliveriesViewModel: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        deliveriesState = UiState.Error("Error al inicializar: ${e.message}")
                    )
                }
            }
        }
    }

    fun loadDeliveries(showLoading: Boolean = true) {
        viewModelScope.launch {
            val colmadoId = _uiState.value.colmadoId ?: return@launch

            if (showLoading) {
                _uiState.update { it.copy(deliveriesState = UiState.Loading) }
            }

            Log.d(TAG, "📥 Cargando deliveries del colmado: $colmadoId")

            when (val result = repository.getDeliveriesByColmado(colmadoId)) {
                is DeliveriesRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} deliveries cargados")
                    _uiState.update {
                        it.copy(
                            deliveriesState = UiState.Success(
                                data = result.data,
                                isFromCache = result.isFromCache,
                                cacheTimestamp = result.cacheTimestamp
                            ),
                            successMessage = if (showLoading) "Deliveries cargados" else it.successMessage
                        )
                    }
                }
                is DeliveriesRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update {
                        it.copy(
                            deliveriesState = UiState.Error(result.message)
                        )
                    }
                }
            }
        }
    }

    fun loadAvailableDeliveries() {
        // Ya no se necesita cargar deliveries disponibles
        // Cada seller crea sus propios deliveries
    }

    fun createDeliveryForColmado(
        email: String,
        password: String,
        nombre: String,
        avatarUri: Uri? = null
    ) {
        viewModelScope.launch {
            val colmadoId = _uiState.value.colmadoId ?: return@launch

            _uiState.update { it.copy(deliveriesState = UiState.Loading) }

            Log.d(TAG, "➕ Creando nuevo delivery para el colmado")

            when (val result = repository.createDeliveryForColmado(
                email = email,
                password = password,
                nombre = nombre,
                colmadoId = colmadoId,
                avatarUri = avatarUri
            )) {
                is DeliveriesRepository.Result.Success -> {
                    Log.d(TAG, "✅ Delivery creado exitosamente")
                    _uiState.update {
                        it.copy(
                            successMessage = "Delivery creado exitosamente: ${result.data.nombre}"
                        )
                    }
                    // Recargar en background para asegurar sincronización
                    loadDeliveries(showLoading = false)
                }
                is DeliveriesRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update {
                        it.copy(
                            deliveriesState = UiState.Error(result.message)
                        )
                    }
                }
            }
        }
    }

    fun removeDeliveryFromColmado(userId: String) {
        viewModelScope.launch {
            val colmadoId = _uiState.value.colmadoId ?: return@launch

            Log.d(TAG, "🗑️ Desvinculando delivery del colmado")

            when (val result = repository.removeDeliveryFromColmado(userId, colmadoId)) {
                is DeliveriesRepository.Result.Success -> {
                    Log.d(TAG, "✅ Delivery desvinculado exitosamente")
                    removeDeliveryLocally(userId)
                    _uiState.update {
                        it.copy(successMessage = "Delivery eliminado")
                    }
                    loadDeliveries(showLoading = false)
                }
                is DeliveriesRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update {
                        it.copy(errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun enableDelivery(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "✅ Habilitando delivery")

            when (val result = repository.enableDelivery(userId)) {
                is DeliveriesRepository.Result.Success -> {
                    Log.d(TAG, "✅ Delivery habilitado exitosamente")
                    updateDeliveryLocally(userId) { it.copy(activo = true) }
                    _uiState.update {
                        it.copy(successMessage = "Delivery habilitado")
                    }
                    loadDeliveries(showLoading = false)
                }
                is DeliveriesRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update {
                        it.copy(errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun disableDelivery(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "🚫 Deshabilitando delivery")

            when (val result = repository.disableDelivery(userId)) {
                is DeliveriesRepository.Result.Success -> {
                    Log.d(TAG, "✅ Delivery deshabilitado exitosamente")
                    updateDeliveryLocally(userId) { it.copy(activo = false) }
                    _uiState.update {
                        it.copy(successMessage = "Delivery deshabilitado")
                    }
                    loadDeliveries(showLoading = false)
                }
                is DeliveriesRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update {
                        it.copy(errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun updateDelivery(
        userId: String,
        nombre: String,
        email: String,
        avatarUri: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(deliveriesState = UiState.Loading) }

            Log.d(TAG, "🔄 Actualizando delivery: $userId")

            when (val result = repository.updateDelivery(userId, nombre, email, avatarUri)) {
                is DeliveriesRepository.Result.Success -> {
                    Log.d(TAG, "✅ Delivery actualizado exitosamente")
                    updateDeliveryLocally(userId) { result.data }
                    _uiState.update {
                        it.copy(
                            successMessage = "Delivery actualizado: ${result.data.nombre}"
                        )
                    }
                    // Recargar en background para asegurar sincronización
                    loadDeliveries(showLoading = false)
                }
                is DeliveriesRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update {
                        it.copy(
                            deliveriesState = UiState.Error(result.message)
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

    fun setShowInactiveOnly(show: Boolean) {
        Log.d(TAG, "👁️ Mostrar inactivos: $show")
        _uiState.update { it.copy(showInactiveOnly = show) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    val filteredDeliveries: List<DeliveryUser>
        get() {
            val query = _uiState.value.searchQuery.lowercase()
            val currentDeliveries = (_uiState.value.deliveriesState as? UiState.Success)?.data ?: emptyList()

            return currentDeliveries.filter { delivery ->
                val matchesSearch = delivery.email.lowercase().contains(query) ||
                        delivery.nombre.lowercase().contains(query)

                val matchesStatus = if (_uiState.value.showInactiveOnly) {
                    !delivery.activo
                } else {
                    delivery.activo
                }

                matchesSearch && matchesStatus
            }
        }

    private fun updateDeliveryLocally(userId: String, transform: (DeliveryUser) -> DeliveryUser) {
        _uiState.update { state ->
            val currentDeliveries = (state.deliveriesState as? UiState.Success)?.data ?: emptyList()
            val updatedDeliveries = currentDeliveries.map { delivery ->
                if (delivery.id == userId) transform(delivery) else delivery
            }
            state.copy(
                deliveriesState = UiState.Success(updatedDeliveries)
            )
        }
    }

    private fun removeDeliveryLocally(userId: String) {
        _uiState.update { state ->
            val currentDeliveries = (state.deliveriesState as? UiState.Success)?.data ?: emptyList()
            val filteredDeliveries = currentDeliveries.filterNot { it.id == userId }
            state.copy(
                deliveriesState = UiState.Success(filteredDeliveries)
            )
        }
    }
}