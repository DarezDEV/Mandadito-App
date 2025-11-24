package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserRole(
    val user_id: String,
    val role_id: Int
)

// Enum para los roles disponibles
@Serializable
enum class Role(val value: String) {
    @SerialName("client")
    CLIENT("client"),
    @SerialName("seller")
    SELLER("seller"),
    @SerialName("delivery")
    DELIVERY("delivery"),
    @SerialName("admin")
    ADMIN("admin");

    companion object {
        fun fromString(value: String): Role? {
            return values().find { it.value == value }
        }
    }
}