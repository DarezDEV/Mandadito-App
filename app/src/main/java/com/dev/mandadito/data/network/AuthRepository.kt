package com.dev.mandadito.data.network

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.RegisterData
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.RoleRecord
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.models.UserRole
import com.dev.mandadito.utils.SharedPreferenHelper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(private val context: Context) {

    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val supabase = SupabaseClient.client
    private val TAG = "AuthRepository"

    sealed class Result {
        object Success : Result()
        data class NeedsConfirm(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    sealed class LoginResult {
        data class Success(val role: Role) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    // ==========================================
    // REGISTRO CON VALIDACIÓN MEJORADA
    // ==========================================
    suspend fun register(registerData: RegisterData): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando registro para: ${registerData.email}")

            if (!isSupabaseConfigured()) {
                Log.e(TAG, "Supabase no está configurado correctamente")
                return@withContext Result.Error("Error de configuración. Contacta al administrador.")
            }


            Log.d(TAG, "Registrando usuario con metadatos...")

            // Registrar usuario en Supabase Auth
            // El trigger handle_new_user() se encargará automáticamente de:
            // 1. Crear el perfil en la tabla profiles
            // 2. Asignar el rol 'client' en user_roles
            val authResponse = supabase.auth.signUpWith(Email) {
                email = registerData.email
                password = registerData.password
            }

            val userId = authResponse?.id
            Log.d(TAG, "Usuario registrado exitosamente con ID: $userId")

            // IMPORTANTE: Verificar que el trigger creó el perfil
            if (userId != null) {
                // Esperar a que el trigger termine (aumentado a 1 segundo)
                kotlinx.coroutines.delay(1000)

                // Verificar el perfil con reintentos
                var profile: UserProfile? = null
                var intentos = 0
                val maxIntentos = 3

                while (profile == null && intentos < maxIntentos) {
                    try {
                        profile = supabase.from("profiles")
                            .select {
                                filter { eq("id", userId) }
                            }
                            .decodeSingleOrNull<UserProfile>()

                        if (profile != null) {
                            Log.d(TAG, "✅ Perfil verificado: ${profile.nombre}")
                            break
                        }

                        intentos++
                        if (intentos < maxIntentos) {
                            Log.w(TAG, "Perfil no encontrado, reintento $intentos/$maxIntentos...")
                            kotlinx.coroutines.delay(1000)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error verificando perfil (intento $intentos): ${e.message}")
                        intentos++
                        if (intentos < maxIntentos) {
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                }

                // Si después de los reintentos no hay perfil, es un error crítico
                if (profile == null) {
                    Log.e(TAG, "❌ ERROR CRÍTICO: Perfil no creado después de $maxIntentos intentos")
                    // Intentar eliminar el usuario de auth para permitir reintento
                    try {
                        supabase.auth.signOut()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cerrando sesión: ${e.message}")
                    }
                    return@withContext Result.Error(
                        "Error al crear el perfil. Por favor intenta nuevamente o contacta al administrador."
                    )
                }
            }

            // Cerrar la sesión automática después del registro
            // para que el usuario tenga que hacer login manualmente
            try {
                supabase.auth.signOut()
                Log.d(TAG, "Sesión cerrada después del registro")
            } catch (e: Exception) {
                Log.w(TAG, "Error al cerrar sesión: ${e.message}")
            }

            Log.d(TAG, "Registro completado exitosamente")
            return@withContext Result.Success

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en registro: ${e.message}", e)

            val errorMessage = when {
                e.message?.contains("User already registered", ignoreCase = true) == true ||
                        e.message?.contains("already registered", ignoreCase = true) == true ||
                        e.message?.contains("already exists", ignoreCase = true) == true ||
                        e.message?.contains("duplicate key", ignoreCase = true) == true ->
                    "Este correo electrónico ya está registrado"

                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("unable to resolve host", ignoreCase = true) == true ||
                        e.message?.contains("failed to connect", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                e.message?.contains("invalid email", ignoreCase = true) == true ->
                    "Correo electrónico inválido"

                e.message?.contains("weak password", ignoreCase = true) == true ||
                        e.message?.contains("password", ignoreCase = true) == true ->
                    "La contraseña es muy débil. Usa al menos 8 caracteres con mayúsculas, minúsculas y números"

                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "La operación tardó demasiado. Intenta nuevamente"

                else -> {
                    Log.e(TAG, "Error no manejado: ${e.javaClass.simpleName}")
                    "Error al crear la cuenta: ${e.message ?: "Intenta nuevamente"}"
                }
            }

            return@withContext Result.Error(errorMessage)
        }
    }

    // ==========================================
    // LOGIN CON VALIDACIÓN ROBUSTA DE PERFIL
    // ==========================================
    suspend fun login(email: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando login para: $email")

            if (!isSupabaseConfigured()) {
                Log.e(TAG, "Supabase no está configurado correctamente")
                return@withContext LoginResult.Error("Error de configuración. Contacta al administrador.")
            }

            // 1. Verificar primero si el usuario existe y está activo (antes de autenticar)
            val preCheckProfile = try {
                supabase.from("profiles")
                    .select {
                        filter { eq("email", email) }
                    }
                    .decodeSingleOrNull<UserProfile>()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo verificar perfil pre-login: ${e.message}")
                null
            }

            // 2. Si existe el perfil, verificar que esté activo
            if (preCheckProfile != null && !preCheckProfile.activo) {
                Log.w(TAG, "Intento de login con cuenta deshabilitada: $email")
                return@withContext LoginResult.Error(
                    "Tu cuenta ha sido bloqueada. Contacta al administrador para más información."
                )
            }

            // 3. Autenticar con Supabase
            Log.d(TAG, "Autenticando usuario...")
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // 4. Obtener el usuario autenticado
            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                Log.e(TAG, "❌ No se pudo obtener el usuario después del login")
                return@withContext LoginResult.Error("Error de autenticación. Intenta nuevamente.")
            }

            val userId = currentUser.id
            Log.d(TAG, "✅ Usuario autenticado: $userId")

            // 5. VALIDACIÓN CRÍTICA: Verificar que el perfil existe con reintentos
            var profile: UserProfile? = null
            var intentos = 0
            val maxIntentos = 3

            while (profile == null && intentos < maxIntentos) {
                try {
                    Log.d(TAG, "Verificando perfil (intento ${intentos + 1}/$maxIntentos)...")

                    profile = supabase.from("profiles")
                        .select {
                            filter { eq("id", userId) }
                        }
                        .decodeSingleOrNull<UserProfile>()

                    if (profile != null) {
                        Log.d(TAG, "✅ Perfil encontrado: ${profile.nombre}")
                        break
                    }

                    intentos++
                    if (intentos < maxIntentos) {
                        Log.w(TAG, "⚠️ Perfil no encontrado, esperando...")
                        kotlinx.coroutines.delay(1000) // Esperar 1 segundo antes de reintentar
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verificando perfil (intento ${intentos + 1}): ${e.message}")
                    intentos++
                    if (intentos < maxIntentos) {
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }

            // 6. Si no existe el perfil después de los reintentos, es un ERROR CRÍTICO
            if (profile == null) {
                Log.e(TAG, "❌ ERROR CRÍTICO: Perfil no encontrado para usuario autenticado: $userId")
                Log.e(TAG, "Email del usuario: $email")

                // Cerrar sesión para evitar estados inconsistentes
                try {
                    supabase.auth.signOut()
                    Log.d(TAG, "Sesión cerrada por falta de perfil")
                } catch (e: Exception) {
                    Log.e(TAG, "Error cerrando sesión: ${e.message}")
                }

                return@withContext LoginResult.Error(
                    "Error de configuración de cuenta. Tu perfil no fue creado correctamente. " +
                            "Por favor contacta al administrador con este email: $email"
                )
            }

            // 7. Verificar el estado del perfil (doble verificación)
            if (!profile.activo) {
                Log.w(TAG, "❌ Usuario deshabilitado después de autenticar: $userId")
                try {
                    supabase.auth.signOut()
                } catch (e: Exception) {
                    Log.e(TAG, "Error cerrando sesión: ${e.message}")
                }
                return@withContext LoginResult.Error(
                    "Tu cuenta ha sido bloqueada. Contacta al administrador para más información."
                )
            }

            // 8. Obtener el rol del usuario con reintentos
            var userRole: Role? = null
            intentos = 0

            while (userRole == null && intentos < maxIntentos) {
                try {
                    Log.d(TAG, "Obteniendo rol (intento ${intentos + 1}/$maxIntentos)...")
                    userRole = getUserRole(userId)

                    if (userRole != null) {
                        Log.d(TAG, "✅ Rol obtenido: ${userRole.value}")
                        break
                    }

                    intentos++
                    if (intentos < maxIntentos) {
                        Log.w(TAG, "⚠️ Rol no encontrado, esperando...")
                        kotlinx.coroutines.delay(1000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error obteniendo rol (intento ${intentos + 1}): ${e.message}")
                    intentos++
                    if (intentos < maxIntentos) {
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }

            // 9. Si no hay rol, asignar 'client' por defecto
            if (userRole == null) {
                Log.w(TAG, "⚠️ No se encontró rol, asignando 'client' por defecto")
                userRole = Role.CLIENT

                // Intentar asignar el rol en la base de datos
                try {
                    val clientRoleId = supabase.from("roles")
                        .select {
                            filter { eq("name", "client") }
                        }
                        .decodeSingle<RoleRecord>()
                        .id

                    supabase.from("user_roles")
                        .insert(UserRole(user_id = userId, role_id = clientRoleId))

                    Log.d(TAG, "✅ Rol 'client' asignado exitosamente")
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ No se pudo asignar rol 'client': ${e.message}")
                    // Continuar con el rol CLIENT de todas formas
                }
            }

            // 10. Guardar sesión
            saveUserSession(
                userId = userId,
                email = profile.email,
                userName = profile.nombre,
                role = userRole
            )

            Log.d(TAG, "✅ LOGIN EXITOSO - Usuario: ${profile.nombre}, Rol: ${userRole.value}")
            return@withContext LoginResult.Success(userRole)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en login: ${e.message}", e)

            val errorMessage = when {
                e.message?.contains("Invalid login", ignoreCase = true) == true ||
                        e.message?.contains("invalid credentials", ignoreCase = true) == true ||
                        e.message?.contains("Invalid email or password", ignoreCase = true) == true ->
                    "Correo electrónico o contraseña incorrectos"

                e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                    "Por favor verifica tu correo electrónico para activar tu cuenta"

                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("unable to resolve host", ignoreCase = true) == true ||
                        e.message?.contains("failed to connect", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                e.message?.contains("too many requests", ignoreCase = true) == true ||
                        e.message?.contains("429", ignoreCase = true) == true ->
                    "Demasiados intentos. Espera unos minutos"

                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "La operación tardó demasiado. Intenta nuevamente"

                else -> {
                    Log.e(TAG, "Error no manejado en login: ${e.javaClass.simpleName}")
                    "No se pudo iniciar sesión. Verifica tus credenciales"
                }
            }

            return@withContext LoginResult.Error(errorMessage)
        }
    }

    // ==========================================
    // OBTENER ROL DEL USUARIO CON MEJOR MANEJO
    // ==========================================
    private suspend fun getUserRole(userId: String): Role? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo rol para usuario: $userId")

            val userRoles = supabase.from("user_roles")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<UserRole>()

            if (userRoles.isEmpty()) {
                Log.w(TAG, "⚠️ No se encontraron roles para el usuario: $userId")
                return@withContext null
            }

            val roleId = userRoles.first().role_id
            Log.d(TAG, "Role ID encontrado: $roleId")

            val roleRecord = supabase.from("roles")
                .select {
                    filter { eq("id", roleId) }
                }
                .decodeSingle<RoleRecord>()

            val role = Role.fromString(roleRecord.name)
            Log.d(TAG, "Rol mapeado: ${role?.value ?: "null"}")

            return@withContext role

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener rol: ${e.message}", e)
            return@withContext null
        }
    }

    // ==========================================
    // LOGOUT
    // ==========================================
    suspend fun logout(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cerrando sesión...")

            try {
                supabase.auth.signOut()
                Log.d(TAG, "Sesión cerrada en Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión en Supabase: ${e.message}")
            }

            // Limpiar sesión local
            sharedPrefsHelper.clearUserSession()

            Log.d(TAG, "Sesión cerrada exitosamente")
            return@withContext Result.Success

        } catch (t: Throwable) {
            Log.e(TAG, "Error al cerrar sesión: ${t.message}", t)
            return@withContext Result.Error("Error al cerrar sesión")
        }
    }

    // ==========================================
    // OBTENER USUARIO ACTUAL
    // ==========================================
    suspend fun getCurrentUser(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: return@withContext null

            val profiles = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeList<UserProfile>()

            return@withContext profiles.firstOrNull()

        } catch (t: Throwable) {
            Log.e(TAG, "Error al obtener usuario actual: ${t.message}", t)
            return@withContext null
        }
    }

    // ==========================================
    // OBTENER ROL ACTUAL
    // ==========================================
    suspend fun getCurrentUserRole(): Role? = withContext(Dispatchers.IO) {
        try {
            // Primero intentar obtener de la sesión guardada
            val cachedRole = sharedPrefsHelper.getUserRole()
            if (cachedRole != null) {
                Log.d(TAG, "Rol obtenido de caché: ${cachedRole.value}")
                return@withContext cachedRole
            }

            // Si no hay en caché, obtener de Supabase
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: return@withContext null

            return@withContext getUserRole(userId)

        } catch (t: Throwable) {
            Log.e(TAG, "Error al obtener rol del usuario: ${t.message}", t)
            return@withContext null
        }
    }

    // ==========================================
    // REFRESCAR ROL
    // ==========================================
    suspend fun refreshUserRole(): Role? = withContext(Dispatchers.IO) {
        try {
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: return@withContext null
            val role = getUserRole(userId)
            if (role != null) {
                sharedPrefsHelper.updateUserRole(role)
            }
            return@withContext role
        } catch (e: Exception) {
            Log.e(TAG, "Error refrescando rol: ${e.message}", e)
            return@withContext null
        }
    }

    // ==========================================
    // VERIFICAR SESIÓN ACTIVA
    // ==========================================
    suspend fun hasActiveSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Verificar si Supabase tiene una sesión activa
            val supabaseSession = supabase.auth.currentSessionOrNull()
            if (supabaseSession != null) {
                Log.d(TAG, "Sesión de Supabase encontrada")

                // Sincronizar con SharedPreferences si no está guardada
                if (!sharedPrefsHelper.isUserLoggedIn()) {
                    val currentUser = supabase.auth.currentUserOrNull()
                    if (currentUser != null) {
                        val userId = currentUser.id
                        val email = currentUser.email ?: ""
                        val userName = currentUser.userMetadata?.get("nombre")?.toString()
                            ?: email.split("@").first()
                        val role = getUserRole(userId) ?: Role.CLIENT
                        saveUserSession(userId, email, userName, role)
                        Log.d(TAG, "Sesión sincronizada con SharedPreferences")
                    }
                }
                return@withContext true
            }

            // Si no hay sesión en Supabase, limpiar SharedPreferences
            val hasStoredSession = sharedPrefsHelper.isUserLoggedIn()
            if (hasStoredSession) {
                Log.d(TAG, "Sesión encontrada en SharedPreferences pero no en Supabase - Limpiando...")
                sharedPrefsHelper.clearUserSession()
            }

            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando sesión activa: ${e.message}")
            return@withContext false
        }
    }

    // ==========================================
    // VERIFICAR SESIÓN SÍNCRONA
    // ==========================================
    fun hasActiveSessionSync(): Boolean {
        return sharedPrefsHelper.isUserLoggedIn()
    }

    // ==========================================
    // OBTENER SESIÓN ACTUAL
    // ==========================================
    fun getCurrentSession(): SharedPreferenHelper.UserSession {
        return sharedPrefsHelper.getUserSession()
    }

    // ==========================================
    // GUARDAR SESIÓN
    // ==========================================
    private fun saveUserSession(userId: String, email: String, userName: String, role: Role) {
        sharedPrefsHelper.saveUserSession(
            email = email,
            role = role,
            userId = userId,
            userName = userName,
            sessionToken = "supabase_session_${System.currentTimeMillis()}"
        )
    }

    // ==========================================
    // VERIFICAR CONFIGURACIÓN DE SUPABASE
    // ==========================================
    private fun isSupabaseConfigured(): Boolean {
        return try {
            val url = com.dev.mandadito.config.AppConfig.SUPABASE_URL
            val key = com.dev.mandadito.config.AppConfig.SUPABASE_ANON_KEY

            !url.contains("placeholder") &&
                    !key.contains("placeholder") &&
                    url.isNotBlank() &&
                    key.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}