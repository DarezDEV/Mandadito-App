// AdminUsersViewModel.kt - VERSIÓN CORRECTA Y LIMPIA
package com.dev.mandadito.presentation.viewmodels.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.network.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val showDisabledOnly: Boolean = false,
    val selectedRoleFilter: Role? = null
)

class AdminUsersViewModel(context: Context) : ViewModel() {
    private val repository = AdminRepository(context)
    private val context = context

    private val _uiState = MutableStateFlow(AdminUsersUiState(isLoading = true))
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()
    
    private fun getCurrentUserId(): String? {
        return com.dev.mandadito.utils.SharedPreferenHelper(context).getUserId()
    }

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getAllUsers()) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            users = result.data,
                            isLoading = false,
                            successMessage = "Usuarios cargados"
                        )
                    }
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun createUser(
        email: String,
        password: String,
        nombre: String,
        telefono: String?,
        role: Role
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.createUser(email, password, nombre, telefono, role)) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            users = it.users + result.data,
                            isLoading = false,
                            successMessage = "Usuario creado: ${result.data.email}"
                        )
                    }
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun updateUserProfile(userId: String, nombre: String, telefono: String?) {
        viewModelScope.launch {
            when (val result = repository.updateUserProfile(userId, nombre, telefono)) {
                is AdminRepository.Result.Success -> {
                    loadUsers()
                    _uiState.update { it.copy(successMessage = "Perfil actualizado") }
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun disableUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId() ?: return@launch
            when (val result = repository.disableUser(userId, currentUserId)) {
                is AdminRepository.Result.Success -> {
                    loadUsers()
                    _uiState.update { it.copy(successMessage = "Usuario deshabilitado") }
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun enableUser(userId: String) {
        viewModelScope.launch {
            when (val result = repository.enableUser(userId)) {
                is AdminRepository.Result.Success -> {
                    loadUsers()
                    _uiState.update { it.copy(successMessage = "Usuario habilitado") }
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId() ?: return@launch
            when (val result = repository.deleteUser(userId, currentUserId)) {
                is AdminRepository.Result.Success -> {
                    loadUsers()
                    _uiState.update { it.copy(successMessage = "Usuario eliminado") }
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun changeUserRole(userId: String, newRole: Role) {
        viewModelScope.launch {
            when (val result = repository.changeUserRole(userId, newRole)) {
                is AdminRepository.Result.Success -> {
                    loadUsers()
                    _uiState.update { it.copy(successMessage = "Rol actualizado") }
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowDisabledOnly(show: Boolean) {
        _uiState.update { it.copy(showDisabledOnly = show) }
    }

    fun setRoleFilter(role: Role?) {
        _uiState.update { it.copy(selectedRoleFilter = role) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    val filteredUsers: List<UserProfile>
        get() {
            val query = _uiState.value.searchQuery.lowercase()
            return _uiState.value.users.filter { user ->
                val matchesSearch = user.email.lowercase().contains(query) ||
                        user.nombre.lowercase().contains(query) ||
                        user.telefono?.contains(query) == true

                val matchesDisabled = if (_uiState.value.showDisabledOnly) {
                    !user.activo
                } else true

                val matchesRole = _uiState.value.selectedRoleFilter?.let {
                    user.role == it
                } ?: true

                matchesSearch && matchesDisabled && matchesRole
            }
        }

    override fun onCleared() {
        repository.cleanup()
        super.onCleared()
    }
}