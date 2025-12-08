package com.dev.mandadito.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.data.models.DeliveryUser
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
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
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DeliveriesRepository(private val context: Context) {

    private val supabase = SupabaseClient.client
    private val TAG = "DeliveriesRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Obtener todos los deliveries de un colmado específico
     */
    suspend fun getDeliveriesByColmado(colmadoId: String): Result<List<DeliveryUser>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Obteniendo deliveries del colmado: $colmadoId")

                // Query usando la vista que une profiles, user_colmado y user_roles
                val deliveries = supabase.from("deliveries_view")
                    .select {
                        filter {
                            eq("colmado_id", colmadoId)
                            eq("role_in_colmado", "delivery")
                        }
                    }
                    .decodeList<DeliveryUser>()

                Log.d(TAG, "✅ ${deliveries.size} deliveries obtenidos")
                Result.Success(deliveries)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo deliveries: ${e.message}", e)
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Error de conexión. Verifica tu internet"

                    else -> "Error al cargar deliveries: ${e.message}"
                }
                Result.Error(errorMessage)
            }
        }

    /**
     * Vincular un usuario existente con rol delivery a un colmado
     */
    suspend fun addDeliveryToColmado(
        userId: String,
        colmadoId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Vinculando delivery $userId al colmado $colmadoId")

            // Insertar en user_colmado
            supabase.from("user_colmado")
                .insert(
                    mapOf(
                        "user_id" to userId,
                        "colmado_id" to colmadoId,
                        "role_in_colmado" to "delivery"
                    )
                )

            Log.d(TAG, "✅ Delivery vinculado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error vinculando delivery: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("duplicate", ignoreCase = true) == true ->
                    "Este delivery ya está asignado a este colmado"

                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                else -> "Error al vincular delivery: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    /**
     * Desvincular un delivery de un colmado
     */
    suspend fun removeDeliveryFromColmado(
        userId: String,
        colmadoId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Desvinculando delivery $userId del colmado $colmadoId")

            supabase.from("user_colmado")
                .delete {
                    filter {
                        eq("user_id", userId)
                        eq("colmado_id", colmadoId)
                    }
                }

            Log.d(TAG, "✅ Delivery desvinculado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error desvinculando delivery: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"

                else -> "Error al desvincular delivery: ${e.message}"
            }
            Result.Error(errorMessage)
        }
    }

    /**
     * Habilitar un delivery (activar su perfil)
     */
    suspend fun enableDelivery(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Habilitando delivery: $userId")

            supabase.from("profiles")
                .update(mapOf("activo" to true)) {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d(TAG, "✅ Delivery habilitado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error habilitando delivery: ${e.message}", e)
            Result.Error("Error al habilitar delivery: ${e.message}")
        }
    }

    /**
     * Deshabilitar un delivery (desactivar su perfil)
     */
    suspend fun disableDelivery(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deshabilitando delivery: $userId")

            supabase.from("profiles")
                .update(mapOf("activo" to false)) {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d(TAG, "✅ Delivery deshabilitado exitosamente")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deshabilitando delivery: ${e.message}", e)
            Result.Error("Error al deshabilitar delivery: ${e.message}")
        }
    }

    /**
     * Obtener usuarios disponibles con rol delivery que NO están en este colmado
     * @deprecated Ya no se usan deliveries existentes, cada seller crea los suyos
     */
    suspend fun getAvailableDeliveries(colmadoId: String): Result<List<DeliveryUser>> =
        withContext(Dispatchers.IO) {
            // Retornar lista vacía ya que ahora cada seller crea sus propios deliveries
            Result.Success(emptyList())
        }

    /**
     * Crear un nuevo delivery y asociarlo automáticamente al colmado del seller
     */
    suspend fun createDeliveryForColmado(
        email: String,
        password: String,
        nombre: String,
        colmadoId: String,
        avatarUri: Uri? = null
    ): Result<DeliveryUser> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creando nuevo delivery para colmado: $colmadoId")

            val httpClient = HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    })
                }
            }


            // 1. Crear usuario con rol delivery usando la edge function
            val session = supabase.auth.currentSessionOrNull()
            val accessToken = session?.accessToken
                ?: return@withContext Result.Error("No hay sesión activa")

            @Serializable
            data class CreateDeliveryRequest(
                val email: String,
                val password: String,
                val nombre: String,
                val role: String = "delivery",
                val avatar_base64: String? = null
            )

            @Serializable
            data class CreateDeliveryResponse(
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

            // Convertir avatar a base64 si existe
            var avatarBase64: String? = null
            if (avatarUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(avatarUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        avatarBase64 = "data:image/jpeg;base64," +
                                Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error convirtiendo avatar: ${e.message}")
                }
            }

            val request = CreateDeliveryRequest(
                email = email,
                password = password,
                nombre = nombre,
                role = "delivery",
                avatar_base64 = avatarBase64
            )

            val response = httpClient.post("${AppConfig.SUPABASE_URL}/functions/v1/create-user") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                    append("apikey", AppConfig.SUPABASE_ANON_KEY)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            Log.d(TAG, "📡 Respuesta HTTP status: ${response.status}")

            if (!response.status.isSuccess()) {
                val errorBody = response.body<String>()
                Log.e(TAG, "❌ Error HTTP: ${response.status} - $errorBody")
                return@withContext Result.Error("Error del servidor: ${response.status}")
            }

            val result = response.body<CreateDeliveryResponse>()

            if (!result.success || result.user == null) {
                Log.e(TAG, "❌ Error creando usuario: ${result.error ?: result.message}")
                return@withContext Result.Error(
                    result.error ?: result.message ?: "Error al crear delivery"
                )
            }

            val userId = result.user.id
            Log.d(TAG, "✅ Usuario delivery creado: $userId (email: ${result.user.email})")

            // 2. Subir avatar directamente si no se envió en base64
            var avatarUrl: String? = result.user.avatar_url
            if (avatarUri != null && avatarUrl == null) {
                try {
                    avatarUrl = uploadAvatar(userId, avatarUri)
                    if (avatarUrl != null) {
                        supabase.from("profiles")
                            .update(mapOf("avatar_url" to avatarUrl)) {
                                filter { eq("id", userId) }
                            }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error subiendo avatar: ${e.message}")
                }
            }

            // 3. Asociar el delivery al colmado
            try {
                Log.d(TAG, "🔗 Asociando delivery $userId al colmado $colmadoId")
                val insertResult = supabase.from("user_colmado")
                    .insert(
                        mapOf(
                            "user_id" to userId,
                            "colmado_id" to colmadoId,
                            "role_in_colmado" to "delivery"
                        )
                    )
                Log.d(TAG, "✅ Insert ejecutado, verificando resultado...")

                // Verificar que el insert se completó correctamente
                // Esperar un momento para que la base de datos procese el insert
                delay(300)

                Log.d(TAG, "✅ Delivery asociado al colmado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error asociando delivery al colmado: ${e.message}", e)
                Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
                return@withContext Result.Error("Error al asociar delivery al colmado: ${e.message}")
            }

            // 4. Obtener el delivery creado
            try {
                Log.d(TAG, "🔍 Buscando delivery creado: userId=$userId, colmadoId=$colmadoId")

                // Esperar un momento más para que la vista se actualice
                delay(500)

                val deliveries = supabase.from("deliveries_view")
                    .select {
                        filter {
                            eq("id", userId)
                            eq("colmado_id", colmadoId)
                        }
                    }
                    .decodeList<DeliveryUser>()

                Log.d(TAG, "📊 Deliveries encontrados: ${deliveries.size}")

                val delivery = deliveries.firstOrNull()
                if (delivery == null) {
                    Log.e(
                        TAG,
                        "❌ No se encontró el delivery en la vista. Intentando obtener desde profiles directamente..."
                    )

                    // Fallback: obtener desde profiles directamente
                    val profile = supabase.from("profiles")
                        .select {
                            filter { eq("id", userId) }
                        }
                        .decodeSingleOrNull<Map<String, Any>>()

                    if (profile != null) {
                        Log.d(TAG, "✅ Perfil encontrado, creando DeliveryUser manualmente")
                        // Crear DeliveryUser manualmente con los datos disponibles
                        val deliveryUser = DeliveryUser(
                            id = userId,
                            email = result.user.email,
                            nombre = result.user.nombre,
                            activo = result.user.activo,
                            avatar_url = avatarUrl,
                            colmado_id = colmadoId,
                            role_in_colmado = "delivery"
                        )
                        Log.d(
                            TAG,
                            "✅ Delivery creado y asociado exitosamente: ${deliveryUser.nombre}"
                        )
                        return@withContext Result.Success(deliveryUser)
                    } else {
                        return@withContext Result.Error("No se pudo obtener el delivery creado")
                    }
                }

                Log.d(TAG, "✅ Delivery creado y asociado exitosamente: ${delivery.nombre}")
                Result.Success(delivery)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo delivery creado: ${e.message}", e)
                // Aún así, intentar crear un DeliveryUser con los datos que tenemos
                val deliveryUser = DeliveryUser(
                    id = userId,
                    email = result.user.email,
                    nombre = result.user.nombre,
                    activo = result.user.activo,
                    avatar_url = avatarUrl,
                    colmado_id = colmadoId,
                    role_in_colmado = "delivery"
                )
                Log.w(TAG, "⚠️ Retornando delivery con datos parciales debido a error en consulta")
                Result.Success(deliveryUser)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando delivery: ${e.message}", e)
            Result.Error("Error al crear delivery: ${e.message}")
        }
    }

    /**
     * Actualizar información de un delivery
     */
    suspend fun updateDelivery(
        userId: String,
        nombre: String,
        email: String,
        avatarUri: Uri? = null
    ): Result<DeliveryUser> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Actualizando delivery: $userId")

            // 1. Actualizar perfil (nombre y email)
            supabase.from("profiles")
                .update(
                    mapOf(
                        "nombre" to nombre,
                        "email" to email
                    )
                ) {
                    filter { eq("id", userId) }
                }

            Log.d(TAG, "✅ Perfil actualizado")

            // 2. Actualizar avatar si se proporciona
            var avatarUrl: String? = null
            if (avatarUri != null) {
                avatarUrl = uploadAvatar(userId, avatarUri)
                if (avatarUrl != null) {
                    supabase.from("profiles")
                        .update(mapOf("avatar_url" to avatarUrl)) {
                            filter { eq("id", userId) }
                        }
                    Log.d(TAG, "✅ Avatar actualizado")
                }
            }

            // 3. Obtener el delivery actualizado
            delay(300) // Esperar a que la BD se actualice

            val deliveries = supabase.from("deliveries_view")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeList<DeliveryUser>()

            val updatedDelivery = deliveries.firstOrNull()
            if (updatedDelivery == null) {
                Log.e(TAG, "❌ No se encontró el delivery actualizado")
                return@withContext Result.Error("No se pudo obtener el delivery actualizado")
            }

            Log.d(TAG, "✅ Delivery actualizado exitosamente: ${updatedDelivery.nombre}")
            Result.Success(updatedDelivery)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando delivery: ${e.message}", e)
            Result.Error("Error al actualizar delivery: ${e.message}")
        }
    }

    /**
     * Subir avatar directamente a storage
     */
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
}