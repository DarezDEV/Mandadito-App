package com.dev.mandadito.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.local.dao.NotificationDao  // ✅ AGREGAR ESTE IMPORT
import com.dev.mandadito.data.local.database.MandaditoDatabase
import com.dev.mandadito.data.local.entities.NotificationEntity
import com.dev.mandadito.data.models.Notification
import com.dev.mandadito.utils.SharedPreferenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val unreadCount: Int = 0,
    val filterType: String? = null,
    val showOnlyUnread: Boolean = false
)

class NotificationViewModel(context: Context) : ViewModel() {

    private val database = MandaditoDatabase.getDatabase(context)
    private val notificationDao = database.notificationDao()
    private val sharedPrefs = SharedPreferenHelper(context)

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
                val id = notificationId.toLongOrNull() ?: return@launch
                notificationDao.markAsRead(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al marcar como leída: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }

    fun markAllAsRead() {
        val userId = sharedPrefs.getUserId() ?: return

        viewModelScope.launch {
            try {
                notificationDao.markAllAsRead(userId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Todas las notificaciones marcadas como leídas"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al marcar como leídas: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                val id = notificationId.toLongOrNull() ?: return@launch
                notificationDao.deleteById(id)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Notificación eliminada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }

    fun deleteReadNotifications() {
        val userId = sharedPrefs.getUserId() ?: return

        viewModelScope.launch {
            try {
                notificationDao.deleteReadNotifications(userId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Notificaciones leídas eliminadas"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar: ${e.message}"
                )
                e.printStackTrace()
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

    fun createNotification(
        type: String,
        title: String,
        message: String,
        isPush: Boolean = false
    ) {
        val userId = sharedPrefs.getUserId() ?: return

        viewModelScope.launch {
            try {
                val notification = NotificationEntity(
                    userId = userId,
                    type = type,
                    title = title,
                    message = message,
                    isPush = isPush,
                    timestamp = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )

                notificationDao.insertNotification(notification)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Notificación creada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al crear notificación: ${e.message}"
                )
                e.printStackTrace()
            }
        }
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
}