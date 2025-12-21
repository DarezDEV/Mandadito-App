package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.ColmadoWithOwner
import com.dev.mandadito.data.network.ConnectivityMonitor
import com.dev.mandadito.data.network.RetryPolicy
import com.dev.mandadito.data.network.RetryState
import com.dev.mandadito.data.repository.ColmadosRepository
import com.dev.mandadito.presentation.viewmodels.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientHomeUiState(
    val colmadosState: UiState<List<ColmadoWithOwner>> = UiState.Idle,
    val searchQuery: String = "",
    val isConnected: Boolean = true
)

class ClientHomeViewModel(context: Context) : ViewModel() {

    private val repository = ColmadosRepository(context)
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val TAG = "ClientHomeViewModel"

    private val _uiState = MutableStateFlow(ClientHomeUiState())
    val uiState: StateFlow<ClientHomeUiState> = _uiState.asStateFlow()

    init {
        // Observar cambios de conectividad
        viewModelScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                Log.d(TAG, "🌐 Conexión: ${if (isConnected) "ONLINE" else "OFFLINE"}")
                _uiState.update { it.copy(isConnected = isConnected) }

                // Si vuelve la conexión y hay error, reintentar
                if (isConnected && _uiState.value.colmadosState is UiState.Error) {
                    Log.d(TAG, "🔄 Conexión restaurada, recargando...")
                    loadColmados()
                }
            }
        }
        loadColmados()
    }

    fun loadColmados() {
        viewModelScope.launch {
            Log.d(TAG, "🔥 Cargando colmados activos...")

            // Usar RetryPolicy para reintentos automáticos
            RetryPolicy.retryWithBackoff(
                isConnected = connectivityMonitor.isCurrentlyConnected(),
                operation = { repository.getActiveColmados() }
            ).collect { retryState ->
                when (retryState) {
                    is RetryState.Loading -> {
                        _uiState.update { it.copy(colmadosState = UiState.Loading) }
                    }

                    is RetryState.Success -> {
                        when (val result = retryState.data) {
                            is ColmadosRepository.Result.Success -> {
                                Log.d(TAG, "✅ ${result.data.size} colmados cargados")
                                _uiState.update {
                                    it.copy(
                                        colmadosState = UiState.Success(
                                            data = result.data,
                                            isFromCache = result.isFromCache,
                                            cacheTimestamp = result.cacheTimestamp
                                        )
                                    )
                                }
                            }
                            is ColmadosRepository.Result.Error -> {
                                Log.e(TAG, "❌ Error: ${result.message}")
                                _uiState.update {
                                    it.copy(colmadosState = UiState.Error(result.message))
                                }
                            }
                        }
                    }

                    is RetryState.Retrying -> {
                        Log.d(TAG, "🔄 Reintentando... Intento #${retryState.attempt}")
                        _uiState.update {
                            it.copy(
                                colmadosState = UiState.Retrying(
                                    attempt = retryState.attempt,
                                    nextRetryInSeconds = retryState.nextRetryInSeconds
                                )
                            )
                        }
                    }

                    is RetryState.Error -> {
                        Log.e(TAG, "❌ Error cargando colmados: ${retryState.message}")
                        _uiState.update {
                            it.copy(colmadosState = UiState.Error(retryState.message))
                        }
                    }
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
            val colmadosState = _uiState.value.colmadosState
            val colmados = when (colmadosState) {
                is UiState.Success -> colmadosState.data
                else -> emptyList()
            }

            val query = _uiState.value.searchQuery.lowercase()
            if (query.isBlank()) {
                return colmados
            }

            return colmados.filter { colmado ->
                colmado.name.lowercase().contains(query) ||
                        colmado.address.lowercase().contains(query) ||
                        colmado.description?.lowercase()?.contains(query) == true
            }
        }

}