package com.dev.mandadito.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.ReviewStats

/**
 * Componente para mostrar el resumen completo de reseñas
 * Incluye promedio, total y distribución
 */
@Composable
fun ReviewSummary(
    stats: ReviewStats,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null,
    onRatingFilterClick: ((Int) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reseñas de clientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (onSeeAllClick != null && stats.totalReviews > 0) {
                    TextButton(onClick = onSeeAllClick) {
                        Text("Ver todas")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (stats.totalReviews > 0) {
                // Promedio y distribución
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Promedio
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = String.format("%.1f", stats.averageRating),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        RatingBar(
                            rating = stats.averageRating,
                            starSize = 22.dp,
                            showRatingNumber = false
                        )
                        Text(
                            text = "${stats.totalReviews} reseña${if (stats.totalReviews != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Distribución
                    RatingDistribution(
                        distribution = stats.ratingDistribution,
                        totalReviews = stats.totalReviews,
                        modifier = Modifier.weight(2f),
                        onRatingClick = onRatingFilterClick
                    )
                }
            } else {
                // Estado vacío
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Aún no hay reseñas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sé el primero en compartir tu opinión",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Componente compacto para mostrar resumen de reseñas en tarjetas de producto
 */
@Composable
fun CompactReviewSummary(
    stats: ReviewStats,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (stats.totalReviews > 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            onClick = { onClick?.invoke() },
            enabled = onClick != null,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RatingBar(
                        rating = stats.averageRating,
                        starSize = 20.dp,
                        showRatingNumber = true
                    )
                    Text(
                        text = "${stats.totalReviews} reseña${if (stats.totalReviews != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ver reseñas",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Badge inline para mostrar rating en listas de productos
 */
@Composable
fun InlineReviewBadge(
    averageRating: Double,
    totalReviews: Int,
    modifier: Modifier = Modifier
) {
    if (totalReviews > 0) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFB300)
                )
                Text(
                    text = String.format("%.1f", averageRating),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "($totalReviews)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Componente para mostrar invitación a dejar reseña cuando no hay ninguna
 */
@Composable
fun NoReviewsPrompt(
    onWriteReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onWriteReviewClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Sin reseñas aún",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Sé el primero en compartir tu opinión",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/**
 * Barra de progreso para distribución de ratings (alternativa visual)
 */
@Composable
fun RatingProgressBar(
    rating: Int,
    count: Int,
    totalReviews: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val percentage = if (totalReviews > 0) {
        (count.toFloat() / totalReviews * 100).toInt()
    } else 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = "$rating",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Barra
            LinearProgressIndicator(
                progress = percentage / 100f,
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp),
                color = Color(0xFFFFB300),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Conteo
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
        }
    }
}