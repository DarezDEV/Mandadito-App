package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateColmadoDto(
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null
)