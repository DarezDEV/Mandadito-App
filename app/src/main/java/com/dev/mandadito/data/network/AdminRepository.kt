package com.dev.mandadito.data.network

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.RoleRecord
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.models.UserRole
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.dev.mandadito.config.AppConfig
import org.slf4j.MDC.put

@Serializable
data class UserWithRole(
    val id: String,
    val email: String,
    val nombre: String,
    val telefono: String?,
    val direccion: String?,
    val activo: Boolean,
    val created_at: String,
    val role_name: String?,
    val role_id: Int?
)

class AdminRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val supabaseAdmin = SupabaseClient.adminClient
    private val TAG = "AdminRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Obtiene todos los usuarios con sus roles
     */
    suspend fun getAllUsers(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo todos los usuarios...")

            // Obtener perfiles
            val profiles = supabase.from("profiles")
                .select()
                .decodeList<UserProfile>()

            // Obtener roles de cada usuario
            val usersWithRoles = profiles.map { profile ->
                val role = getUserRole(profile.id)
                profile.copy(role = role)
            }

            Log.d(TAG, "Usuarios obtenidos: ${usersWithRoles.size}")
            Result.Success(usersWithRoles)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuarios: ${e.message}", e)
            Result.Error("Error al cargar usuarios: ${e.message}")
        }
    }

    /**
     * Obtiene el rol de un usuario específico
     */
    private suspend fun getUserRole(userId: String): Role? {
        return try {
            val userRoles = supabase.from("user_roles")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<UserRole>()

            val roleId = userRoles.firstOrNull()?.role_id ?: return null

            val roleRecord = supabase.from("roles")
                .select {
                    filter { eq("id", roleId) }
                }
                .decodeList<RoleRecord>()
                .firstOrNull()

            roleRecord?.name?.let { Role.fromString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener rol de usuario $userId: ${e.message}")
            null
        }
    }

    /**
     * Crea un nuevo usuario en auth.users y en profiles
     * IMPORTANTE: Requiere la SERVICE_ROLE_KEY para usar admin API
     */
    suspend fun createUser(
        email: String,
        password: String,
        nombre: String,
        telefono: String?,
        role: Role
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creando usuario: $email con rol: ${role.value}")

            // Validar que no sea delivery
            if (role == Role.DELIVERY) {
                return@withContext Result.Error("No se pueden crear usuarios con rol delivery")
            }

            // 1. Crear usuario en auth.users usando Admin API
            // NOTA: Esto requiere usar la SERVICE_ROLE_KEY en lugar de ANON_KEY
            val adminClient = supabaseAdmin
            if (adminClient == null) {
                return@withContext Result.Error("Cliente admin no disponible. Verifica la configuración de SERVICE_ROLE_KEY")
            }

            val userMetadata = buildJsonObject {
                put("nombre", nombre)
                if (!telefono.isNullOrBlank()) put("telefono", telefono)
            }

            // Crear usuario usando llamada HTTP directa a la API Admin de Supabase
            // Esto es necesario porque la API admin de Supabase Kotlin no está disponible
            val userId = try {
                HttpClient(Android).use { httpClient ->
                    // Construir el cuerpo de la petición
                    val requestBodyJson = buildJsonObject {
                        put("email", email)
                        put("password", password)
                        put("email_confirm", true)
                        put("user_metadata", userMetadata)
                    }
                    
                    val requestBodyString = Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), requestBodyJson)

                    val response = httpClient.post("${AppConfig.SUPABASE_URL}/auth/v1/admin/users") {
                        headers {
                            put("apikey", AppConfig.SUPABASE_SERVICE_ROLE_KEY)
                            put("Authorization", "Bearer ${AppConfig.SUPABASE_SERVICE_ROLE_KEY}")
                        }
                        contentType(ContentType.Application.Json)
                        setBody(requestBodyString)
                    }

                    val responseBody = response.body<Map<String, Any>>()
                    @Suppress("UNCHECKED_CAST")
                    val userData = responseBody["user"] as? Map<String, Any>
                    userData?.get("id") as? String
                        ?: throw Exception("No se pudo obtener el ID del usuario creado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear usuario en auth: ${e.message}", e)
                return@withContext Result.Error("Error al crear usuario: ${e.message}")
            }
            Log.d(TAG, "Usuario creado en auth: $userId")

            // 2. Verificar si el trigger creó el perfil
            var profileCreated = false
            var retries = 3
            while (!profileCreated && retries > 0) {
                kotlinx.coroutines.delay(500) // Esperar un poco
                try {
                    val existingProfile = supabase.from("profiles")
                        .select {
                            filter { eq("id", userId) }
                        }
                        .decodeList<UserProfile>()

                    profileCreated = existingProfile.isNotEmpty()
                    if (profileCreated) {
                        Log.d(TAG, "Perfil creado por trigger")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Intento $retries - Error verificando perfil: ${e.message}")
                }
                retries--
            }

            // 3. Si el trigger no creó el perfil, crearlo manualmente
            if (!profileCreated) {
                try {
                    supabase.from("profiles").insert(
                        mapOf(
                            "id" to userId,
                            "email" to email,
                            "nombre" to nombre,
                            "telefono" to telefono,
                            "activo" to true
                        )
                    )
                    Log.d(TAG, "Perfil creado manualmente")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al crear perfil: ${e.message}", e)
                    return@withContext Result.Error("Error al crear perfil: ${e.message}")
                }
            }

            // 4. Asignar rol
            try {
                val roles = supabase.from("roles")
                    .select {
                        filter { eq("name", role.value) }
                    }
                    .decodeList<RoleRecord>()

                val roleId = roles.firstOrNull()?.id
                    ?: return@withContext Result.Error("Rol no encontrado")

                supabase.from("user_roles").insert(
                    mapOf(
                        "user_id" to userId,
                        "role_id" to roleId
                    )
                )
                Log.d(TAG, "Rol asignado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al asignar rol: ${e.message}", e)
                return@withContext Result.Error("Error al asignar rol: ${e.message}")
            }

            // 5. Retornar el perfil creado
            val userProfile = UserProfile(
                id = userId,
                email = email,
                nombre = nombre,
                role = role,
                telefono = telefono,
                activo = true
            )

            Log.d(TAG, "Usuario creado exitosamente")
            Result.Success(userProfile)

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear usuario: ${e.message}", e)
            Result.Error("Error al crear usuario: ${e.message}")
        }
    }

    /**
     * Actualiza el perfil de un usuario
     */
    suspend fun updateUserProfile(
        userId: String,
        nombre: String,
        telefono: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Actualizando perfil de usuario: $userId")

            supabase.from("profiles").update(
                mapOf(
                    "nombre" to nombre,
                    "telefono" to telefono
                )
            ) {
                filter { eq("id", userId) }
            }

            Log.d(TAG, "Perfil actualizado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar perfil: ${e.message}", e)
            Result.Error("Error al actualizar perfil: ${e.message}")
        }
    }

    /**
     * Deshabilita un usuario
     */
    suspend fun disableUser(userId: String, currentUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deshabilitando usuario: $userId")

                // Verificar que no sea el mismo usuario
                if (userId == currentUserId) {
                    return@withContext Result.Error("No puedes deshabilitar tu propia cuenta")
                }

                supabase.from("profiles").update(
                    mapOf("activo" to false)
                ) {
                    filter { eq("id", userId) }
                }

                Log.d(TAG, "Usuario deshabilitado exitosamente")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Error al deshabilitar usuario: ${e.message}", e)
                Result.Error("Error al deshabilitar usuario: ${e.message}")
            }
        }

    /**
     * Habilita un usuario
     */
    suspend fun enableUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Habilitando usuario: $userId")

            supabase.from("profiles").update(
                mapOf("activo" to true)
            ) {
                filter { eq("id", userId) }
            }

            Log.d(TAG, "Usuario habilitado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al habilitar usuario: ${e.message}", e)
            Result.Error("Error al habilitar usuario: ${e.message}")
        }
    }

    /**
     * Elimina un usuario completamente
     * IMPORTANTE: No se puede eliminar el propio usuario admin
     */
    suspend fun deleteUser(userId: String, currentUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Eliminando usuario: $userId")

                // Verificar que no sea el mismo usuario
                if (userId == currentUserId) {
                    return@withContext Result.Error("No puedes eliminar tu propia cuenta")
                }

                // 1. Eliminar roles
                supabase.from("user_roles").delete {
                    filter { eq("user_id", userId) }
                }

                // 2. Eliminar perfil (esto también eliminará el usuario de auth por cascade)
                supabase.from("profiles").delete {
                    filter { eq("id", userId) }
                }

                Log.d(TAG, "Usuario eliminado exitosamente")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar usuario: ${e.message}", e)
                Result.Error("Error al eliminar usuario: ${e.message}")
            }
        }

    /**
     * Cambia el rol de un usuario
     */
    suspend fun changeUserRole(userId: String, newRole: Role): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Cambiando rol de usuario: $userId a ${newRole.value}")

                // Validar que no sea delivery
                if (newRole == Role.DELIVERY) {
                    return@withContext Result.Error("No se puede asignar el rol delivery")
                }

                // Obtener el ID del nuevo rol
                val roles = supabase.from("roles")
                    .select {
                        filter { eq("name", newRole.value) }
                    }
                    .decodeList<RoleRecord>()

                val roleId = roles.firstOrNull()?.id
                    ?: return@withContext Result.Error("Rol no encontrado")

                // Eliminar roles actuales
                supabase.from("user_roles").delete {
                    filter { eq("user_id", userId) }
                }

                // Asignar nuevo rol
                supabase.from("user_roles").insert(
                    mapOf(
                        "user_id" to userId,
                        "role_id" to roleId
                    )
                )

                Log.d(TAG, "Rol cambiado exitosamente")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Error al cambiar rol: ${e.message}", e)
                Result.Error("Error al cambiar rol: ${e.message}")
            }
        }
}