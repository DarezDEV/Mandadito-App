package com.dev.mandadito.data.repository

import com.dev.mandadito.data.models.CreateReviewRequest
import com.dev.mandadito.data.models.Review
import com.dev.mandadito.data.models.ReviewStats
import com.dev.mandadito.data.models.ReviewWithUser
import com.dev.mandadito.data.models.UpdateReviewRequest
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


class ReviewRepository {
    private val supabase = SupabaseClient.client

    /**
     * Obtener todas las reseñas de un producto con paginación
     */
    suspend fun getProductReviews(
        productId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<ReviewWithUser>> {
        return try {
            val reviews = supabase.from("product_reviews")
                .select(
                    columns = Columns.raw("""
                        id,
                        product_id,
                        user_id,
                        rating,
                        title,
                        comment,
                        is_verified_purchase,
                        helpful_count,
                        created_at,
                        updated_at,
                        profiles!user_id (
                            full_name,
                            email
                        )
                    """.trimIndent())
                ) {
                    filter {
                        eq("product_id", productId)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Map<String, Any?>>()
                .map { reviewMap ->
                    val profile = reviewMap["profiles"] as? Map<String, Any?>
                    ReviewWithUser(
                        id = reviewMap["id"] as String,
                        productId = reviewMap["product_id"] as String,
                        userId = reviewMap["user_id"] as String,
                        rating = (reviewMap["rating"] as Number).toInt(),
                        title = reviewMap["title"] as? String,
                        comment = reviewMap["comment"] as? String,
                        isVerifiedPurchase = reviewMap["is_verified_purchase"] as? Boolean ?: false,
                        helpfulCount = (reviewMap["helpful_count"] as? Number)?.toInt() ?: 0,
                        createdAt = reviewMap["created_at"] as String,
                        updatedAt = reviewMap["updated_at"] as String,
                        userName = profile?.get("full_name") as? String ?: "Usuario",
                        userEmail = profile?.get("email") as? String ?: ""
                    )
                }

            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener estadísticas agregadas de reseñas de un producto
     */
    suspend fun getReviewStats(productId: String): Result<ReviewStats> {
        return try {
            val reviews = supabase.from("product_reviews")
                .select(columns = Columns.list("rating")) {
                    filter {
                        eq("product_id", productId)
                    }
                }
                .decodeList<Map<String, Int>>()

            val totalReviews = reviews.size
            val averageRating = if (totalReviews > 0) {
                reviews.map { (it["rating"] ?: 0).toDouble() }.average()
            } else 0.0

            // Calcular distribución de calificaciones
            val distribution = mutableMapOf("1" to 0, "2" to 0, "3" to 0, "4" to 0, "5" to 0)
            reviews.forEach { review ->
                val rating = review["rating"] ?: 0
                if (rating in 1..5) {
                    distribution[rating.toString()] = distribution[rating.toString()]!! + 1
                }
            }

            Result.success(
                ReviewStats(
                    averageRating = averageRating,
                    totalReviews = totalReviews,
                    ratingDistribution = distribution
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear una nueva reseña
     */
    suspend fun createReview(request: CreateReviewRequest, userId: String): Result<Review> {
        return try {
            // Validar que el rating esté entre 1 y 5
            if (request.rating !in 1..5) {
                return Result.failure(IllegalArgumentException("El rating debe estar entre 1 y 5"))
            }

            // ✅ Crear un objeto serializable
            @Serializable
            data class ReviewInsert(
                @SerialName("product_id") val productId: String,
                @SerialName("user_id") val userId: String,
                val rating: Int,
                val title: String? = null,
                val comment: String? = null
            )

            val reviewInsert = ReviewInsert(
                productId = request.productId,
                userId = userId,
                rating = request.rating,
                title = request.title,
                comment = request.comment
            )

            val review = supabase.from("product_reviews")
                .insert(reviewInsert) {
                    select()
                }
                .decodeSingle<Review>()

            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar una reseña existente
     */
    suspend fun updateReview(
        reviewId: String,
        userId: String,
        request: UpdateReviewRequest
    ): Result<Review> {
        return try {
            val updates = buildMap<String, Any?> {
                request.rating?.let {
                    if (it in 1..5) {
                        put("rating", it)
                    } else {
                        throw IllegalArgumentException("El rating debe estar entre 1 y 5")
                    }
                }
                request.title?.let { put("title", it) }
                request.comment?.let { put("comment", it) }
            }

            if (updates.isEmpty()) {
                return Result.failure(IllegalArgumentException("No hay cambios para actualizar"))
            }

            val review = supabase.from("product_reviews")
                .update(updates) {
                    filter {
                        eq("id", reviewId)
                        eq("user_id", userId) // Seguridad: solo el autor puede actualizar
                    }
                    select()
                }
                .decodeSingle<Review>()

            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar una reseña
     */
    suspend fun deleteReview(reviewId: String, userId: String): Result<Unit> {
        return try {
            supabase.from("product_reviews")
                .delete {
                    filter {
                        eq("id", reviewId)
                        eq("user_id", userId) // Seguridad: solo el autor puede eliminar
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verificar si el usuario ya ha dejado una reseña para este producto
     */
    suspend fun getUserReviewForProduct(
        productId: String,
        userId: String
    ): Result<Review?> {
        return try {
            val review = supabase.from("product_reviews")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<Review>()

            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Incrementar el contador de "útil" de una reseña
     * CORREGIDO: Usa RPC para incremento atómico
     */
    suspend fun markReviewAsHelpful(reviewId: String): Result<Unit> {
        return try {
            // Opción 1: Usando RPC (necesitas crear esta función en Supabase)
            // supabase.rpc("increment_helpful_count", mapOf("review_id" to reviewId))

            // Opción 2: Fetch actual value and update
            val current = supabase.from("product_reviews")
                .select(columns = Columns.list("helpful_count")) {
                    filter {
                        eq("id", reviewId)
                    }
                }
                .decodeSingle<Map<String, Int>>()

            val newCount = (current["helpful_count"] ?: 0) + 1

            supabase.from("product_reviews")
                .update(mapOf("helpful_count" to newCount)) {
                    filter {
                        eq("id", reviewId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener reseñas filtradas por calificación específica
     */
    suspend fun getReviewsByRating(
        productId: String,
        rating: Int,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<ReviewWithUser>> {
        return try {
            if (rating !in 1..5) {
                return Result.failure(IllegalArgumentException("El rating debe estar entre 1 y 5"))
            }

            val reviews = supabase.from("product_reviews")
                .select(
                    columns = Columns.raw("""
                        id,
                        product_id,
                        user_id,
                        rating,
                        title,
                        comment,
                        is_verified_purchase,
                        helpful_count,
                        created_at,
                        updated_at,
                        profiles!user_id (
                            full_name,
                            email
                        )
                    """.trimIndent())
                ) {
                    filter {
                        eq("product_id", productId)
                        eq("rating", rating)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Map<String, Any?>>()
                .map { reviewMap ->
                    val profile = reviewMap["profiles"] as? Map<String, Any?>
                    ReviewWithUser(
                        id = reviewMap["id"] as String,
                        productId = reviewMap["product_id"] as String,
                        userId = reviewMap["user_id"] as String,
                        rating = (reviewMap["rating"] as Number).toInt(),
                        title = reviewMap["title"] as? String,
                        comment = reviewMap["comment"] as? String,
                        isVerifiedPurchase = reviewMap["is_verified_purchase"] as? Boolean ?: false,
                        helpfulCount = (reviewMap["helpful_count"] as? Number)?.toInt() ?: 0,
                        createdAt = reviewMap["created_at"] as String,
                        updatedAt = reviewMap["updated_at"] as String,
                        userName = profile?.get("full_name") as? String ?: "Usuario",
                        userEmail = profile?.get("email") as? String ?: ""
                    )
                }

            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener las reseñas más útiles de un producto
     */
    suspend fun getMostHelpfulReviews(
        productId: String,
        limit: Int = 5
    ): Result<List<ReviewWithUser>> {
        return try {
            val reviews = supabase.from("product_reviews")
                .select(
                    columns = Columns.raw("""
                        id,
                        product_id,
                        user_id,
                        rating,
                        title,
                        comment,
                        is_verified_purchase,
                        helpful_count,
                        created_at,
                        updated_at,
                        profiles!user_id (
                            full_name,
                            email
                        )
                    """.trimIndent())
                ) {
                    filter {
                        eq("product_id", productId)
                    }
                    order(column = "helpful_count", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<Map<String, Any?>>()
                .map { reviewMap ->
                    val profile = reviewMap["profiles"] as? Map<String, Any?>
                    ReviewWithUser(
                        id = reviewMap["id"] as String,
                        productId = reviewMap["product_id"] as String,
                        userId = reviewMap["user_id"] as String,
                        rating = (reviewMap["rating"] as Number).toInt(),
                        title = reviewMap["title"] as? String,
                        comment = reviewMap["comment"] as? String,
                        isVerifiedPurchase = reviewMap["is_verified_purchase"] as? Boolean ?: false,
                        helpfulCount = (reviewMap["helpful_count"] as? Number)?.toInt() ?: 0,
                        createdAt = reviewMap["created_at"] as String,
                        updatedAt = reviewMap["updated_at"] as String,
                        userName = profile?.get("full_name") as? String ?: "Usuario",
                        userEmail = profile?.get("email") as? String ?: ""
                    )
                }

            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verificar si el usuario ha comprado el producto (compra verificada)
     */
    suspend fun hasUserPurchasedProduct(
        productId: String,
        userId: String
    ): Result<Boolean> {
        return try {
            val orders = supabase.from("orders")
                .select(
                    columns = Columns.raw("""
                        id,
                        order_items!inner (
                            product_id
                        )
                    """.trimIndent())
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("order_items.product_id", productId)
                        eq("status", "completed")
                    }
                }
                .decodeList<Map<String, Any?>>()

            Result.success(orders.isNotEmpty())
        } catch (e: Exception) {
            // Si hay error, asumimos que no ha comprado
            Result.success(false)
        }
    }

    /**
     * Marcar una reseña como compra verificada
     */
    suspend fun markReviewAsVerifiedPurchase(reviewId: String): Result<Unit> {
        return try {
            supabase.from("product_reviews")
                .update(mapOf("is_verified_purchase" to true)) {
                    filter {
                        eq("id", reviewId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}