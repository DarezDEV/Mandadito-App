package com.dev.mandadito.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla para trackear cuándo se guardó cada tipo de dato en caché
 * Permite invalidar caché basado en tiempo
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey
    val key: String, // Ej: "products_colmado_123", "categories_colmado_123"

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // System.currentTimeMillis()

    @ColumnInfo(name = "data_type")
    val dataType: String, // "products", "categories", "colmados", etc.

    @ColumnInfo(name = "related_id")
    val relatedId: String? = null // ID del colmado, usuario, etc.
)

/**
 * Estrategia de expiración de caché
 */
object CacheStrategy {

    // Tiempos de vida del caché (en milisegundos)
    const val PRODUCTS_TTL = 5 * 60 * 1000L          // 5 minutos
    const val CATEGORIES_TTL = 10 * 60 * 1000L       // 10 minutos
    const val COLMADOS_TTL = 15 * 60 * 1000L         // 15 minutos
    const val CART_TTL = 2 * 60 * 1000L              // 2 minutos (más corto porque cambia frecuentemente)

    /**
     * Verifica si el caché es válido
     */
    fun isValid(metadata: CacheMetadata?, ttl: Long): Boolean {
        if (metadata == null) return false
        val now = System.currentTimeMillis()
        return (now - metadata.timestamp) < ttl
    }

    /**
     * Genera una key única para el caché
     */
    fun generateKey(dataType: String, relatedId: String? = null): String {
        return if (relatedId != null) {
            "${dataType}_${relatedId}"
        } else {
            dataType
        }
    }
}
