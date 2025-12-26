package com.dev.mandadito.presentation.viewmodels.admin

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.repository.ProfileRepository
import com.dev.mandadito.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminProfileViewModel(context: Context) : ViewModel() {

    private val profileRepository = ProfileRepository(context)
    private val authRepository = AuthRepository(context)
    private val TAG = "AdminProfileViewModel"

    private val _uiState = MutableStateFlow(AdminProfileUiState())
    val uiState: StateFlow<AdminProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = profileRepository.getCurrentUserProfile()) {
                is ProfileRepository.Result.Success -> {
                    Log.d(TAG, "✅ Perfil cargado: ${result.data.nombre}")
                    _uiState.value = _uiState.value.copy(
                        userProfile = result.data,
                        isLoading = false,
                        error = null
                    )
                }
                is ProfileRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando perfil: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun updateProfile(
        nombre: String,
        email: String,
        avatarUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, updateError = null)

            when (val result = profileRepository.updateProfile(nombre, email, avatarUri)) {
                is ProfileRepository.Result.Success -> {
                    Log.d(TAG, "✅ Perfil actualizado exitosamente")
                    _uiState.value = _uiState.value.copy(
                        userProfile = result.data,
                        isUpdating = false,
                        updateError = null
                    )
                    onSuccess()
                }
                is ProfileRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error actualizando perfil: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateError = result.message
                    )
                    onError(result.message)
                }
            }
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = profileRepository.changePassword(currentPassword, newPassword)) {
                is ProfileRepository.Result.Success -> {
                    Log.d(TAG, "✅ Contraseña cambiada exitosamente")
                    onSuccess()
                }
                is ProfileRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cambiando contraseña: ${result.message}")
                    onError(result.message)
                }
            }
        }
    }

    fun clearUpdateError() {
        _uiState.value = _uiState.value.copy(updateError = null)
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (authRepository.logout()) {
                is AuthRepository.AuthResult.Success -> {
                    Log.d(TAG, "✅ Sesión cerrada exitosamente")
                    onSuccess()
                }
                is AuthRepository.AuthResult.Error -> {
                    Log.e(TAG, "❌ Error cerrando sesión")
                    // Aun así navegar al login
                    onSuccess()
                }
                else -> {}
            }
        }
    }
}

data class AdminProfileUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val updateError: String? = null
)
