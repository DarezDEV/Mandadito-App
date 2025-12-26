package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("type") val type: String = "INFO",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_push") val isPush: Boolean = false,
    @SerialName("timestamp") val timestamp: String = "",
    @SerialName("action_url") val actionUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)