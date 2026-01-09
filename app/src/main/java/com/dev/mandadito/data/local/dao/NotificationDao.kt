package com.dev.mandadito.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dev.mandadito.data.local.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    fun getAllNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :notificationId LIMIT 1")
    suspend fun getNotificationById(notificationId: Long): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE supabase_id = :supabaseId LIMIT 1")
    suspend fun getNotificationBySupabaseId(supabaseId: String): NotificationEntity?

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    fun getUnreadCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("UPDATE notifications SET is_read = 1 WHERE supabase_id = :supabaseId")
    suspend fun markAsReadBySupabaseId(supabaseId: String)

    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId AND is_read = 0")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteById(notificationId: Long)

    @Query("DELETE FROM notifications WHERE user_id = :userId AND is_read = 1")
    suspend fun deleteReadNotifications(userId: String)

    @Query("DELETE FROM notifications WHERE user_id = :userId AND created_at < :timestamp")
    suspend fun deleteOldNotifications(userId: String, timestamp: Long)

    @Query("DELETE FROM notifications WHERE user_id = :userId")
    suspend fun deleteAllNotifications(userId: String)
}