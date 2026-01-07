package com.dev.mandadito.data.repository

import com.dev.mandadito.data.models.CreateReviewRequest
import com.dev.mandadito.data.models.Review
import com.dev.mandadito.data.models.ReviewStats
import com.dev.mandadito.data.models.ReviewWithUser
import com.dev.mandadito.data.models.UpdateReviewRequest
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import android.util.Log

class ReviewRepository {
    private val supabase = SupabaseClient.client
    private val TAG = "ReviewRepository"

    /**
     * Obtener todas las reseñas de un producto con paginación
     */
    suspend fun getProductReviews(
        productId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<ReviewWithUser>> {
        return try {
            Log.d(TAG, "Fetching reviews for product: $productId")

            // Primero obtener las reviews
            val reviews = supabase.from("product_reviews")
                .select {
                    filter {
                        eq("product_id", productId)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<Review>()

            Log.d(TAG, "Found ${reviews.size} reviews")

            // Luego obtener los datos del usuario para cada review
            val reviewsWithUser = reviews.map { review ->
                val profile = try {
                    supabase.from("profiles")
                        .select(columns = Columns.list("full_name", "email")) {
                            filter {
                                eq("id", review.userId)
                            }
                        }
                        .decodeSingleOrNull<Map<String, String?>>()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching profile for user ${review.userId}: ${e.message}")
                    null
                }

                ReviewWithUser(
                    id = review.id,
                    productId = review.productId,
                    userId = review.userId,
                    rating = review.rating,
                    title = review.title,
                    comment = review.comment,
                    isVerifiedPurchase = review.isVerifiedPurchase,
                    helpfulCount = review.helpfulCount,
                    createdAt = review.createdAt,
                    updatedAt = review.updatedAt,
                    userName = profile?.get("full_name") ?: "Usuario",
                    userEmail = profile?.get("email") ?: ""
                )
            }

            Log.d(TAG, "Successfully mapped ${reviewsWithUser.size} reviews with user data")
            Result.success(reviewsWithUser)

        } catch (e: Exception) {
            Log.e(TAG, "Error in getProductReviews: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener estadísticas agregadas de reseñas de un producto
     */
    suspend fun getReviewStats(productId: String): Result<ReviewStats> {
        return try {
            Log.d(TAG, "Fetching review stats for product: $productId")

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

            Log.d(TAG, "Stats: Total=$totalReviews, Average=$averageRating")

            Result.success(
                ReviewStats(
                    averageRating = averageRating,
                    totalReviews = totalReviews,
                    ratingDistribution = distribution
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getReviewStats: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Crear una nueva reseña
     */
    suspend fun createReview(request: CreateReviewRequest, userId: String): Result<Review> {
        return try {
            Log.d(TAG, "Creating review for product: ${request.productId}, user: $userId")

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

            Log.d(TAG, "Review created successfully: ${review.id}")
            Result.success(review)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating review: ${e.message}", e)
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
            Log.d(TAG, "Updating review: $reviewId")

            @Serializable
            data class ReviewUpdate(
                val rating: Int? = null,
                val title: String? = null,
                val comment: String? = null
            )

            val update = ReviewUpdate(
                rating = request.rating?.takeIf { it in 1..5 },
                title = request.title,
                comment = request.comment
            )

            val review = supabase.from("product_reviews")
                .update(update) {
                    filter {
                        eq("id", reviewId)
                        eq("user_id", userId)
                    }
                    select()
                }
                .decodeSingle<Review>()

            Log.d(TAG, "Review updated successfully")
            Result.success(review)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating review: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Eliminar una reseña
     */
    suspend fun deleteReview(reviewId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting review: $reviewId")

            supabase.from("product_reviews")
                .delete {
                    filter {
                        eq("id", reviewId)
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Review deleted successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting review: ${e.message}", e)
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
            Log.d(TAG, "Checking if user $userId has reviewed product $productId")

            val review = supabase.from("product_reviews")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<Review>()

            Log.d(TAG, "User review: ${if (review != null) "Found" else "Not found"}")
            Result.success(review)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking user review: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Incrementar el contador de "útil" de una reseña
     */
    suspend fun markReviewAsHelpful(reviewId: String): Result<Unit> {
        return try {
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
            Log.e(TAG, "Error marking review as helpful: ${e.message}", e)
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

            Log.d(TAG, "Fetching reviews with rating $rating for product: $productId")

            // Primero obtener las reviews filtradas
            val reviews = supabase.from("product_reviews")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("rating", rating)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<Review>()

            // Luego obtener los datos del usuario para cada review
            val reviewsWithUser = reviews.map { review ->
                val profile = try {
                    supabase.from("profiles")
                        .select(columns = Columns.list("full_name", "email")) {
                            filter {
                                eq("id", review.userId)
                            }
                        }
                        .decodeSingleOrNull<Map<String, String?>>()
                } catch (e: Exception) {
                    null
                }

                ReviewWithUser(
                    id = review.id,
                    productId = review.productId,
                    userId = review.userId,
                    rating = review.rating,
                    title = review.title,
                    comment = review.comment,
                    isVerifiedPurchase = review.isVerifiedPurchase,
                    helpfulCount = review.helpfulCount,
                    createdAt = review.createdAt,
                    updatedAt = review.updatedAt,
                    userName = profile?.get("full_name") ?: "Usuario",
                    userEmail = profile?.get("email") ?: ""
                )
            }

            Result.success(reviewsWithUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reviews by rating: ${e.message}", e)
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
            // Primero obtener las reviews ordenadas por helpful_count
            val reviews = supabase.from("product_reviews")
                .select {
                    filter {
                        eq("product_id", productId)
                    }
                    order(column = "helpful_count", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<Review>()

            // Luego obtener los datos del usuario para cada review
            val reviewsWithUser = reviews.map { review ->
                val profile = try {
                    supabase.from("profiles")
                        .select(columns = Columns.list("full_name", "email")) {
                            filter {
                                eq("id", review.userId)
                            }
                        }
                        .decodeSingleOrNull<Map<String, String?>>()
                } catch (e: Exception) {
                    null
                }

                ReviewWithUser(
                    id = review.id,
                    productId = review.productId,
                    userId = review.userId,
                    rating = review.rating,
                    title = review.title,
                    comment = review.comment,
                    isVerifiedPurchase = review.isVerifiedPurchase,
                    helpfulCount = review.helpfulCount,
                    createdAt = review.createdAt,
                    updatedAt = review.updatedAt,
                    userName = profile?.get("full_name") ?: "Usuario",
                    userEmail = profile?.get("email") ?: ""
                )
            }

            Result.success(reviewsWithUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting most helpful reviews: ${e.message}", e)
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