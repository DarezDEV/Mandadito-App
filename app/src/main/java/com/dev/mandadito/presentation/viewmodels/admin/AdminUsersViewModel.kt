package com.dev.mandadito.presentation.viewmodels.admin

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.network.AdminRepository
import com.dev.mandadito.data.network.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val users: List<UserProfile> = emptyList(),
    val filteredUsers: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val selectedRoleFilter: Role? = null,
    val showDisabledOnly: Boolean = false,
    val currentUserId: String? = null
)

class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {

    private val adminRepository = AdminRepository(application)
    private val authRepository = AuthRepository(application)
    private val TAG = "AdminUsersViewModel"

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        loadUsers()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession()
            _uiState.update { it.copy(currentUserId = session.userId) }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.getAllUsers()) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            users = result.data,
                            isLoading = false
                        )
                    }
                    applyFilters()
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    Log.e(TAG, "Error cargando usuarios: ${result.message}")
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.createUser(
                email, password, nombre, telefono, role
            )) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Usuario creado exitosamente"
                        )
                    }
                    loadUsers() // Recargar la lista
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun updateUser(
        userId: String,
        nombre: String,
        telefono: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.updateUserProfile(
                userId, nombre, telefono
            )) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Usuario actualizado exitosamente"
                        )
                    }
                    loadUsers()
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun disableUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.disableUser(userId, currentUserId)) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Usuario deshabilitado"
                        )
                    }
                    loadUsers()
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun enableUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.enableUser(userId)) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Usuario habilitado"
                        )
                    }
                    loadUsers()
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.deleteUser(userId, currentUserId)) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Usuario eliminado"
                        )
                    }
                    loadUsers()
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun changeUserRole(userId: String, newRole: Role) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.changeUserRole(userId, newRole)) {
                is AdminRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Rol actualizado"
                        )
                    }
                    loadUsers()
                }
                is AdminRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setRoleFilter(role: Role?) {
        _uiState.update {
            it.copy(
                selectedRoleFilter = role,
                showDisabledOnly = false
            )
        }
        applyFilters()
    }

    fun setShowDisabledOnly(showDisabled: Boolean) {
        _uiState.update {
            it.copy(
                showDisabledOnly = showDisabled,
                selectedRoleFilter = if (showDisabled) null else it.selectedRoleFilter
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        val filtered = currentState.users.filter { user ->
            val matchesSearch = if (currentState.searchQuery.isEmpty()) {
                true
            } else {
                user.nombre.contains(currentState.searchQuery, ignoreCase = true) ||
                        user.email.contains(currentState.searchQuery, ignoreCase = true)
            }

            val matchesRole = if (currentState.selectedRoleFilter == null) {
                true
            } else {
                user.role == currentState.selectedRoleFilter
            }

            val matchesStatus = if (currentState.showDisabledOnly) {
                !user.activo
            } else {
                user.activo
            }

            matchesSearch && matchesRole && matchesStatus
        }

        _uiState.update { it.copy(filteredUsers = filtered) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}