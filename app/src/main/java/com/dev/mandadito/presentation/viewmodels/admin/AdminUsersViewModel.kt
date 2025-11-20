package com.dev.mandadito.presentation.viewmodels.admin

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.network.AdminRepository
import com.dev.mandadito.utils.SharedPreferenHelper
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
    private val TAG = "AdminUsersViewModel"

    private val _uiState = MutableStateFlow(AdminUsersUiState(isLoading = true))
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): String? {
        return SharedPreferenHelper(context).getUserId()
    }

    init {
        loadUsers()
    }

    // ==========================================
    // CARGAR USUARIOS
    // ==========================================
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "📥 Cargando usuarios...")

            when (val result = repository.getAllUsers()) {
                is AdminRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} usuarios cargados")
                    _uiState.update {
                        it.copy(
                            users = result.data,
                            isLoading = false,
                            successMessage = "Usuarios cargados"
                        )
                    }
                }
                is AdminRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando usuarios: ${result.message}")
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

    // ==========================================
    // CREAR USUARIO (CON AVATAR)
    // ==========================================
    fun createUser(
        email: String,
        password: String,
        nombre: String,
        role: Role,
        avatarUri: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔷 Creando usuario: $email con rol: ${role.value}")
            if (avatarUri != null) {
                Log.d(TAG, "📸 Avatar URI proporcionado: $avatarUri")
            } else {
                Log.d(TAG, "ℹ️ No se proporcionó avatar")
            }

            when (val result = repository.createUser(
                email = email,
                password = password,
                nombre = nombre,
                role = role,
                avatarUri = avatarUri
            )) {
                is AdminRepository.Result.Success -> {
                    Log.d(TAG, "✅ Usuario creado exitosamente: ${result.data.email}")
                    if (result.data.avatar_url != null) {
                        Log.d(TAG, "🖼️ Avatar URL: ${result.data.avatar_url}")
                    } else {
                        Log.d(TAG, "ℹ️ Usuario creado sin avatar")
                    }

                    _uiState.update {
                        it.copy(
                            users = it.users + result.data,
                            isLoading = false,
                            successMessage = "Usuario creado: ${result.data.email}"
                        )
                    }
                }
                is AdminRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error creando usuario: ${result.message}")
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

    // ==========================================
    // ACTUALIZAR PERFIL DE USUARIO
    // ==========================================
    fun updateUserProfile(userId: String, nombre: String) {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Actualizando perfil de usuario: $userId")

            when (val result = repository.updateUserProfile(userId, nombre)) {
                is AdminRepository.Result.Success -> {
                    Log.d(TAG, "✅ Perfil actualizado exitosamente")
                    loadUsers()
                    _uiState.update {
                        it.copy(successMessage = "Perfil actualizado")
                    }
                }
                is AdminRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error actualizando perfil: ${result.message}")
                    _uiState.update {
                        it.copy(error = result.message)
                    }
                }
            }
        }
    }

    // ==========================================
    // DESHABILITAR USUARIO
    // ==========================================
    fun disableUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId() ?: return@launch

            Log.d(TAG, "🚫 Deshabilitando usuario: $userId")

            when (val result = repository.disableUser(userId, currentUserId)) {
                is AdminRepository.Result.Success -> {
                    Log.d(TAG, "✅ Usuario deshabilitado exitosamente")
                    loadUsers()
                    _uiState.update {
                        it.copy(successMessage = "Usuario deshabilitado")
                    }
                }
                is AdminRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error deshabilitando usuario: ${result.message}")
                    _uiState.update {
                        it.copy(error = result.message)
                    }
                }
            }
        }
    }

    // ==========================================
    // HABILITAR USUARIO
    // ==========================================
    fun enableUser(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "✅ Habilitando usuario: $userId")

            when (val result = repository.enableUser(userId)) {
                is AdminRepository.Result.Success -> {
                    Log.d(TAG, "✅ Usuario habilitado exitosamente")
                    loadUsers()
                    _uiState.update {
                        it.copy(successMessage = "Usuario habilitado")
                    }
                }
                is AdminRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error habilitando usuario: ${result.message}")
                    _uiState.update {
                        it.copy(error = result.message)
                    }
                }
            }
        }
    }

    // ==========================================
    // ELIMINAR USUARIO
    // ==========================================
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId() ?: return@launch

            Log.d(TAG, "🗑️ Eliminando usuario: $userId")

            when (val result = repository.deleteUser(userId, currentUserId)) {
                is AdminRepository.Result.Success -> {
                    Log.d(TAG, "✅ Usuario eliminado exitosamente")
                    loadUsers()
                    _uiState.update {
                        it.copy(successMessage = "Usuario eliminado")
                    }
                }
                is AdminRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error eliminando usuario: ${result.message}")
                    _uiState.update {
                        it.copy(error = result.message)
                    }
                }
            }
        }
    }

    // ==========================================
    // CAMBIAR ROL DE USUARIO
    // ==========================================
    fun changeUserRole(userId: String, newRole: Role) {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Cambiando rol de usuario: $userId a ${newRole.value}")

            when (val result = repository.changeUserRole(userId, newRole)) {
                is AdminRepository.Result.Success -> {
                    Log.d(TAG, "✅ Rol actualizado exitosamente")
                    loadUsers()
                    _uiState.update {
                        it.copy(successMessage = "Rol actualizado")
                    }
                }
                is AdminRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cambiando rol: ${result.message}")
                    _uiState.update {
                        it.copy(error = result.message)
                    }
                }
            }
        }
    }

    // ==========================================
    // FILTROS Y BÚSQUEDA
    // ==========================================
    fun setSearchQuery(query: String) {
        Log.d(TAG, "🔍 Búsqueda actualizada: $query")
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowDisabledOnly(show: Boolean) {
        Log.d(TAG, "👁️ Mostrar deshabilitados: $show")
        _uiState.update { it.copy(showDisabledOnly = show) }
    }

    fun setRoleFilter(role: Role?) {
        Log.d(TAG, "🎭 Filtro de rol: ${role?.value ?: "Todos"}")
        _uiState.update { it.copy(selectedRoleFilter = role) }
    }

    // ==========================================
    // LIMPIAR MENSAJES
    // ==========================================
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ==========================================
    // USUARIOS FILTRADOS
    // ==========================================
    val filteredUsers: List<UserProfile>
        get() {
            val query = _uiState.value.searchQuery.lowercase()
            return _uiState.value.users.filter { user ->
                // Filtro de búsqueda por email o nombre
                val matchesSearch = user.email.lowercase().contains(query) ||
                        user.nombre.lowercase().contains(query)

                // Filtro de usuarios activos/deshabilitados
                val matchesDisabled = if (_uiState.value.showDisabledOnly) {
                    !user.activo
                } else {
                    user.activo
                }

                // Filtro de rol
                val matchesRole = _uiState.value.selectedRoleFilter?.let {
                    user.role == it
                } ?: true

                matchesSearch && matchesDisabled && matchesRole
            }
        }

    // ==========================================
    // CLEANUP
    // ==========================================
    override fun onCleared() {
        Log.d(TAG, "🧹 Limpiando ViewModel")
        repository.cleanup()
        super.onCleared()
    }
}