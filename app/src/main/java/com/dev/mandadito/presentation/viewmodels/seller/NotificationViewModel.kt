package com.dev.mandadito.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.local.dao.NotificationDao
import com.dev.mandadito.data.local.database.MandaditoDatabase
import com.dev.mandadito.data.local.entities.NotificationEntity
import com.dev.mandadito.data.models.Notification
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.data.repository.NotificationRepository
import com.dev.mandadito.utils.SharedPreferenHelper
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val unreadCount: Int = 0,
    val filterType: String? = null,
    val showOnlyUnread: Boolean = false
)

class NotificationViewModel(context: Context) : ViewModel() {

    private val database = MandaditoDatabase.getDatabase(context)
    private val notificationDao = database.notificationDao()
    private val notificationRepository = NotificationRepository()
    private val sharedPrefs = SharedPreferenHelper(context)
    private val supabase = SupabaseClient.client
    private val TAG = "NotificationViewModel"

    private var realtimeChannel: RealtimeChannel? = null

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    val filteredNotifications: List<Notification>
        get() {
            var filtered = _uiState.value.notifications

            _uiState.value.filterType?.let { type ->
                filtered = filtered.filter { it.type == type }
            }

            if (_uiState.value.showOnlyUnread) {
                filtered = filtered.filter { !it.isRead }
            }

            return filtered
        }

    init {
        loadNotifications()
        syncWithSupabase()
        startRealtimeSubscription()
    }

    private fun loadNotifications() {
        val userId = sharedPrefs.getUserId() ?: return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Combinar el flujo de notificaciones con el contador de no leídas
                combine(
                    notificationDao.getAllNotifications(userId),
                    notificationDao.getUnreadCount(userId)
                ) { notifications, unreadCount ->
                    Pair(notifications.map { it.toNotification() }, unreadCount)
                }.collect { (notifications, unreadCount) ->
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        unreadCount = unreadCount,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar notificaciones: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "✓ Marcando notificación como leída: $notificationId")

                // El notificationId ahora es el supabaseId (UUID)
                // Marcar en Supabase primero
                when (val result = notificationRepository.markAsRead(notificationId)) {
                    is NotificationRepository.Result.Success -> {
                        Log.d(TAG, "✅ Notificación marcada como leída en Supabase")

                        // Marcar en Room usando supabaseId
                        notificationDao.markAsReadBySupabaseId(notificationId)
                    }
                    is NotificationRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error marcando como leída: ${result.message}")
                        // Aún así marcar localmente para mejor UX
                        notificationDao.markAsReadBySupabaseId(notificationId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error marcando como leída: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al marcar como leída: ${e.message}"
                )
            }
        }
    }

    fun markAllAsRead() {
        val userId = sharedPrefs.getUserId() ?: return

        viewModelScope.launch {
            try {
                Log.d(TAG, "✓✓ Marcando todas las notificaciones como leídas...")

                // Marcar en Supabase primero
                when (val result = notificationRepository.markAllAsRead(userId)) {
                    is NotificationRepository.Result.Success -> {
                        Log.d(TAG, "✅ Todas las notificaciones marcadas como leídas en Supabase")

                        // Marcar en Room también
                        notificationDao.markAllAsRead(userId)

                        _uiState.value = _uiState.value.copy(
                            successMessage = "Todas las notificaciones marcadas como leídas"
                        )
                    }
                    is NotificationRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error marcando como leídas: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Error al marcar como leídas: ${result.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error marcando como leídas: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al marcar como leídas: ${e.message}"
                )
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Eliminando notificación: $notificationId")

                // El notificationId ahora es el supabaseId (UUID)
                // Eliminar de Supabase primero
                when (val result = notificationRepository.deleteNotification(notificationId)) {
                    is NotificationRepository.Result.Success -> {
                        Log.d(TAG, "✅ Notificación eliminada de Supabase")
                        // Eliminar de Room usando supabaseId
                        val notification = notificationDao.getNotificationBySupabaseId(notificationId)
                        notification?.let {
                            notificationDao.deleteById(it.id)
                        }
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Notificación eliminada"
                        )
                    }
                    is NotificationRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error eliminando de Supabase: ${result.message}")
                        // Aún así eliminar localmente
                        val notification = notificationDao.getNotificationBySupabaseId(notificationId)
                        notification?.let {
                            notificationDao.deleteById(it.id)
                        }
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Notificación eliminada localmente"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error eliminando notificación: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar: ${e.message}"
                )
            }
        }
    }

    fun deleteReadNotifications() {
        val userId = sharedPrefs.getUserId() ?: return

        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Eliminando notificaciones leídas...")

                // Eliminar de Supabase primero
                when (val result = notificationRepository.deleteReadNotifications(userId)) {
                    is NotificationRepository.Result.Success -> {
                        Log.d(TAG, "✅ Notificaciones leídas eliminadas de Supabase")

                        // Eliminar de Room también
                        notificationDao.deleteReadNotifications(userId)

                        _uiState.value = _uiState.value.copy(
                            successMessage = "Notificaciones leídas eliminadas"
                        )
                    }
                    is NotificationRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error eliminando notificaciones: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Error al eliminar: ${result.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error eliminando notificaciones: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar: ${e.message}"
                )
            }
        }
    }

    fun setFilterType(type: String?) {
        _uiState.value = _uiState.value.copy(filterType = type)
    }

    fun setShowOnlyUnread(show: Boolean) {
        _uiState.value = _uiState.value.copy(showOnlyUnread = show)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // ============================================
    // SINCRONIZACIÓN CON SUPABASE
    // ============================================

    /**
     * Refresca las notificaciones desde Supabase (pull-to-refresh)
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        syncWithSupabase()
    }

    /**
     * Sincroniza notificaciones desde Supabase al iniciar
     */
    fun syncWithSupabase() {
        val userId = sharedPrefs.getUserId() ?: return

        viewModelScope.launch {
            try {
                Log.d(TAG, "📥 Sincronizando notificaciones desde Supabase...")

                when (val result = notificationRepository.getUserNotifications(userId)) {
                    is NotificationRepository.Result.Success -> {
                        val supabaseNotifications = result.data
                        Log.d(TAG, "✅ ${supabaseNotifications.size} notificaciones obtenidas de Supabase")

                        // IMPORTANTE: Limpiar Room antes de sincronizar para evitar duplicados
                        notificationDao.deleteAllNotifications(userId)
                        Log.d(TAG, "🗑️ Notificaciones locales limpiadas")

                        // Guardar en Room para caché offline
                        supabaseNotifications.forEach { notification ->
                            try {
                                val entity = notification.toNotificationEntity(userId)
                                notificationDao.insertNotification(entity)
                            } catch (e: Exception) {
                                Log.e(TAG, "⚠️ Error guardando notificación: ${e.message}")
                            }
                        }

                        Log.d(TAG, "✅ Notificaciones sincronizadas con Room")
                        _uiState.value = _uiState.value.copy(isRefreshing = false)
                    }
                    is NotificationRepository.Result.Error -> {
                        Log.e(TAG, "❌ Error sincronizando notificaciones: ${result.message}")
                        _uiState.value = _uiState.value.copy(isRefreshing = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en sincronización: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    /**
     * Inicia suscripción Realtime para notificaciones nuevas
     */
    private fun startRealtimeSubscription() {
        val userId = sharedPrefs.getUserId() ?: return

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Iniciando suscripción Realtime para notificaciones...")

                realtimeChannel = supabase.realtime.channel("notifications:user:$userId")

                @Suppress("DEPRECATION")
                realtimeChannel!!.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                    filter(column = "user_id", operator = FilterOperator.EQ, value = userId)
                }.onEach { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            Log.d(TAG, "🆕 Nueva notificación insertada - sincronizando...")
                            syncWithSupabase()
                        }
                        is PostgresAction.Update -> {
                            Log.d(TAG, "🔄 Notificación actualizada - sincronizando...")
                            syncWithSupabase()
                        }
                        is PostgresAction.Delete -> {
                            Log.d(TAG, "🗑️ Notificación eliminada - sincronizando...")
                            syncWithSupabase()
                        }
                        else -> {
                            Log.d(TAG, "ℹ️ Acción Realtime no manejada: ${action::class.simpleName}")
                        }
                    }
                }.launchIn(viewModelScope)

                realtimeChannel!!.subscribe()
                Log.d(TAG, "✅ Suscripción Realtime iniciada correctamente")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error iniciando Realtime: ${e.message}", e)
            }
        }
    }

    /**
     * Convierte Notification (Supabase) a NotificationEntity (Room)
     */
    private fun Notification.toNotificationEntity(userId: String): NotificationEntity {
        // Parsear created_at ISO 8601 a millis
        val timestampMillis = try {
            Instant.parse(this.createdAt).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        return NotificationEntity(
            id = 0, // Room auto-genera el ID
            supabaseId = this.id, // ✅ Guardar UUID de Supabase
            userId = userId,
            type = this.type,
            title = this.title,
            message = this.message,
            timestamp = timestampMillis,
            isRead = this.isRead,
            isPush = this.isPush,
            createdAt = timestampMillis
        )
    }

    // Método para limpiar notificaciones antiguas (más de 30 días)
    fun cleanOldNotifications() {
        val userId = sharedPrefs.getUserId() ?: return
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)

        viewModelScope.launch {
            try {
                notificationDao.deleteOldNotifications(userId, thirtyDaysAgo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============================================
    // CLEANUP
    // ============================================

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                realtimeChannel?.unsubscribe()
                supabase.realtime.removeChannel(realtimeChannel!!)
                Log.d(TAG, "✅ Suscripción Realtime cerrada")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error cerrando Realtime: ${e.message}")
            }
        }
    }
}