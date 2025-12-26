package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dev.mandadito.data.models.Category

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["colmado_id"]),
        Index(value = ["name"]),
        Index(value = ["is_active"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "colmado_id")
    val colmadoId: String,

    val name: String,
    val description: String?,
    val icon: String?,
    val color: String?,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

/**
 * Mappers: Entity ↔ Model
 */
fun CategoryEntity.toModel(): Category {
    return Category(
        id = id,
        colmadoId = colmadoId,
        name = name,
        description = description,
        icon = icon,
        color = color,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        colmadoId = colmadoId,
        name = name,
        description = description,
        icon = icon,
        color = color,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
