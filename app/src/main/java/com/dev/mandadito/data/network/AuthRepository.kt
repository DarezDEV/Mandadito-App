package com.dev.mandadito.data.network

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.RegisterData
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.models.UserRole
import com.dev.mandadito.utils.SharedPreferenHelper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(private val context: Context) {

    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val supabase = SupabaseClient.client

    private companion object {
        const val TAG = "AuthRepository"
    }

    sealed class Result {
        object Success : Result()
        data class NeedsConfirm(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    sealed class LoginResult {
        data class Success(val role: Role) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    suspend fun register(data: RegisterData): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando registro para: ${data.email}")

            // Verificar que las credenciales de Supabase estén configuradas
            if (!isSupabaseConfigured()) {
                Log.e(TAG, "Supabase no está configurado correctamente")
                return@withContext Result.Error("Error de configuración. Contacta al administrador.")
            }

            // 1. Registrar usuario en Supabase Auth
            supabase.auth.signUpWith(Email) {
                email = data.email
                password = data.password
            }

            // 2. Obtener el usuario actual después del registro
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: throw Exception("No se pudo obtener el ID del usuario")

            Log.d(TAG, "Usuario registrado en Auth: $userId")

            // 3. Crear perfil en la tabla profiles

            try {
                supabase.from("profiles").insert(
                    mapOf(
                        "id" to userId,
                        "email" to data.email,
                        "nombre" to data.nombre,
                        "telefono" to data.telefono.ifBlank { null },
                        "direccion" to data.direccion.ifBlank { null },
                        "activo" to true
                    )
                )
                Log.d(TAG, "Perfil creado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear perfil: ${e.message}", e)
                // Continuar aunque falle el perfil, se puede crear después
            }

            // 4. Asignar rol de cliente por defecto
            try {
                supabase.from("user_roles").insert(
                    mapOf(
                        "user_id" to userId,
                        "role_name" to "client"
                    )
                )
                Log.d(TAG, "Rol de cliente asignado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al asignar rol: ${e.message}", e)
            }

            // Verificar si necesita confirmación por email
            // En Supabase, si el registro es exitoso, el usuario ya está creado
            // La confirmación por email es opcional según la configuración del proyecto
            Log.d(TAG, "Registro completado exitosamente")

            // Guardar sesión directamente
            saveUserSession(userId, data.email, data.nombre, Role.CLIENT)

            return@withContext Result.Success

        } catch (e: Exception) {
            Log.e(TAG, "Error en registro: ${e.message}", e)

            val errorMessage = when {
                e.message?.contains("already registered", ignoreCase = true) == true ||
                        e.message?.contains("already exists", ignoreCase = true) == true ->
                    "Este correo electrónico ya está registrado"

                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("unable to resolve host", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                e.message?.contains("invalid email", ignoreCase = true) == true ->
                    "Correo electrónico inválido"

                e.message?.contains("weak password", ignoreCase = true) == true ||
                        e.message?.contains("password", ignoreCase = true) == true ->
                    "La contraseña es muy débil. Usa al menos 8 caracteres"

                else -> "No se pudo crear la cuenta. Intenta nuevamente"
            }

            return@withContext Result.Error(errorMessage)
        }
    }

    suspend fun login(email: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando login para: $email")

            // Verificar configuración de Supabase
            if (!isSupabaseConfigured()) {
                Log.e(TAG, "Supabase no está configurado correctamente")
                return@withContext LoginResult.Error("Error de configuración. Contacta al administrador.")
            }

            // 1. Autenticar con Supabase
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // 2. Obtener el usuario actual después del login
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: throw Exception("No se pudo obtener el ID del usuario")

            Log.d(TAG, "Login exitoso para usuario: $userId")

            // 3. Obtener el rol del usuario
            val userRole = getUserRole(userId) ?: Role.CLIENT
            Log.d(TAG, "Rol obtenido: ${userRole.value}")

            // 4. Obtener nombre del usuario
            val userName = try {
                val profiles = supabase.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<UserProfile>()

                profiles.firstOrNull()?.nombre ?: email.split("@").first()
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener nombre: ${e.message}")
                email.split("@").first()
            }

            // 5. Guardar sesión
            saveUserSession(userId, email, userName, userRole)

            Log.d(TAG, "Sesión guardada correctamente")
            return@withContext LoginResult.Success(userRole)

        } catch (e: Exception) {
            Log.e(TAG, "Error en login: ${e.message}", e)

            val errorMessage = when {
                e.message?.contains("Invalid login", ignoreCase = true) == true ||
                        e.message?.contains("invalid credentials", ignoreCase = true) == true ->
                    "Correo electrónico o contraseña incorrectos"

                e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                    "Por favor verifica tu correo electrónico para activar tu cuenta"

                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("unable to resolve host", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                e.message?.contains("too many requests", ignoreCase = true) == true ||
                        e.message?.contains("429", ignoreCase = true) == true ->
                    "Demasiados intentos. Espera unos minutos"

                else -> "No se pudo iniciar sesión. Verifica tus credenciales"
            }

            return@withContext LoginResult.Error(errorMessage)
        }
    }

    private suspend fun getUserRole(userId: String): Role? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo rol para usuario: $userId")

            val userRoles = supabase.from("user_roles")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<UserRole>()

            val roleName = userRoles.firstOrNull()?.role_name
            Log.d(TAG, "Rol encontrado: $roleName")

            return@withContext roleName?.let { Role.fromString(it) }

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener rol: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun logout(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cerrando sesión...")

            // Cerrar sesión en Supabase
            try {
                supabase.auth.signOut()
                Log.d(TAG, "Sesión cerrada en Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión en Supabase: ${e.message}")
                // Continuar para limpiar la sesión local
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

    suspend fun getCurrentUser(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: return@withContext null

            val profiles = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<UserProfile>()

            return@withContext profiles.firstOrNull()

        } catch (t: Throwable) {
            Log.e(TAG, "Error al obtener usuario actual: ${t.message}", t)
            return@withContext null
        }
    }

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

    /**
     * Verifica si hay una sesión activa guardada
     */
    fun hasActiveSession(): Boolean {
        return sharedPrefsHelper.isUserLoggedIn()
    }

    /**
     * Obtiene los datos de la sesión actual
     */
    fun getCurrentSession(): SharedPreferenHelper.UserSession {
        return sharedPrefsHelper.getUserSession()
    }

    /**
     * Guarda la sesión del usuario
     */
    private fun saveUserSession(userId: String, email: String, userName: String, role: Role) {
        sharedPrefsHelper.saveUserSession(
            email = email,
            role = role,
            userId = userId,
            userName = userName,
            sessionToken = "supabase_session_${System.currentTimeMillis()}"
        )
    }

    /**
     * Verifica si Supabase está configurado correctamente
     */
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