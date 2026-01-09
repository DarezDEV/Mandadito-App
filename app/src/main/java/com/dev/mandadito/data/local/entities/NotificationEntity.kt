package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dev.mandadito.data.models.Notification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "supabase_id")
    val supabaseId: String? = null, // UUID de Supabase

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
            id = supabaseId ?: id.toString(), // Usar supabaseId si existe, sino Room ID como fallback
            userId = userId,
            type = type,
            title = title,
            message = message,
            isRead = isRead,
            isPush = isPush,
            createdAt = java.time.Instant.ofEpochMilli(createdAt).toString()
        )
    }

    // ID local de Room para operaciones internas
    fun getRoomId(): Long = id
}