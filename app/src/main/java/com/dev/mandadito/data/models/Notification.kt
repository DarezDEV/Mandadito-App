package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    val type: String = "INFO",
    val title: String = "",
    val message: String = "",
    val timestamp: String = "", // Mantener como String para ISO 8601
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("is_push")
    val isPush: Boolean = false,
    val metadata: Map<String, String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null // Timestamp de creación de Supabase
)