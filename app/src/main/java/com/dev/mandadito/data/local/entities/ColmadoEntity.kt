package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dev.mandadito.data.models.Colmado
import com.dev.mandadito.data.models.ColmadoWithOwner

/**
 * Entidad local para Colmado (con información del owner)
 */
@Entity(
    tableName = "colmados",
    indices = [
        Index(value = ["seller_id"]),
        Index(value = ["name"]),
        Index(value = ["is_active"])
    ]
)
data class ColmadoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "seller_id")
    val sellerId: String,

    val name: String,
    val address: String,
    val phone: String,
    val description: String?,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    // Información del owner (desnormalizada para caché)
    @ColumnInfo(name = "owner_name")
    val ownerName: String?,

    @ColumnInfo(name = "owner_email")
    val ownerEmail: String?,

    @ColumnInfo(name = "owner_activo")
    val ownerActivo: Boolean?,

    @ColumnInfo(name = "total_users")
    val totalUsers: Int?
)

/**
 * Mappers: Entity ↔ Model
 */
fun ColmadoEntity.toModel(): Colmado {
    return Colmado(
        id = id,
        sellerId = sellerId,
        name = name,
        address = address,
        phone = phone,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ColmadoEntity.toColmadoWithOwner(): ColmadoWithOwner {
    return ColmadoWithOwner(
        id = id,
        sellerId = sellerId,
        name = name,
        address = address,
        phone = phone,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ownerName = ownerName,
        ownerEmail = ownerEmail,
        ownerActivo = ownerActivo,
        totalUsers = totalUsers ?: 0
    )
}

fun Colmado.toEntity(): ColmadoEntity {
    return ColmadoEntity(
        id = id,
        sellerId = sellerId,
        name = name,
        address = address,
        phone = phone,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ownerName = null,
        ownerEmail = null,
        ownerActivo = null,
        totalUsers = null
    )
}

fun ColmadoWithOwner.toEntity(): ColmadoEntity {
    return ColmadoEntity(
        id = id,
        sellerId = sellerId,
        name = name,
        address = address,
        phone = phone,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ownerName = ownerName,
        ownerEmail = ownerEmail,
        ownerActivo = ownerActivo,
        totalUsers = totalUsers
    )
}
