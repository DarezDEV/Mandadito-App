package com.dev.mandadito.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.models.RegisterData
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.RoleRecord
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.models.UserRole
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.utils.SharedPreferenHelper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


class AuthRepository(private val context: Context) {

    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val supabase = SupabaseClient.client
    private val TAG = "AuthRepository"

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }


    // ==========================================
    // RESULT CLASSES
    // ==========================================
    sealed class AuthResult {
        object Success : AuthResult()
        data class NeedsConfirm(val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    sealed class LoginResult {
        data class Success(val role: Role) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    // Resultado genérico para operaciones que retornan datos
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    // ==========================================
    // CREAR USUARIO COMO ADMIN (CON AVATAR)
    // ==========================================
    suspend fun createUserAsAdmin(
        email: String,
        password: String,
        nombre: String,
        telefono: String?,
        role: Role,
        avatarUri: Uri?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔷 Creando usuario como admin: $email con rol: ${role.value}")

            // 1. Convertir avatar a base64 si existe
            var avatarBase64: String? = null
            if (avatarUri != null) {
                try {
                    Log.d(TAG, "📸 Convirtiendo avatar a base64...")
                    val inputStream = context.contentResolver.openInputStream(avatarUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        avatarBase64 = "data:image/jpeg;base64," +
                                Base64.encodeToString(bytes, Base64.NO_WRAP)

                        Log.d(
                            TAG,
                            "✅ Avatar convertido a base64, tamaño: ${avatarBase64.length} caracteres"
                        )
                    } else {
                        Log.w(TAG, "⚠️ No se pudieron leer los bytes del avatar")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error convirtiendo avatar a base64: ${e.message}", e)
                    // Continuar sin avatar en caso de error
                }
            } else {
                Log.d(TAG, "ℹ️ No se proporcionó avatar")
            }

            // 2. Preparar el body del request
            val requestBody = buildJsonObject {
                put("email", email)
                put("password", password)
                put("nombre", nombre)
                put("role", role.name.lowercase())
                telefono?.let { put("telefono", it) }
                avatarBase64?.let {
                    put("avatar_base64", it)
                    Log.d(TAG, "✅ avatar_base64 incluido en el request")
                }
            }

            Log.d(TAG, "📤 Enviando request al Edge Function...")

            // 3. Obtener token de autenticación
            val session = supabase.auth.currentSessionOrNull()
            val token = session?.accessToken
            if (token == null) {
                Log.e(TAG, "❌ No hay token de autenticación")
                return@withContext Result.Error("No autorizado")
            }

            // 4. Llamar al Edge Function usando HttpClient
            val response = httpClient.post("${AppConfig.SUPABASE_URL}/functions/v1/create-user") {
                headers {
                    append("Authorization", "Bearer $token")
                    append("apikey", AppConfig.SUPABASE_ANON_KEY)
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            Log.d(TAG, "📥 Respuesta recibida del Edge Function")

            // 5. Parsear respuesta
            val responseData = response.body<CreateUserResponse>()

            if (responseData.success && responseData.user != null) {
                Log.d(TAG, "✅ Usuario creado exitosamente: ${responseData.user.email}")
                if (responseData.user.avatar_url != null) {
                    Log.d(TAG, "🖼️ Avatar URL: ${responseData.user.avatar_url}")
                } else {
                    Log.d(TAG, "ℹ️ Usuario creado sin avatar")
                }

                return@withContext Result.Success(responseData.user)
            } else {
                Log.e(TAG, "❌ Error en la respuesta: ${responseData.error}")
                return@withContext Result.Error(responseData.error ?: "Error desconocido")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando usuario: ${e.message}", e)

            val errorMessage = when {
                e.message?.contains("already registered", ignoreCase = true) == true ||
                        e.message?.contains("duplicate", ignoreCase = true) == true ->
                    "Este correo electrónico ya está registrado"

                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("unable to resolve host", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                e.message?.contains("unauthorized", ignoreCase = true) == true ||
                        e.message?.contains("403", ignoreCase = true) == true ->
                    "No tienes permisos para crear usuarios"

                else -> "Error al crear usuario: ${e.message ?: "Intenta nuevamente"}"
            }

            return@withContext Result.Error(errorMessage)
        }
    }

    // Clase de datos para la respuesta del Edge Function
    @Serializable
    private data class CreateUserResponse(
        val success: Boolean,
        val user: UserProfile? = null,
        val error: String? = null,
        val message: String? = null
    )

    // ==========================================
    // REGISTRO CON VALIDACIÓN MEJORADA
    // ==========================================
    suspend fun register(registerData: RegisterData): AuthResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando registro para: ${registerData.email}")

            if (!isSupabaseConfigured()) {
                Log.e(TAG, "Supabase no está configurado correctamente")
                return@withContext AuthResult.Error("Error de configuración. Contacta al administrador.")
            }

            Log.d(TAG, "Registrando usuario con metadatos...")

            // Registrar usuario en Supabase Auth
            val authResponse = supabase.auth.signUpWith(Email) {
                email = registerData.email
                password = registerData.password
                data = buildJsonObject {
                    put("nombre", registerData.nombre)
                    put("role", "client")
                }
            }

            val userId = authResponse?.id
            Log.d(TAG, "Usuario registrado exitosamente con ID: $userId")

            // Verificar que el trigger creó el perfil
            if (userId != null) {
                delay(1000)

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
                            delay(1000)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error verificando perfil (intento $intentos): ${e.message}")
                        intentos++
                        if (intentos < maxIntentos) {
                            delay(1000)
                        }
                    }
                }

                if (profile == null) {
                    Log.e(TAG, "❌ ERROR CRÍTICO: Perfil no creado después de $maxIntentos intentos")
                    try {
                        supabase.auth.signOut()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cerrando sesión: ${e.message}")
                    }
                    return@withContext AuthResult.Error(
                        "Error al crear el perfil. Por favor intenta nuevamente o contacta al administrador."
                    )
                }

                Log.d(
                    TAG,
                    "✅ Usuario registrado. La UI mostrará la imagen por defecto si no hay avatar_url"
                )
            }

            // Cerrar la sesión automática después del registro
            try {
                supabase.auth.signOut()
                Log.d(TAG, "Sesión cerrada después del registro")
            } catch (e: Exception) {
                Log.w(TAG, "Error al cerrar sesión: ${e.message}")
            }

            Log.d(TAG, "Registro completado exitosamente")
            return@withContext AuthResult.Success

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

            return@withContext AuthResult.Error(errorMessage)
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

            // 1. Verificar primero si el usuario existe y está activo
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
                        delay(1000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verificando perfil (intento ${intentos + 1}): ${e.message}")
                    intentos++
                    if (intentos < maxIntentos) {
                        delay(1000)
                    }
                }
            }

            // 6. Si no existe el perfil después de los reintentos, es un ERROR CRÍTICO
            if (profile == null) {
                Log.e(
                    TAG,
                    "❌ ERROR CRÍTICO: Perfil no encontrado para usuario autenticado: $userId"
                )
                Log.e(TAG, "Email del usuario: $email")

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

            // 7. Verificar el estado del perfil
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
                        delay(1000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error obteniendo rol (intento ${intentos + 1}): ${e.message}")
                    intentos++
                    if (intentos < maxIntentos) {
                        delay(1000)
                    }
                }
            }

            // 9. Si no hay rol, asignar 'client' por defecto
            if (userRole == null) {
                Log.w(TAG, "⚠️ No se encontró rol, asignando 'client' por defecto")
                userRole = Role.CLIENT

                try {
                    // Intentar obtener el rol 'client' de la tabla roles
                    val clientRole = supabase.from("roles")
                        .select {
                            filter { eq("name", "client") }
                        }
                        .decodeSingleOrNull<RoleRecord>()

                    if (clientRole == null) {
                        Log.e(
                            TAG,
                            "❌ El rol 'client' no existe en la tabla roles. Verifica la base de datos."
                        )
                        Log.e(
                            TAG,
                            "⚠️ Por favor ejecuta el script SQL fix_roles_issue.sql para corregir este problema."
                        )
                        // No intentamos crear el rol desde la app, debe hacerse desde la base de datos
                    } else {
                        // El rol existe, asignarlo al usuario
                        try {
                            supabase.from("user_roles")
                                .insert(UserRole(user_id = userId, role_id = clientRole.id))
                            Log.d(TAG, "✅ Rol 'client' asignado exitosamente")
                        } catch (insertError: Exception) {
                            Log.e(
                                TAG,
                                "❌ No se pudo insertar en user_roles: ${insertError.message}",
                                insertError
                            )
                            // Verificar si el error es porque el rol ya existe
                            if (insertError.message?.contains("duplicate") == true ||
                                insertError.message?.contains("unique") == true
                            ) {
                                Log.d(TAG, "ℹ️ El rol ya estaba asignado al usuario")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ No se pudo asignar rol 'client': ${e.message}", e)
                }
            }

            // 10. ✨ NUEVO: Si es seller, obtener su colmado_id
            var colmadoId: String? = null
            if (userRole == Role.SELLER) {
                try {
                    Log.d(TAG, "📦 Obteniendo colmado del seller...")
                    val sellerRepo = SellerRepository(context)
                    when (val colmadoResult = sellerRepo.getSellerColmadoId(userId)) {
                        is SellerRepository.Result.Success -> {
                            colmadoId = colmadoResult.data
                            Log.d(TAG, "✅ Colmado obtenido: $colmadoId")
                        }

                        is SellerRepository.Result.Error -> {
                            Log.w(TAG, "⚠️ No se pudo obtener colmado: ${colmadoResult.message}")
                            // Opcional: podrías retornar un error aquí si el seller DEBE tener un colmado
                            // return@withContext LoginResult.Error(colmadoResult.message)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error obteniendo colmado del seller: ${e.message}")
                    // Continuar sin colmado_id (no crítico para el login)
                }
            }

            // 11. Guardar sesión (con colmado_id si es seller)
            saveUserSession(
                userId = userId,
                email = profile.email,
                userName = profile.nombre,
                role = userRole,
                colmadoId = colmadoId
            )

            Log.d(
                TAG,
                "✅ LOGIN EXITOSO - Usuario: ${profile.nombre}, Rol: ${userRole.value}, Colmado: ${colmadoId ?: "N/A"}"
            )
            return@withContext LoginResult.Success(userRole)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en login: ${e.message}", e)

            val errorMessage = when {
                e.message?.contains("Invalid login", ignoreCase = true) == true ||
                        e.message?.contains("invalid credentials", ignoreCase = true) == true ||
                        e.message?.contains(
                            "Invalid email or password",
                            ignoreCase = true
                        ) == true ->
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

            val role = Role.Companion.fromString(roleRecord.name)
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
    suspend fun logout(): AuthResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cerrando sesión...")

            try {
                supabase.auth.signOut()
                Log.d(TAG, "Sesión cerrada en Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión en Supabase: ${e.message}")
            }

            sharedPrefsHelper.clearUserSession()

            Log.d(TAG, "Sesión cerrada exitosamente")
            return@withContext AuthResult.Success

        } catch (t: Throwable) {
            Log.e(TAG, "Error al cerrar sesión: ${t.message}", t)
            return@withContext AuthResult.Error("Error al cerrar sesión")
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
            val cachedRole = sharedPrefsHelper.getUserRole()
            if (cachedRole != null) {
                Log.d(TAG, "Rol obtenido de caché: ${cachedRole.value}")
                return@withContext cachedRole
            }

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
            val supabaseSession = supabase.auth.currentSessionOrNull()
            if (supabaseSession != null) {
                Log.d(TAG, "Sesión de Supabase encontrada")

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

            val hasStoredSession = sharedPrefsHelper.isUserLoggedIn()
            if (hasStoredSession) {
                Log.d(
                    TAG,
                    "Sesión encontrada en SharedPreferences pero no en Supabase - Limpiando..."
                )
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
    private fun saveUserSession(
        userId: String,
        email: String,
        userName: String,
        role: Role,
        colmadoId: String? = null
    ) {
        sharedPrefsHelper.saveUserSession(
            email = email,
            role = role,
            userId = userId,
            userName = userName,
            sessionToken = "supabase_session_${System.currentTimeMillis()}",
            colmadoId = colmadoId
        )
    }

    // ==========================================
    // VERIFICAR CONFIGURACIÓN DE SUPABASE
    // ==========================================
    private fun isSupabaseConfigured(): Boolean {
        return try {
            val url = AppConfig.SUPABASE_URL
            val key = AppConfig.SUPABASE_ANON_KEY

            !url.contains("placeholder") &&
                    !key.contains("placeholder") &&
                    url.isNotBlank() &&
                    key.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}