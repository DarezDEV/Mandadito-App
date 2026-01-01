package com.dev.mandadito.data.repository

import android.util.Log
import com.dev.mandadito.data.models.Notification
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NotificationRepository {

    private val supabase = SupabaseClient.client
    private val tag = "NotificationRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    @Serializable
    private data class MarkAsReadData(
        @SerialName("is_read")
        val isRead: Boolean = true
    )

    // Obtener todas las notificaciones del usuario
    suspend fun getUserNotifications(userId: String): Result<List<Notification>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "📥 Obteniendo notificaciones para usuario: $userId")

                val notifications = supabase.from("notifications")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("timestamp", Order.DESCENDING)
                    }
                    .decodeList<Notification>()

                Log.d(tag, "✅ ${notifications.size} notificaciones obtenidas")
                Result.Success(notifications)

            } catch (e: Exception) {
                Log.e(tag, "❌ Error obteniendo notificaciones: ${e.message}", e)
                Result.Error("Error al cargar notificaciones: ${e.message}")
            }
        }

    // Marcar notificación como leída
    suspend fun markAsRead(notificationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "✓ Marcando notificación como leída: $notificationId")

                supabase.from("notifications")
                    .update(MarkAsReadData(isRead = true)) {
                        filter {
                            eq("id", notificationId)
                        }
                    }

                Log.d(tag, "✅ Notificación marcada como leída")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(tag, "❌ Error marcando como leída: ${e.message}", e)
                Result.Error("Error al actualizar notificación: ${e.message}")
            }
        }

    // Marcar todas como leídas
    suspend fun markAllAsRead(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "✓✓ Marcando todas como leídas para: $userId")

                supabase.from("notifications")
                    .update(MarkAsReadData(isRead = true)) {
                        filter {
                            eq("user_id", userId)
                            eq("is_read", false)
                        }
                    }

                Log.d(tag, "✅ Todas las notificaciones marcadas como leídas")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(tag, "❌ Error marcando todas como leídas: ${e.message}", e)
                Result.Error("Error al actualizar notificaciones: ${e.message}")
            }
        }

    // Eliminar notificación
    suspend fun deleteNotification(notificationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "🗑️ Eliminando notificación: $notificationId")

                supabase.from("notifications")
                    .delete {
                        filter {
                            eq("id", notificationId)
                        }
                    }

                Log.d(tag, "✅ Notificación eliminada")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(tag, "❌ Error eliminando notificación: ${e.message}", e)
                Result.Error("Error al eliminar notificación: ${e.message}")
            }
        }

    // Eliminar todas las notificaciones leídas
    suspend fun deleteReadNotifications(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "🗑️ Eliminando notificaciones leídas para: $userId")

                supabase.from("notifications")
                    .delete {
                        filter {
                            eq("user_id", userId)
                            eq("is_read", true)
                        }
                    }

                Log.d(tag, "✅ Notificaciones leídas eliminadas")
                Result.Success(Unit)

            } catch (e: Exception) {
                Log.e(tag, "❌ Error eliminando notificaciones: ${e.message}", e)
                Result.Error("Error al eliminar notificaciones: ${e.message}")
            }
        }
}