package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.data.models.ProductWithCategories

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["colmado_id"]),
        Index(value = ["name"]),
        Index(value = ["is_active"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "colmado_id")
    val colmadoId: String,

    val name: String,
    val description: String?,
    val price: Double,
    val stock: Int,

    @ColumnInfo(name = "min_stock")
    val minStock: Int,

    @ColumnInfo(name = "image_urls")
    val imageUrls: List<String>,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

/**
 * Relación many-to-many entre Product y Category
 */
@Entity(
    tableName = "product_category_cross_ref",
    primaryKeys = ["productId", "categoryId"]
)
data class ProductCategoryCrossRef(
    val productId: String,
    val categoryId: String
)

/**
 * Clase para obtener productos con sus categorías (JOIN)
 */
data class ProductWithCategoriesEntity(
    @Embedded val product: ProductEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ProductCategoryCrossRef::class,
            parentColumn = "productId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<CategoryEntity>
)

/**
 * Mappers: Entity ↔ Model
 */
fun ProductEntity.toModel(categories: List<Category>): ProductWithCategories {
    return ProductWithCategories(
        id = id,
        colmadoId = colmadoId,
        name = name,
        description = description,
        price = price,
        stock = stock,
        minStock = minStock,
        // imageUrls = imageUrls,  // ← ELIMINA O COMENTA esta línea// imageUrls = imageUrls,  // ← ELIMINA O COMENTA esta línea
        imageUrl = imageUrl,
        images = emptyList(), // Las imágenes con metadata se cargan aparte si es necesario
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        categories = categories
    )
}

fun ProductWithCategories.toEntity(): ProductEntity {
    return ProductEntity(
        id = id,
        colmadoId = colmadoId,
        name = name,
        description = description,
        price = price,
        stock = stock,
        minStock = minStock,
        imageUrls = allImageUrls,  // ← USA EL HELPER que ya existe en ProductWithCategories
        imageUrl = imageUrl,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
