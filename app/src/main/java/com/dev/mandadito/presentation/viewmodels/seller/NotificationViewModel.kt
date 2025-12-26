package com.dev.mandadito.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.Notification
import com.dev.mandadito.data.repository.NotificationRepository
import com.dev.mandadito.utils.SharedPreferenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val filterType: String? = null, // null = todas, "LOW_STOCK", "OUT_OF_STOCK", etc.
    val showOnlyUnread: Boolean = false
)

class NotificationViewModel(context: Context) : ViewModel() {

    private val repository = NotificationRepository()
    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val TAG = "NotificationViewModel"

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val userId = sharedPrefsHelper.getUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Usuario no identificado"
                    )
                }
                return@launch
            }

            Log.d(TAG, "🔥 Cargando notificaciones para usuario: $userId")

            when (val result = repository.getUserNotifications(userId)) {
                is NotificationRepository.Result.Success -> {
                    Log.d(TAG, "✅ ${result.data.size} notificaciones cargadas")

                    // Obtener contador de no leídas
                    val unreadCount = result.data.count { !it.isRead }

                    _uiState.update {
                        it.copy(
                            notifications = result.data,
                            unreadCount = unreadCount,
                            isLoading = false
                        )
                    }
                }
                is NotificationRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error cargando notificaciones: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            Log.d(TAG, "✓ Marcando como leída: $notificationId")

            when (val result = repository.markAsRead(notificationId)) {
                is NotificationRepository.Result.Success -> {
                    // Actualizar localmente
                    _uiState.update { currentState ->
                        currentState.copy(
                            notifications = currentState.notifications.map { notification ->
                                if (notification.id == notificationId) {
                                    notification.copy(isRead = true)
                                } else {
                                    notification
                                }
                            },
                            unreadCount = maxOf(0, currentState.unreadCount - 1)
                        )
                    }
                    Log.d(TAG, "✅ Notificación marcada como leída")
                }
                is NotificationRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = sharedPrefsHelper.getUserId() ?: return@launch

            Log.d(TAG, "✓✓ Marcando todas como leídas")

            when (val result = repository.markAllAsRead(userId)) {
                is NotificationRepository.Result.Success -> {
                    _uiState.update { currentState ->
                        currentState.copy(
                            notifications = currentState.notifications.map { it.copy(isRead = true) },
                            unreadCount = 0,
                            successMessage = "Todas las notificaciones marcadas como leídas"
                        )
                    }
                    Log.d(TAG, "✅ Todas marcadas como leídas")
                }
                is NotificationRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            Log.d(TAG, "🗑️ Eliminando notificación: $notificationId")

            when (val result = repository.deleteNotification(notificationId)) {
                is NotificationRepository.Result.Success -> {
                    _uiState.update { currentState ->
                        val deletedNotification = currentState.notifications.find { it.id == notificationId }
                        currentState.copy(
                            notifications = currentState.notifications.filterNot { it.id == notificationId },
                            unreadCount = if (deletedNotification?.isRead == false) {
                                maxOf(0, currentState.unreadCount - 1)
                            } else {
                                currentState.unreadCount
                            },
                            successMessage = "Notificación eliminada"
                        )
                    }
                    Log.d(TAG, "✅ Notificación eliminada")
                }
                is NotificationRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun deleteReadNotifications() {
        viewModelScope.launch {
            val userId = sharedPrefsHelper.getUserId() ?: return@launch

            Log.d(TAG, "🗑️ Eliminando todas las leídas")

            when (val result = repository.deleteReadNotifications(userId)) {
                is NotificationRepository.Result.Success -> {
                    _uiState.update { currentState ->
                        currentState.copy(
                            notifications = currentState.notifications.filter { !it.isRead },
                            successMessage = "Notificaciones leídas eliminadas"
                        )
                    }
                    Log.d(TAG, "✅ Notificaciones leídas eliminadas")
                }
                is NotificationRepository.Result.Error -> {
                    Log.e(TAG, "❌ Error: ${result.message}")
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun setFilterType(type: String?) {
        Log.d(TAG, "🎭 Filtro de tipo: ${type ?: "Todas"}")
        _uiState.update { it.copy(filterType = type) }
    }

    fun setShowOnlyUnread(show: Boolean) {
        Log.d(TAG, "👁️ Mostrar solo no leídas: $show")
        _uiState.update { it.copy(showOnlyUnread = show) }
    }

    val filteredNotifications: List<Notification>
        get() {
            val notifications = _uiState.value.notifications

            var filtered = notifications

            // Filtrar por tipo
            _uiState.value.filterType?.let { type ->
                filtered = filtered.filter { it.type == type }
            }

            // Filtrar por leídas/no leídas
            if (_uiState.value.showOnlyUnread) {
                filtered = filtered.filter { !it.isRead }
            }

            return filtered
        }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}