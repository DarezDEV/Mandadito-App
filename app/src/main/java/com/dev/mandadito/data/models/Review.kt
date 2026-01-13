package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String = "",
    @SerialName("product_id")
    val productId: String,
    @SerialName("user_id")
    val userId: String,
    val rating: Int, // 1-5
    val title: String? = null,
    val comment: String? = null,
    @SerialName("is_verified_purchase")
    val isVerifiedPurchase: Boolean = false,
    @SerialName("helpful_count")
    val helpfulCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class ReviewWithUser(
    val id: String = "",
    @SerialName("product_id")
    val productId: String,
    @SerialName("user_id")
    val userId: String,
    val rating: Int,
    val title: String? = null,
    val comment: String? = null,
    @SerialName("is_verified_purchase")
    val isVerifiedPurchase: Boolean = false,
    @SerialName("helpful_count")
    val helpfulCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    @SerialName("user_name")
    val userName: String = "",
    @SerialName("user_email")
    val userEmail: String = ""
)

// ✅ CORREGIDO: Cambiar Map<Int, Int> a Map<String, Int>
@Serializable
data class ReviewStats(
    @SerialName("average_rating")
    val averageRating: Double = 0.0,
    @SerialName("total_reviews")
    val totalReviews: Int = 0,
    @SerialName("rating_distribution")
    val ratingDistribution: Map<String, Int> = emptyMap() // "1"-"5" stars -> count
)

@Serializable
data class CreateReviewRequest(
    @SerialName("product_id")
    val productId: String,
    val rating: Int,
    val title: String? = null,
    val comment: String? = null
)

@Serializable
data class UpdateReviewRequest(
    val rating: Int? = null,
    val title: String? = null,
    val comment: String? = null
)