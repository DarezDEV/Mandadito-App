package com.dev.mandadito.presentation.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.RegisterData
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.network.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Verificar si hay una sesión activa al iniciar
        checkActiveSession()
    }

    private companion object {
        const val TAG = "AuthViewModel"
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_NAME_LENGTH = 50
        const val MAX_EMAIL_LENGTH = 100
    }

    // Patrones de validación profesional
    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )

    private val PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$"
    )

    /**
     * Verifica si hay una sesión activa
     * Solo se ejecuta si el estado actual indica que no hay sesión
     */
    private fun checkActiveSession() {
        // Solo verificar si actualmente no hay sesión activa en el estado
        // Esto evita verificar después de un logout reciente
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch {
                try {
                    // Verificar si hay una sesión activa (primero Supabase, luego SharedPreferences)
                    val hasSession = authRepository.hasActiveSession()
                    
                    if (hasSession) {
                        // Verificar que realmente haya una sesión válida en Supabase
                        val currentUser = authRepository.getCurrentUser()
                        val currentRole = authRepository.getCurrentUserRole()
                        
                        if (currentUser != null && currentRole != null) {
                            // Hay una sesión válida, restaurar el estado
                            val session = authRepository.getCurrentSession()
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                userRole = currentRole
                            )
                            Log.d(TAG, "Sesión activa restaurada: ${session.email} - ${currentRole.value}")
                        } else {
                            // No hay sesión válida en Supabase, limpiar
                            Log.d(TAG, "No se pudo obtener usuario o rol, limpiando sesión...")
                            authRepository.logout()
                            _uiState.value = AuthUiState()
                        }
                    } else {
                        Log.d(TAG, "No hay sesión activa")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verificando sesión activa: ${e.message}", e)
                    // En caso de error, limpiar todo para evitar estados inconsistentes
                    _uiState.value = AuthUiState()
                }
            }
        }
    }

    /**
     * Valida formato de email
     */
    fun isValidEmail(email: String): ValidationResult {
        when {
            email.isBlank() -> return ValidationResult.Error("El correo electrónico es requerido")
            email.length > MAX_EMAIL_LENGTH -> return ValidationResult.Error("El correo electrónico es demasiado largo")
            !EMAIL_PATTERN.matcher(email).matches() -> return ValidationResult.Error("Ingresa un correo electrónico válido")
            else -> return ValidationResult.Success
        }
    }

    /**
     * Valida contraseña
     */
    fun isValidPassword(password: String): ValidationResult {
        when {
            password.isBlank() -> return ValidationResult.Error("La contraseña es requerida")
            password.length < MIN_PASSWORD_LENGTH -> return ValidationResult.Error("La contraseña debe tener al menos 8 caracteres")
            password.length > 128 -> return ValidationResult.Error("La contraseña es demasiado larga")
            !PASSWORD_PATTERN.matcher(password).matches() -> return ValidationResult.Error("La contraseña debe contener mayúsculas, minúsculas y números")
            else -> return ValidationResult.Success
        }
    }

    /**
     * Valida confirmación de contraseña
     */
    fun isValidPasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        when {
            confirmPassword.isBlank() -> return ValidationResult.Error("Confirma tu contraseña")
            password != confirmPassword -> return ValidationResult.Error("Las contraseñas no coinciden")
            else -> return ValidationResult.Success
        }
    }

    /**
     * Valida nombre
     */
    fun isValidName(name: String): ValidationResult {
        when {
            name.isBlank() -> return ValidationResult.Error("El nombre es requerido")
            name.length < 2 -> return ValidationResult.Error("El nombre es demasiado corto")
            name.length > MAX_NAME_LENGTH -> return ValidationResult.Error("El nombre es demasiado largo")
            !name.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> return ValidationResult.Error("El nombre solo puede contener letras")
            else -> return ValidationResult.Success
        }
    }

    // Sealed class para resultados de validación
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun register(
        nombre: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        // Limpiar estados previos
        clearError()
        clearSuccess()

        // Validar nombre
        val nameValidation = isValidName(nombre.trim())
        if (nameValidation is ValidationResult.Error) {
            _uiState.value = _uiState.value.copy(error = nameValidation.message)
            return
        }

        // Validar email
        val emailValidation = isValidEmail(email.trim())
        if (emailValidation is ValidationResult.Error) {
            _uiState.value = _uiState.value.copy(error = emailValidation.message)
            return
        }

        // Validar contraseña
        val passwordValidation = isValidPassword(password)
        if (passwordValidation is ValidationResult.Error) {
            _uiState.value = _uiState.value.copy(error = passwordValidation.message)
            return
        }

        // Validar confirmación de contraseña
        val passwordConfirmationValidation = isValidPasswordConfirmation(password, confirmPassword)
        if (passwordConfirmationValidation is ValidationResult.Error) {
            _uiState.value = _uiState.value.copy(error = passwordConfirmationValidation.message)
            return
        }

        // Iniciar registro
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val registerData = RegisterData(
                    nombre = nombre.trim(),
                    email = email.trim().lowercase(),
                    password = password
                )

                val result = authRepository.register(registerData)

                when (result) {
                    is AuthRepository.AuthResult.Success -> {
                        Log.d(TAG, "Registro exitoso - Redirigiendo a login")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRegistered = true,
                            error = null,
                            successMessage = "¡Cuenta creada exitosamente! Inicia sesión para continuar",
                            showSuccessDialog = true
                        )
                    }
                    is AuthRepository.AuthResult.NeedsConfirm -> {
                        Log.d(TAG, "Registro exitoso - Requiere confirmación de email")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRegistered = true,
                            error = null,
                            successMessage = result.message,
                            showSuccessDialog = true
                        )
                    }
                    is AuthRepository.AuthResult.Error -> {
                        Log.e(TAG, "Error en registro: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en registro: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error de conexión. Verifica tu internet e intenta nuevamente."
                )
            }
        }
    }

    fun login(email: String, password: String) {
        // Limpiar estados previos
        clearError()
        clearSuccess()

        // Validar email
        val emailValidation = isValidEmail(email.trim())
        if (emailValidation is ValidationResult.Error) {
            _uiState.value = _uiState.value.copy(error = emailValidation.message)
            return
        }

        // Validar contraseña básica (no aplicar todas las reglas en login)
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "La contraseña es requerida")
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val result = authRepository.login(email.trim().lowercase(), password)

                when (result) {
                    is AuthRepository.LoginResult.Success -> {
                        Log.d(TAG, "Login exitoso con rol: ${result.role.value}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userRole = result.role,
                            error = null,
                            successMessage = "¡Bienvenido de nuevo!"
                        )
                    }
                    is AuthRepository.LoginResult.Error -> {
                        Log.e(TAG, "Error en login: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en login: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error de conexión. Verifica tu internet e intenta nuevamente."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            showSuccessDialog = false
        )
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            showSuccessDialog = false
        )
    }

    fun clearFieldErrors() {
        _uiState.value = _uiState.value.copy(fieldErrors = emptyMap())
    }

    fun setFieldError(field: String, error: String) {
        _uiState.value = _uiState.value.copy(
            fieldErrors = _uiState.value.fieldErrors + (field to error)
        )
    }

    fun clearFieldError(field: String) {
        val newErrors = _uiState.value.fieldErrors.toMutableMap().apply {
            remove(field)
        }
        _uiState.value = _uiState.value.copy(fieldErrors = newErrors)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Primero resetear el estado inmediatamente para evitar redirecciones
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    userRole = null,
                    error = null
                )
                
                // Luego cerrar sesión en el repositorio
                authRepository.logout()
                
                Log.d(TAG, "Logout exitoso - Estado reseteado")
            } catch (e: Exception) {
                Log.e(TAG, "Error en logout: ${e.message}", e)
                // Asegurar que el estado esté reseteado incluso si hay error
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    userRole = null,
                    error = null
                )
            }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRole: Role? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val showSuccessDialog: Boolean = false,
    // Errores de campos específicos para validación en tiempo real
    val fieldErrors: Map<String, String> = emptyMap()
)