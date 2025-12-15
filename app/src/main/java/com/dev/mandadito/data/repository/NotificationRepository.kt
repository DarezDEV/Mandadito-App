package com.dev.mandadito.data.repository

import com.dev.mandadito.data.models.Notification

class NotificationRepository {

    suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            Result.success(0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserNotifications(userId: String, limit: Int = 10): Result<List<Notification>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}