package com.dev.mandadito.data.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.RoleRecord
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.models.UserRole
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ============================================
// DATOS SERIALIZABLES
// ============================================
@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val nombre: String,
    val role: String,
    val avatar_base64: String? = null
)

@Serializable
data class CreateUserResponse(
    val success: Boolean,
    val user: UserResponseData? = null,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class UserResponseData(
    val id: String,
    val email: String,
    val nombre: String,
    val role: String,
    val activo: Boolean,
    val avatar_url: String? = null
)

class AdminRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "AdminRepository"

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    // ============================================
    // OBTENER TODOS LOS USUARIOS
    // ============================================
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

            Log.d(TAG, "✅ ${usersWithRoles.size} usuarios obtenidos")
            Result.Success(usersWithRoles)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo usuarios: ${e.message}", e)
            Result.Error("Error al cargar usuarios: ${e.message}")
        }
    }

    // ============================================
    // CREAR USUARIO VÍA EDGE FUNCTION (SIN AVATAR)
    // ============================================
    suspend fun createUser(
        email: String,
        password: String,
        nombre: String,
        role: Role,
        avatarUri: Uri? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creando usuario: $email con rol: ${role.value}")

            // 1. Crear usuario sin avatar (más rápido)
            val request = CreateUserRequest(
                email = email,
                password = password,
                nombre = nombre,
                role = role.value,
                avatar_base64 = null // No enviar avatar en el request
            )

            val session = supabase.auth.currentSessionOrNull()
            val accessToken = session?.accessToken
                ?: return@withContext Result.Error("No hay sesión activa")

            val response = httpClient.post("${AppConfig.SUPABASE_URL}/functions/v1/create-user") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                    append("apikey", AppConfig.SUPABASE_ANON_KEY)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val result = response.body<CreateUserResponse>()

            if (!result.success || result.user == null) {
                Log.e(TAG, "❌ Error en respuesta: ${result.error ?: result.message}")
                return@withContext Result.Error(result.error ?: result.message ?: "Error desconocido")
            }

            val userId = result.user.id
            Log.d(TAG, "✅ Usuario creado exitosamente: ${result.user.email}")

            // 2. Subir avatar directamente desde Android si existe
            var avatarUrl: String? = null
            if (avatarUri != null && userId != null) {
                try {
                    Log.d(TAG, "📸 Subiendo avatar directamente desde Android...")
                    avatarUrl = uploadAvatar(userId, avatarUri)
                    if (avatarUrl != null) {
                        Log.d(TAG, "✅ Avatar subido exitosamente: $avatarUrl")
                        
                        // Actualizar perfil con la URL del avatar
                        supabase.from("profiles")
                            .update(mapOf("avatar_url" to avatarUrl)) {
                                filter { eq("id", userId) }
                            }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error subiendo avatar (no crítico): ${e.message}", e)
                    // Continuar sin avatar si falla
                }
            }

            val userProfile = UserProfile(
                id = result.user.id,
                email = result.user.email,
                nombre = result.user.nombre,
                activo = result.user.activo,
                avatar_url = avatarUrl
            )

            Result.Success(userProfile)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando usuario: ${e.message}", e)
            Result.Error("Error al crear usuario: ${e.message}")
        }
    }

    // ============================================
    // SUBIR AVATAR DIRECTAMENTE A STORAGE
    // ============================================
    private suspend fun uploadAvatar(userId: String, avatarUri: Uri): String? {
        return try {
            Log.d(TAG, "📸 Leyendo imagen desde URI: $avatarUri")
            
            // Leer imagen
            val inputStream = context.contentResolver.openInputStream(avatarUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) {
                Log.e(TAG, "❌ No se pudieron leer los bytes de la imagen")
                return null
            }

            Log.d(TAG, "📦 Imagen leída: ${bytes.size} bytes")

            // Determinar extensión basada en el tipo MIME
            val mimeType = context.contentResolver.getType(avatarUri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }

            val fileName = "$userId/avatar.$extension"
            Log.d(TAG, "📁 Subiendo archivo: $fileName (${bytes.size} bytes)")

            // Subir a Storage directamente
            supabase.storage.from("profile-pictures")
                .upload(fileName, bytes, upsert = true)

            // Construir URL pública
            val publicUrl = "${AppConfig.SUPABASE_URL}/storage/v1/object/public/profile-pictures/$fileName"
            
            Log.d(TAG, "✅ Avatar subido exitosamente: $publicUrl")
            publicUrl

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error subiendo avatar: ${e.message}", e)
            null
        }
    }

    // ============================================
    // ACTUALIZAR PERFIL DE USUARIO
    // ============================================
    suspend fun updateUserProfile(
        userId: String,
        nombre: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Actualizando perfil: $userId")

            supabase.from("profiles")
                .update(
                    mapOf(
                        "nombre" to nombre
                    )
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

            // Obtener el perfil actualizado
            val updatedProfile = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<UserProfile>()

            Log.d(TAG, "✅ Perfil actualizado: ${updatedProfile.nombre}")
            Result.Success(updatedProfile)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando perfil: ${e.message}", e)
            Result.Error("Error al actualizar perfil: ${e.message}")
        }
    }

    // ============================================
    // DESHABILITAR USUARIO
    // ============================================
    suspend fun disableUser(
        userId: String,
        currentUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId == currentUserId) {
                return@withContext Result.Error("No puedes deshabilitarte a ti mismo")
            }

            Log.d(TAG, "Deshabilitando usuario: $userId")

            supabase.from("profiles")
                .update(
                    mapOf("activo" to false)
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d(TAG, "✅ Usuario deshabilitado")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deshabilitando usuario: ${e.message}", e)
            Result.Error("Error al deshabilitar usuario: ${e.message}")
        }
    }

    // ============================================
    // HABILITAR USUARIO
    // ============================================
    suspend fun enableUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Habilitando usuario: $userId")

            supabase.from("profiles")
                .update(
                    mapOf("activo" to true)
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d(TAG, "✅ Usuario habilitado")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error habilitando usuario: ${e.message}", e)
            Result.Error("Error al habilitar usuario: ${e.message}")
        }
    }

    // ============================================
    // ELIMINAR USUARIO
    // ============================================
    suspend fun deleteUser(
        userId: String,
        currentUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId == currentUserId) {
                return@withContext Result.Error("No puedes eliminarte a ti mismo")
            }

            Log.d(TAG, "Eliminando usuario: $userId")

            // Primero eliminar roles
            supabase.from("user_roles")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }

            // Luego eliminar perfil
            supabase.from("profiles")
                .delete {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d(TAG, "✅ Usuario eliminado")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando usuario: ${e.message}", e)
            Result.Error("Error al eliminar usuario: ${e.message}")
        }
    }

    // ============================================
    // CAMBIAR ROL DE USUARIO
    // ============================================
    suspend fun changeUserRole(
        userId: String,
        newRole: Role
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cambiando rol de usuario $userId a ${newRole.value}")

            // Obtener el ID del nuevo rol
            val roleRecord = supabase.from("roles")
                .select {
                    filter { eq("name", newRole.value) }
                }
                .decodeSingle<RoleRecord>()

            // Eliminar roles anteriores
            supabase.from("user_roles")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }

            // Asignar nuevo rol
            supabase.from("user_roles")
                .insert(
                    UserRole(
                        user_id = userId,
                        role_id = roleRecord.id
                    )
                )

            Log.d(TAG, "✅ Rol cambiado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cambiando rol: ${e.message}", e)
            Result.Error("Error al cambiar rol: ${e.message}")
        }
    }

    // ============================================
    // OBTENER ROL DE USUARIO
    // ============================================
    private suspend fun getUserRole(userId: String): Role? {
        return try {
            val userRoles = supabase.from("user_roles")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<UserRole>()

            if (userRoles.isEmpty()) {
                Log.w(TAG, "No se encontró rol para usuario: $userId")
                return null
            }

            val roleId = userRoles.first().role_id

            val roleRecord = supabase.from("roles")
                .select {
                    filter { eq("id", roleId) }
                }
                .decodeSingle<RoleRecord>()

            Role.fromString(roleRecord.name)

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo rol de usuario $userId: ${e.message}")
            null
        }
    }

    // ============================================
    // CLEANUP
    // ============================================
    fun cleanup() {
        try {
            httpClient.close()
            Log.d(TAG, "HttpClient cerrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando HttpClient: ${e.message}")
        }
    }
}