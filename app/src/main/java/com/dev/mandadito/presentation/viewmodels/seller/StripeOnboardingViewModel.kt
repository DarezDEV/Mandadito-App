package com.dev.mandadito.presentation.viewmodels.seller

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.repository.SellerRepository
import com.dev.mandadito.data.repository.StripeRepository
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.utils.SharedPreferenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StripeOnboardingUiState(
    val checkingStatus: Boolean = false,  // Cambiado a false para permitir el primer check
    val needsOnboarding: Boolean = false,
    val onboardingUrl: String? = null,
    val stripeReady: Boolean = false,
    val error: String? = null,
    val creatingAccount: Boolean = false
)

class StripeOnboardingViewModel(context: Context) : ViewModel() {

    private val stripeRepository = StripeRepository(context)
    private val sellerRepository = SellerRepository(context)
    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val TAG = "StripeOnboardingVM"

    private val _uiState = MutableStateFlow(StripeOnboardingUiState())
    val uiState: StateFlow<StripeOnboardingUiState> = _uiState.asStateFlow()

    private var hasChecked = false // Evitar múltiples checks simultáneos

    /**
     * Verifica si el seller tiene Stripe configurado
     */
    fun checkStripeStatus() {
        Log.d(TAG, "🔍 checkStripeStatus() llamado - checkingStatus=${_uiState.value.checkingStatus}, hasChecked=$hasChecked")

        // Evitar múltiples checks simultáneos
        if (_uiState.value.checkingStatus || hasChecked) {
            Log.d(TAG, "⏭️ Check ya en progreso o completado, saltando...")
            return
        }
        hasChecked = true
        Log.d(TAG, "✅ Iniciando verificación de Stripe...")

        viewModelScope.launch {
            _uiState.update { it.copy(checkingStatus = true, error = null) }
            Log.d(TAG, "📊 Estado actualizado: checkingStatus=true")

            try {
                // Obtener colmado_id del seller
                val userId = sharedPrefsHelper.getUserId()
                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            checkingStatus = false,
                            error = "Usuario no autenticado"
                        )
                    }
                    return@launch
                }

                val colmadoId = getColmadoId(userId)
                if (colmadoId == null) {
                    _uiState.update {
                        it.copy(
                            checkingStatus = false,
                            error = "No se encontró el colmado"
                        )
                    }
                    return@launch
                }

                // Verificar estado de Stripe
                when (val result = stripeRepository.checkStripeStatus(colmadoId)) {
                    is StripeRepository.Result.Success -> {
                        val response = result.data
                        val isReady = response.onboardingCompleted == true &&
                                     response.chargesEnabled == true

                        Log.d(TAG, "✅ Estado Stripe - Ready: $isReady, Onboarding: ${response.onboardingCompleted}")

                        _uiState.update {
                            it.copy(
                                checkingStatus = false,
                                needsOnboarding = !isReady,
                                stripeReady = isReady,
                                onboardingUrl = response.onboardingUrl
                            )
                        }
                        Log.d(TAG, "📊 Estado final: stripeReady=$isReady, needsOnboarding=${!isReady}")
                    }
                    is StripeRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error verificando estado: ${result.message}")

                        // Si el error es 400/404, probablemente el endpoint no existe o la cuenta no está creada
                        // En ese caso, mostrar la opción de crear cuenta
                        val isEndpointError = result.message.contains("400") || result.message.contains("404")

                        _uiState.update {
                            it.copy(
                                checkingStatus = false,
                                needsOnboarding = true,
                                stripeReady = false,
                                error = if (isEndpointError) {
                                    "No tienes una cuenta de Stripe configurada"
                                } else {
                                    result.message
                                }
                            )
                        }
                        Log.d(TAG, "📊 Estado final (error): stripeReady=false, needsOnboarding=true, error=${if (isEndpointError) "No hay cuenta Stripe" else result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción verificando Stripe: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        checkingStatus = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    /**
     * Crea la cuenta Stripe y obtiene el link de onboarding
     */
    fun createStripeAccount(businessName: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(creatingAccount = true, error = null) }

            try {
                val userId = sharedPrefsHelper.getUserId()
                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            creatingAccount = false,
                            error = "Usuario no autenticado"
                        )
                    }
                    return@launch
                }

                val colmadoId = getColmadoId(userId)
                if (colmadoId == null) {
                    _uiState.update {
                        it.copy(
                            creatingAccount = false,
                            error = "No se encontró el colmado"
                        )
                    }
                    return@launch
                }

                when (val result = stripeRepository.createStripeAccount(
                    colmadoId = colmadoId,
                    email = email,
                    businessName = businessName
                )) {
                    is StripeRepository.Result.Success -> {
                        Log.d(TAG, "✅ Cuenta Stripe creada, URL: ${result.data.onboardingUrl}")
                        _uiState.update {
                            it.copy(
                                creatingAccount = false,
                                onboardingUrl = result.data.onboardingUrl,
                                needsOnboarding = true
                            )
                        }
                    }
                    is StripeRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error creando cuenta: ${result.message}")
                        _uiState.update {
                            it.copy(
                                creatingAccount = false,
                                error = result.message
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción creando cuenta: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        creatingAccount = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    /**
     * Refresca el link de onboarding si expiró
     */
    fun refreshOnboardingLink() {
        viewModelScope.launch {
            _uiState.update { it.copy(creatingAccount = true, error = null) }

            try {
                val userId = sharedPrefsHelper.getUserId()
                val colmadoId = getColmadoId(userId ?: return@launch)

                when (val result = stripeRepository.refreshOnboardingLink(colmadoId ?: return@launch)) {
                    is StripeRepository.Result.Success -> {
                        _uiState.update {
                            it.copy(
                                creatingAccount = false,
                                onboardingUrl = result.data.onboardingUrl
                            )
                        }
                    }
                    is StripeRepository.Result.Error -> {
                        _uiState.update {
                            it.copy(
                                creatingAccount = false,
                                error = result.message
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        creatingAccount = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private suspend fun getColmadoId(userId: String): String? {
        return when (val result = sellerRepository.getSellerColmadoId(userId)) {
            is SellerRepository.Result.Success -> result.data
            is SellerRepository.Result.Error -> null
        }
    }
}
