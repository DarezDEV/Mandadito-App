package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dev.mandadito.data.models.DeliveryUser

/**
 * Entidad local para DeliveryUser
 */
@Entity(
    tableName = "delivery_users",
    indices = [
        Index(value = ["colmado_id"]),
        Index(value = ["email"]),
        Index(value = ["activo"])
    ]
)
data class DeliveryUserEntity(
    @PrimaryKey
    val id: String,

    val email: String,
    val nombre: String,
    val activo: Boolean,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "colmado_id")
    val colmadoId: String?,

    @ColumnInfo(name = "role_in_colmado")
    val roleInColmado: String?
)

/**
 * Mappers: Entity ↔ Model
 */
fun DeliveryUserEntity.toModel(): DeliveryUser {
    return DeliveryUser(
        id = id,
        email = email,
        nombre = nombre,
        activo = activo,
        avatar_url = avatarUrl,
        colmado_id = colmadoId,
        role_in_colmado = roleInColmado
    )
}

fun DeliveryUser.toEntity(): DeliveryUserEntity {
    return DeliveryUserEntity(
        id = id,
        email = email,
        nombre = nombre,
        activo = activo,
        avatarUrl = avatar_url,
        colmadoId = colmado_id,
        roleInColmado = role_in_colmado
    )
}
