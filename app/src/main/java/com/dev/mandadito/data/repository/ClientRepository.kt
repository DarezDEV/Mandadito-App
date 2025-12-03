package com.dev.mandadito.data.repository

import android.util.Log
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository para operaciones de cliente
 * Version simplificada que funciona con Supabase
 */
object ClientRepository {
    private val supabase = SupabaseClient.client
    private const val TAG = "ClientRepository"

    // ========================================
    // COLMADOS
    // ========================================

    /**
     * Obtiene todos los colmados activos
     */
    suspend fun getColmados(): List<Colmado> {
        return try {
            Log.d(TAG, "Obteniendo colmados...")
            val result = supabase.from("colmados").select().decodeList<Colmado>()
            Log.d(TAG, "✅ Colmados obtenidos: ${result.size}")
            result.filter { it.isActive } // Filtrar en Kotlin
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting colmados: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene un colmado por su ID
     */
    suspend fun getColmadoById(id: String): Colmado? {
        return try {
            Log.d(TAG, "Obteniendo colmado con id: $id")
            val result = supabase.from("colmados").select().decodeList<Colmado>()
            val colmado = result.firstOrNull { it.id == id }
            if (colmado != null) {
                Log.d(TAG, "✅ Colmado obtenido: ${colmado.name}")
            }
            colmado
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting colmado: ${e.message}", e)
            null
        }
    }

    /**
     * Busca colmados por nombre
     */
    suspend fun searchColmados(query: String): List<Colmado> {
        return try {
            Log.d(TAG, "Buscando colmados con query: $query")
            val result = supabase.from("colmados").select().decodeList<Colmado>()
            val filtered = result.filter {
                it.name.contains(query, ignoreCase = true) && it.isActive
            }
            Log.d(TAG, "✅ Colmados encontrados: ${filtered.size}")
            filtered
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error searching colmados: ${e.message}", e)
            emptyList()
        }
    }

    // ========================================
    // PRODUCTOS
    // ========================================

    /**
     * Obtiene todos los productos de un colmado específico
     */
    suspend fun getProductsByColmado(colmadoId: String): List<Product> {
        return try {
            Log.d(TAG, "Obteniendo productos del colmado: $colmadoId")
            val result = supabase.from("products").select().decodeList<Product>()
            val filtered = result.filter {
                it.colmadoId == colmadoId && it.isActive
            }
            Log.d(TAG, "✅ Productos obtenidos: ${filtered.size}")
            filtered
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting products: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene un producto por su ID
     */
    suspend fun getProductById(productId: String): Product? {
        return try {
            Log.d(TAG, "Obteniendo producto con id: $productId")
            val result = supabase.from("products").select().decodeList<Product>()
            val product = result.firstOrNull { it.id == productId }
            if (product != null) {
                Log.d(TAG, "✅ Producto obtenido: ${product.name}")
            }
            product
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting product: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene productos con sus categorías
     */
    suspend fun getProductsWithCategories(colmadoId: String): List<ProductWithCategories> {
        return try {
            Log.d(TAG, "Obteniendo productos con categorías del colmado: $colmadoId")
            val result = supabase.from("products")
                .select(
                    columns = Columns.raw("""
                        *,
                        product_categories(
                            categories(*)
                        ),
                        product_images(*)
                    """)
                )
                .decodeList<ProductWithCategories>()
            val filtered = result.filter { it.isActive }
            Log.d(TAG, "✅ Productos con categorías obtenidos: ${filtered.size}")
            filtered
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting products with categories: ${e.message}", e)
            emptyList()
        }
    }

    // ========================================
    // CATEGORÍAS
    // ========================================

    /**
     * Obtiene todas las categorías activas
     */
    suspend fun getCategories(): List<Category> {
        return try {
            Log.d(TAG, "Obteniendo categorías...")
            val result = supabase.from("categories").select().decodeList<Category>()
            val filtered = result.filter { it.isActive }
            Log.d(TAG, "✅ Categorías obtenidas: ${filtered.size}")
            filtered
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting categories: ${e.message}", e)
            emptyList()
        }
    }

    // ========================================
    // PEDIDOS
    // ========================================

    /**
     * Crea un nuevo pedido
     */
    suspend fun createPedido(pedido: CreatePedidoRequest): String? {
        return try {
            Log.d(TAG, "Creando pedido...")
            val result = supabase.from("pedidos")
                .insert(pedido)
                .decodeSingle<PedidoResponse>()
            Log.d(TAG, "✅ Pedido creado con ID: ${result.id}")
            result.id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating pedido: ${e.message}", e)
            null
        }
    }

    /**
     * Crea los items de un pedido
     */
    suspend fun createPedidoItems(items: List<CreatePedidoItemRequest>): Boolean {
        return try {
            Log.d(TAG, "Creando items del pedido...")
            supabase.from("pedido_items").insert(items)
            Log.d(TAG, "✅ Items del pedido creados: ${items.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating pedido items: ${e.message}", e)
            false
        }
    }

    /**
     * Obtiene todos los pedidos de un usuario
     */
    suspend fun getUserPedidos(userId: String): List<PedidoResponse> {
        return try {
            Log.d(TAG, "Obteniendo pedidos del usuario: $userId")
            val result = supabase.from("pedidos").select().decodeList<PedidoResponse>()
            val filtered = result.filter { it.clienteId == userId }
                .sortedByDescending { it.createdAt }
            Log.d(TAG, "✅ Pedidos obtenidos: ${filtered.size}")
            filtered
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user pedidos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene un pedido con todos sus detalles
     */
    suspend fun getPedidoWithDetails(pedidoId: String): PedidoWithDetails? {
        return try {
            Log.d(TAG, "Obteniendo detalles del pedido: $pedidoId")
            val result = supabase.from("pedidos")
                .select(
                    columns = Columns.raw("""
                        *,
                        pedido_items(*),
                        colmados(id, name, address, phone)
                    """)
                )
                .decodeList<PedidoWithDetails>()
            val pedido = result.firstOrNull { it.id == pedidoId }
            if (pedido != null) {
                Log.d(TAG, "✅ Detalles del pedido obtenidos")
            }
            pedido
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting pedido with details: ${e.message}", e)
            null
        }
    }
}

// ========================================
// DTOs (Data Transfer Objects)
// ========================================

/**
 * DTO para crear un nuevo pedido
 */
@Serializable
data class CreatePedidoRequest(
    @SerialName("cliente_id") val clienteId: String,
    @SerialName("colmado_id") val colmadoId: String,
    val total: Double,
    val estado: String = "pendiente",
    @SerialName("direccion_entrega") val direccionEntrega: String,
    @SerialName("telefono_contacto") val telefonoContacto: String? = null,
    val notas: String? = null
)

/**
 * DTO para crear items de un pedido
 */
@Serializable
data class CreatePedidoItemRequest(
    @SerialName("pedido_id") val pedidoId: String,
    @SerialName("producto_id") val productoId: String,
    val nombre: String,
    val cantidad: Int,
    @SerialName("precio_unitario") val precioUnitario: Double,
    val subtotal: Double
)

/**
 * Respuesta de la base de datos al crear un pedido
 */
@Serializable
data class PedidoResponse(
    val id: String,
    @SerialName("cliente_id") val clienteId: String,
    @SerialName("colmado_id") val colmadoId: String,
    val total: Double,
    val estado: String,
    @SerialName("direccion_entrega") val direccionEntrega: String,
    @SerialName("telefono_contacto") val telefonoContacto: String? = null,
    val notas: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

/**
 * Pedido con todos sus detalles (items y colmado)
 */
@Serializable
data class PedidoWithDetails(
    val id: String,
    @SerialName("cliente_id") val clienteId: String,
    @SerialName("colmado_id") val colmadoId: String,
    val total: Double,
    val estado: String,
    @SerialName("direccion_entrega") val direccionEntrega: String,
    @SerialName("telefono_contacto") val telefonoContacto: String?,
    val notas: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("pedido_items") val items: List<PedidoItemResponse>,
    val colmados: ColmadoInfo
)

/**
 * Información básica de un colmado (para joins)
 */
@Serializable
data class ColmadoInfo(
    val id: String,
    val name: String,
    val address: String,
    val phone: String
)

/**
 * Item de un pedido en la respuesta
 */
@Serializable
data class PedidoItemResponse(
    val id: String,
    @SerialName("pedido_id") val pedidoId: String,
    @SerialName("producto_id") val productoId: String,
    val nombre: String,
    val cantidad: Int,
    @SerialName("precio_unitario") val precioUnitario: Double,
    val subtotal: Double
)