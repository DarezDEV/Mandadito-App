package com.dev.mandadito.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "ProfileRepository"

    private val httpClient = HttpClient(OkHttp) {
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

    @Serializable
    private data class UpdateProfileResponse(
        val success: Boolean,
        val user: UserProfile? = null,
        val error: String? = null
    )

    @Serializable
    private data class ProfileUpdate(
        val nombre: String,
        val email: String
    )

    @Serializable
    private data class EdgeFunctionRequest(
        val nombre: String,
        val email: String,
        val avatar_base64: String? = null
    )

    /**
     * Convierte un Uri de imagen a string base64
     */
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo imagen a base64: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene el perfil del usuario actual autenticado
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo perfil del usuario actual...")

            // Obtener el usuario autenticado
            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                Log.e(TAG, "No hay usuario autenticado")
                return@withContext Result.Error("No hay sesión activa")
            }

            val userId = currentUser.id
            Log.d(TAG, "Usuario ID: $userId")

            // Obtener el perfil desde la tabla profiles
            val profile = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfile>()

            if (profile == null) {
                Log.e(TAG, "Perfil no encontrado para el usuario: $userId")
                return@withContext Result.Error("Perfil no encontrado")
            }

            Log.d(TAG, "✅ Perfil obtenido: ${profile.nombre}, email: ${profile.email}")
            return@withContext Result.Success(profile)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo perfil: ${e.message}", e)
            return@withContext Result.Error("Error al obtener perfil: ${e.message}")
        }
    }

    /**
     * Actualiza el perfil del usuario (nombre, email, avatar)
     * Usa la Edge Function para actualizar todo incluyendo avatar
     */
    suspend fun updateProfile(
        nombre: String,
        email: String,
        avatarUri: Uri? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔷 Actualizando perfil completo...")

            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.Error("No hay sesión activa")
            }

            // Obtener el token de autenticación
            val session = supabase.auth.currentSessionOrNull()
            val accessToken = session?.accessToken
            if (accessToken == null) {
                Log.e(TAG, "❌ No hay sesión o token disponible")
                return@withContext Result.Error("No se pudo obtener token de autenticación")
            }

            Log.d(TAG, "✅ Token obtenido: ${accessToken.take(20)}...")

            // 1. Convertir avatar a base64 si existe
            val avatarBase64 = if (avatarUri != null) {
                Log.d(TAG, "📸 Convirtiendo avatar a base64...")
                uriToBase64(avatarUri)
            } else {
                null
            }

            // 2. Preparar datos para Edge Function
            val requestData = EdgeFunctionRequest(
                nombre = nombre,
                email = email,
                avatar_base64 = avatarBase64
            )

            val edgeFunctionUrl = "${AppConfig.SUPABASE_URL}/functions/v1/update-profile"
            Log.d(TAG, "📤 Enviando petición a: $edgeFunctionUrl")
            Log.d(TAG, "📦 Datos: nombre=$nombre, email=$email, avatar=${if (avatarBase64 != null) "presente" else "null"}")

            // 3. Llamar a la Edge Function
            val response = httpClient.post(edgeFunctionUrl) {
                headers {
                    append("Authorization", "Bearer $accessToken")
                    append("apikey", AppConfig.SUPABASE_ANON_KEY)
                }
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(requestData)
            }

            Log.d(TAG, "📥 Respuesta recibida: ${response.status}")

            // 4. Parsear respuesta
            val responseData = response.body<UpdateProfileResponse>()

            if (responseData.success && responseData.user != null) {
                Log.d(TAG, "✅ Perfil actualizado exitosamente via Edge Function")
                return@withContext Result.Success(responseData.user)
            } else {
                val errorMsg = responseData.error ?: "Error desconocido"
                Log.e(TAG, "❌ Error en Edge Function: $errorMsg")
                return@withContext Result.Error(errorMsg)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando perfil: ${e.message}", e)
            return@withContext Result.Error("Error al actualizar perfil: ${e.message}")
        }
    }

    /**
     * Cambia la contraseña del usuario
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔷 Cambiando contraseña...")

            // 1. Obtener el usuario actual
            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.Error("No hay sesión activa")
            }

            // 2. Re-autenticar con la contraseña actual
            try {
                supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                    this.email = currentUser.email ?: ""
                    this.password = currentPassword
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Contraseña actual incorrecta")
                return@withContext Result.Error("Contraseña actual incorrecta")
            }

            // 3. Actualizar la contraseña usando updateUser
            try {
                supabase.auth.updateUser {
                    password = newPassword
                }
                Log.d(TAG, "✅ Contraseña actualizada exitosamente")
                return@withContext Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error actualizando contraseña: ${e.message}", e)
                return@withContext Result.Error("Error al actualizar contraseña: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cambiando contraseña: ${e.message}", e)
            return@withContext Result.Error("Error al cambiar contraseña: ${e.message}")
        }
    }
}
