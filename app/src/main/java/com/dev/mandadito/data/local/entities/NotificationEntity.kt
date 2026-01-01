package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dev.mandadito.data.models.Notification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    @ColumnInfo(name = "is_push")
    val isPush: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toNotification(): Notification {
        return Notification(
            id = id.toString(),
            userId = userId,
            type = type,
            title = title,
            message = message,
            timestamp = java.time.Instant.ofEpochMilli(timestamp).toString(),
            isRead = isRead,
            isPush = isPush
        )
    }
}