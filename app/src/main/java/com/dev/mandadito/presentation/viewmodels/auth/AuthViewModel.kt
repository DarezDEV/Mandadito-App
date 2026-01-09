package com.dev.mandadito.presentation.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.RegisterData
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.repository.AuthRepository
import com.dev.mandadito.data.repository.SellerRepository
import com.dev.mandadito.data.repository.StripeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AuthViewModel(
    application: Application,
    sessionAlreadyChecked: Boolean = false,
    hasActiveSession: Boolean = false,
    initialUserRole: Role? = null
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val sellerRepository = SellerRepository(application)
    private val stripeRepository = StripeRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Si la sesión ya fue verificada por SplashActivity, usar esos datos
        if (sessionAlreadyChecked) {
            if (hasActiveSession && initialUserRole != null) {
                // Establecer el estado inmediatamente con el rol proporcionado
                _uiState.value = _uiState.value.copy(
                    isCheckingSession = false,
                    isLoggedIn = true,
                    userRole = initialUserRole
                )
                Log.d(TAG, "Sesión restaurada desde SplashActivity con rol: ${initialUserRole.value}")

                // Si es SELLER, verificar estado de Stripe
                if (initialUserRole == Role.SELLER) {
                    checkSellerStripeStatus()
                }
            } else if (hasActiveSession && initialUserRole == null) {
                // Tenemos sesión pero no rol, obtenerlo del repositorio
                viewModelScope.launch {
                    try {
                        val currentRole = authRepository.getCurrentUserRole()
                        _uiState.value = _uiState.value.copy(
                            isCheckingSession = false,
                            isLoggedIn = true,
                            userRole = currentRole
                        )
                        Log.d(TAG, "Sesión restaurada desde SplashActivity con rol: ${currentRole?.value}")

                        // Si es SELLER, verificar estado de Stripe
                        if (currentRole == Role.SELLER) {
                            checkSellerStripeStatus()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error obteniendo rol de usuario: ${e.message}", e)
                        _uiState.value = AuthUiState(isCheckingSession = false)
                    }
                }
            } else {
                // No hay sesión activa según SplashActivity
                _uiState.value = AuthUiState(isCheckingSession = false)
                Log.d(TAG, "Sin sesión activa según SplashActivity")
            }
        } else {
            // Verificar si hay una sesión activa (modo legacy)
            checkActiveSession()
        }
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
                    // Marcar que estamos verificando sesión
                    _uiState.value = _uiState.value.copy(isCheckingSession = true)

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
                                isCheckingSession = false,
                                isLoggedIn = true,
                                userRole = currentRole
                            )
                            Log.d(TAG, "Sesión activa restaurada: ${session.email} - ${currentRole.value}")

                            // Si es SELLER, verificar estado de Stripe
                            if (currentRole == Role.SELLER) {
                                checkSellerStripeStatus()
                            }
                        } else {
                            // No hay sesión válida en Supabase, limpiar
                            Log.d(TAG, "No se pudo obtener usuario o rol, limpiando sesión...")
                            authRepository.logout()
                            _uiState.value = AuthUiState(isCheckingSession = false)
                        }
                    } else {
                        Log.d(TAG, "No hay sesión activa")
                        _uiState.value = _uiState.value.copy(isCheckingSession = false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verificando sesión activa: ${e.message}", e)
                    // En caso de error, limpiar todo para evitar estados inconsistentes
                    _uiState.value = AuthUiState(isCheckingSession = false)
                }
            }
        } else {
            // Si ya hay sesión, no necesitamos verificar
            _uiState.value = _uiState.value.copy(isCheckingSession = false)
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

                        // Si es SELLER, verificar estado de Stripe antes de completar login
                        if (result.role == Role.SELLER) {
                            checkSellerStripeStatus()
                        } else {
                            // Para otros roles, completar login normalmente
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                userRole = result.role,
                                error = null,
                                successMessage = "¡Bienvenido de nuevo!",
                                stripeConfigured = null // No aplica para otros roles
                            )
                        }
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

    /**
     * Verifica el estado de Stripe para un seller después de login
     */
    private fun checkSellerStripeStatus() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 Verificando estado de Stripe para seller...")

                // Obtener el usuario actual
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    Log.e(TAG, "❌ No se pudo obtener usuario actual")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userRole = Role.SELLER,
                        stripeConfigured = false, // Asumir no configurado si hay error
                        successMessage = "¡Bienvenido de nuevo!"
                    )
                    return@launch
                }

                // Obtener el colmado_id del seller
                when (val colmadoResult = sellerRepository.getSellerColmadoId(currentUser.id)) {
                    is SellerRepository.Result.Success -> {
                        val colmadoId = colmadoResult.data
                        Log.d(TAG, "✅ Colmado ID obtenido: $colmadoId")

                        // Verificar estado de Stripe
                        when (val stripeResult = stripeRepository.checkStripeStatus(colmadoId)) {
                            is StripeRepository.Result.Success -> {
                                val stripeStatus = stripeResult.data
                                val isConfigured = stripeStatus.onboardingCompleted == true && stripeStatus.chargesEnabled == true
                                Log.d(TAG, "✅ Estado Stripe - Configurado: $isConfigured (onboarding: ${stripeStatus.onboardingCompleted}, charges: ${stripeStatus.chargesEnabled})")

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    userRole = Role.SELLER,
                                    stripeConfigured = isConfigured,
                                    error = null,
                                    successMessage = "¡Bienvenido de nuevo!"
                                )
                            }
                            is StripeRepository.Result.Error -> {
                                Log.e(TAG, "❌ Error verificando Stripe: ${stripeResult.message}")
                                // Si hay error, asumir que no está configurado para mostrar la pantalla
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    userRole = Role.SELLER,
                                    stripeConfigured = false,
                                    successMessage = "¡Bienvenido de nuevo!"
                                )
                            }
                        }
                    }
                    is SellerRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error obteniendo colmado: ${colmadoResult.message}")
                        // Si no tiene colmado, definitivamente no tiene Stripe configurado
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userRole = Role.SELLER,
                            stripeConfigured = false,
                            successMessage = "¡Bienvenido de nuevo!"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción verificando Stripe: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    userRole = Role.SELLER,
                    stripeConfigured = false, // Asumir no configurado en caso de error
                    successMessage = "¡Bienvenido de nuevo!"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Primero resetear el estado inmediatamente para evitar redirecciones
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isCheckingSession = false, // No verificar sesión después de logout
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
                    isCheckingSession = false,
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
    val isCheckingSession: Boolean = true, // TRUE por defecto para verificar sesión al iniciar
    val isRegistered: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRole: Role? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val showSuccessDialog: Boolean = false,
    // Errores de campos específicos para validación en tiempo real
    val fieldErrors: Map<String, String> = emptyMap(),
    // Estado de Stripe para sellers (null = no verificado/no aplica, true = configurado, false = no configurado)
    val stripeConfigured: Boolean? = null
)