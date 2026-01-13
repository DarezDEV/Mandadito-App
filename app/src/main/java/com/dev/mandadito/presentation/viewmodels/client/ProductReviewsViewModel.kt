package com.dev.mandadito.presentation.viewmodels.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewsUiState(
    val reviews: List<ReviewWithUser> = emptyList(),
    val stats: ReviewStats = ReviewStats(),
    val userReview: Review? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedRatingFilter: Int? = null,
    val successMessage: String? = null
)

class ProductReviewsViewModel : ViewModel() {
    private val repository = ReviewRepository()

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    fun loadReviews(productId: String, userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Cargar estadísticas
                val statsResult = repository.getReviewStats(productId)
                val stats = statsResult.getOrNull() ?: ReviewStats()

                // Cargar reseñas
                val reviewsResult = repository.getProductReviews(productId)
                val reviews = reviewsResult.getOrNull() ?: emptyList()

                // Cargar reseña del usuario si está autenticado
                var userReview: Review? = null
                if (userId != null) {
                    val userReviewResult = repository.getUserReviewForProduct(productId, userId)
                    userReview = userReviewResult.getOrNull()
                }

                _uiState.value = _uiState.value.copy(
                    reviews = reviews,
                    stats = stats,
                    userReview = userReview,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar las reseñas"
                )
            }
        }
    }

    fun createReview(
        productId: String,
        userId: String,
        rating: Int,
        title: String?,
        comment: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val request = CreateReviewRequest(
                productId = productId,
                rating = rating,
                title = title,
                comment = comment
            )

            val result = repository.createReview(request, userId)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Reseña publicada exitosamente"
                )
                loadReviews(productId, userId)
                onSuccess()
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Error al crear la reseña"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                onError(errorMessage)
            }
        }
    }

    fun updateReview(
        reviewId: String,
        productId: String,
        userId: String,
        rating: Int?,
        title: String?,
        comment: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val request = UpdateReviewRequest(
                rating = rating,
                title = title,
                comment = comment
            )

            val result = repository.updateReview(reviewId, userId, request)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Reseña actualizada exitosamente"
                )
                loadReviews(productId, userId)
                onSuccess()
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Error al actualizar la reseña"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                onError(errorMessage)
            }
        }
    }

    fun deleteReview(
        reviewId: String,
        productId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.deleteReview(reviewId, userId)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Reseña eliminada exitosamente"
                )
                loadReviews(productId, userId)
                onSuccess()
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Error al eliminar la reseña"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                onError(errorMessage)
            }
        }
    }

    fun markAsHelpful(reviewId: String, productId: String, userId: String?) {
        viewModelScope.launch {
            val result = repository.markReviewAsHelpful(reviewId)
            if (result.isSuccess) {
                loadReviews(productId, userId)
            }
        }
    }

    fun filterByRating(productId: String, rating: Int?, userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                selectedRatingFilter = rating
            )

            try {
                val reviewsResult = if (rating != null) {
                    repository.getReviewsByRating(productId, rating)
                } else {
                    repository.getProductReviews(productId)
                }

                val reviews = reviewsResult.getOrNull() ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    reviews = reviews,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al filtrar reseñas"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}