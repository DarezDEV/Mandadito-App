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

    suspend fun register(registerData: RegisterData): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando registro para: ${registerData.email}")

            // Verificar que las credenciales de Supabase estén configuradas
            if (!isSupabaseConfigured()) {
                Log.e(TAG, "Supabase no está configurado correctamente")
                return@withContext Result.Error("Error de configuración. Contacta al administrador.")
            }

            // 1. Registrar usuario en Supabase Auth con metadatos para el trigger
            // Crear los metadatos antes de pasarlos al signUpWith
            val userMetadata = buildJsonObject {
                put("nombre", registerData.nombre)
                if (registerData.telefono.isNotBlank()) {
                    put("telefono", registerData.telefono)
                }
            }

            supabase.auth.signUpWith(Email) {
                email = registerData.email
                password = registerData.password
                // Pasar metadatos para que el trigger pueda crear el perfil automáticamente
                data = userMetadata
            }

            // 2. Obtener el usuario actual después del registro
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: throw Exception("No se pudo obtener el ID del usuario")

            Log.d(TAG, "Usuario registrado en Auth: $userId")

            // 3. Verificar si el perfil fue creado por el trigger, si no, crearlo manualmente
            var profileExists = false
            try {
                val existingProfile = supabase.from("profiles")
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeList<UserProfile>()

                profileExists = existingProfile.isNotEmpty()
                if (profileExists) {
                    Log.d(TAG, "Perfil ya existe (creado por trigger)")
                    // Actualizar el perfil si falta información
                    val profile = existingProfile.first()
                    if (profile.telefono.isNullOrBlank()) {
                        supabase.from("profiles").update(
                            mapOf(
                                "telefono" to (registerData.telefono.ifBlank { null })
                            )
                        ) {
                            filter { eq("id", userId) }
                        }
                        Log.d(TAG, "Perfil actualizado con información adicional")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error al verificar perfil existente: ${e.message}")
            }

            // Si el trigger no creó el perfil, crearlo manualmente
            if (!profileExists) {
                try {
                    supabase.from("profiles").insert(
                        mapOf(
                            "id" to userId,
                            "email" to registerData.email,
                            "nombre" to registerData.nombre,
                            "telefono" to registerData.telefono.ifBlank { null },
                            "activo" to true
                        )
                    )
                    Log.d(TAG, "Perfil creado manualmente exitosamente")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al crear perfil manualmente: ${e.message}", e)
                    // Re-lanzar el error para que el usuario sepa que algo falló
                    throw Exception("No se pudo crear el perfil: ${e.message}")
                }
            }

            // 4. Asignar rol de cliente por defecto usando roles.id
            try {
                val roles = supabase.from("roles")
                    .select {
                        filter { eq("name", "client") }
                    }
                    .decodeList<RoleRecord>()

                val clientRoleId = roles.firstOrNull()?.id
                if (clientRoleId != null) {
                    supabase.from("user_roles").insert(
                        mapOf(
                            "user_id" to userId,
                            "role_id" to clientRoleId
                        )
                    )
                    Log.d(TAG, "Rol de cliente asignado exitosamente (id=$clientRoleId)")
                } else {
                    Log.e(TAG, "No se encontró el rol 'client' en la tabla roles")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al asignar rol: ${e.message}", e)
            }

            // Verificar si necesita confirmación por email
            // En Supabase, si el registro es exitoso, el usuario ya está creado
            // La confirmación por email es opcional según la configuración del proyecto
            Log.d(TAG, "Registro completado exitosamente")

            // Guardar sesión directamente
            saveUserSession(userId, registerData.email, registerData.nombre, Role.CLIENT)

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

            if (!isSupabaseConfigured()) {
                Log.e(TAG, "Supabase no está configurado correctamente")
                return@withContext LoginResult.Error("Error de configuración. Contacta al administrador.")
            }

            // 1. Verificar primero si el usuario está activo
            val isActive = try {
                val profile = supabase.from("profiles")
                    .select {
                        filter { eq("email", email) }
                    }
                    .decodeSingleOrNull<UserProfile>()

                profile?.activo ?: true // Si no existe el perfil, permitir login (se creará)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo verificar estado del usuario: ${e.message}")
                true // Permitir continuar si hay error al verificar
            }

            // 2. Si el usuario está deshabilitado, no permitir login
            if (!isActive) {
                Log.w(TAG, "Intento de login con cuenta deshabilitada: $email")
                return@withContext LoginResult.Error(
                    "Tu cuenta ha sido bloqueada. Por favor, contacta con tu proveedor para más información."
                )
            }

            // 3. Autenticar con Supabase
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // 4. Obtener el usuario actual después del login
            val currentUser = supabase.auth.currentUserOrNull()
            val userId = currentUser?.id ?: throw Exception("No se pudo obtener el ID del usuario")

            Log.d(TAG, "Login exitoso para usuario: $userId")

            // 5. Verificar que el perfil existe y está activo, o crearlo si no existe
            val profile = try {
                supabase.from("profiles")
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<UserProfile>()
            } catch (e: Exception) {
                Log.w(TAG, "Error al obtener perfil: ${e.message}")
                null
            }

            // Si el perfil no existe, crearlo automáticamente
            if (profile == null) {
                try {
                    Log.d(TAG, "Perfil no encontrado, creándolo automáticamente...")
                    val userEmail = currentUser.email ?: email
                    val userName = currentUser.userMetadata?.get("nombre")?.toString() 
                        ?: userEmail.split("@").first()
                    
                    supabase.from("profiles").insert(
                        mapOf(
                            "id" to userId,
                            "email" to userEmail,
                            "nombre" to userName,
                            "telefono" to (currentUser.userMetadata?.get("telefono")?.toString()),
                            "activo" to true
                        )
                    )
                    Log.d(TAG, "Perfil creado automáticamente")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al crear perfil automáticamente: ${e.message}", e)
                    // Continuar con el login aunque falle crear el perfil
                }
            } else if (!profile.activo) {
                // Si el perfil existe pero está deshabilitado, bloquear login
                supabase.auth.signOut()
                return@withContext LoginResult.Error(
                    "Tu cuenta ha sido bloqueada. Por favor, contacta con tu proveedor para más información."
                )
            }

            // 6. Obtener el rol del usuario
            val userRole = getUserRole(userId) ?: Role.CLIENT
            Log.d(TAG, "Rol obtenido: ${userRole.value}")

            // 7. Obtener nombre del usuario
            val userName = try {
                val profiles = supabase.from("profiles")
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeList<UserProfile>()

                profiles.firstOrNull()?.nombre ?: email.split("@").first()
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener nombre: ${e.message}")
                email.split("@").first()
            }

            // 8. Guardar sesión
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

            val roleId = userRoles.firstOrNull()?.role_id
            Log.d(TAG, "Rol id encontrado: $roleId")

            if (roleId == null) return@withContext null

            val roleRecord = supabase.from("roles")
                .select {
                    filter { eq("id", roleId) }
                }
                .decodeList<RoleRecord>()
                .firstOrNull()

            val role = roleRecord?.name?.let { Role.fromString(it) }
            return@withContext role

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
     * Refresca el rol del usuario desde el servidor y actualiza la caché
     */
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

    /**
     * Verifica si hay una sesión activa guardada
     * Primero verifica la sesión de Supabase, luego SharedPreferences
     */
    suspend fun hasActiveSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Primero verificar si Supabase tiene una sesión activa
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
            
            // Si no hay sesión en Supabase, verificar SharedPreferences
            val hasStoredSession = sharedPrefsHelper.isUserLoggedIn()
            if (hasStoredSession) {
                Log.d(TAG, "Sesión encontrada en SharedPreferences pero no en Supabase")
                // Limpiar SharedPreferences si Supabase no tiene sesión
                sharedPrefsHelper.clearUserSession()
            }
            
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando sesión activa: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Versión síncrona para compatibilidad (usa la sesión guardada)
     */
    fun hasActiveSessionSync(): Boolean {
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