package com.dev.mandadito.presentation.viewmodels.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.data.repository.FinanceRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminFinanceUiState(
    val summary: FinancialSummary? = null,
    val dailyRevenue: List<DailyRevenue> = emptyList(),
    val revenueByColmado: List<RevenueByColmado> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPeriod: DatePeriod = DatePeriod.MONTH,
    val isRefreshing: Boolean = false,
    val hasRealtimeUpdate: Boolean = false
)

class AdminFinanceViewModel(context: Context) : ViewModel() {

    private val repository = FinanceRepository(context)
    private val TAG = "AdminFinanceViewModel"

    private var realtimeChannel: RealtimeChannel? = null

    private val _uiState = MutableStateFlow(AdminFinanceUiState(isLoading = true))
    val uiState: StateFlow<AdminFinanceUiState> = _uiState.asStateFlow()

    init {
        loadFinanceData()
        setupRealtime()
    }

    private fun setupRealtime() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando suscripcion realtime...")

                val channelId = "finance_payments_${System.currentTimeMillis()}"
                realtimeChannel = SupabaseClient.client.realtime.channel(channelId)

                realtimeChannel!!.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "payments"
                }.catch { e ->
                    Log.e(TAG, "Error en Realtime: ${e.message}", e)
                }.onEach { action ->
                    Log.d(TAG, "Evento realtime: ${action::class.simpleName}")
                    _uiState.update { it.copy(hasRealtimeUpdate = true) }
                }.launchIn(viewModelScope)

                realtimeChannel!!.subscribe()
                Log.d(TAG, "Realtime activado: $channelId")

            } catch (e: Exception) {
                Log.e(TAG, "Error configurando realtime: ${e.message}", e)
            }
        }
    }

    fun loadFinanceData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasRealtimeUpdate = false) }
            Log.d(TAG, "Cargando datos financieros...")

            when (val result = repository.getFullMetrics()) {
                is FinanceRepository.Result.Success -> {
                    Log.d(TAG, "Datos cargados")
                    _uiState.update {
                        it.copy(
                            summary = result.data.summary,
                            dailyRevenue = result.data.dailyRevenue,
                            revenueByColmado = result.data.revenueByColmado,
                            isLoading = false
                        )
                    }
                }
                is FinanceRepository.Result.Error -> {
                    Log.e(TAG, "Error: ${result.message}")
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, hasRealtimeUpdate = false) }
            Log.d(TAG, "Actualizando datos...")

            when (val result = repository.getFullMetrics()) {
                is FinanceRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            summary = result.data.summary,
                            dailyRevenue = result.data.dailyRevenue,
                            revenueByColmado = result.data.revenueByColmado,
                            isRefreshing = false
                        )
                    }
                }
                is FinanceRepository.Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    fun setPeriod(period: DatePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadFinanceData()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearRealtimeUpdate() {
        _uiState.update { it.copy(hasRealtimeUpdate = false) }
    }

    val maxRevenue: Double
        get() = _uiState.value.dailyRevenue.maxOfOrNull { it.revenue }.takeIf { it != null && it > 0 } ?: 1.0

    val successRate: Float
        get() {
            val summary = _uiState.value.summary ?: return 0f
            val total = summary.successfulPayments + summary.failedPayments
            return if (total > 0) summary.successfulPayments.toFloat() / total else 0f
        }

    override fun onCleared() {
        Log.d(TAG, "Limpiando ViewModel...")
        val channel = realtimeChannel
        if (channel != null) {
            viewModelScope.launch {
                try {
                    channel.unsubscribe()
                    SupabaseClient.client.realtime.removeChannel(channel)
                    Log.d(TAG, "Canal realtime cerrado")
                } catch (e: Exception) {
                    Log.e(TAG, "Error cerrando canal: ${e.message}")
                }
            }
        }
        repository.cleanup()
        super.onCleared()
    }
}
