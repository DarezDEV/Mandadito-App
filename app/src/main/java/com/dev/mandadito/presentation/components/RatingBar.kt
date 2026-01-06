package com.dev.mandadito.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Componente para mostrar calificación con estrellas (solo lectura)
 */
@Composable
fun RatingBar(
    rating: Double,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
    starColor: Color = Color(0xFFFFB300),
    showRatingNumber: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { index ->
            val starRating = index + 1
            Icon(
                imageVector = when {
                    rating >= starRating -> Icons.Filled.Star
                    rating >= starRating - 0.5 -> Icons.Filled.StarHalf
                    else -> Icons.Filled.StarBorder
                },
                contentDescription = "Star $starRating",
                tint = starColor,
                modifier = Modifier.size(starSize)
            )
        }

        if (showRatingNumber) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Componente para seleccionar calificación con estrellas (interactivo)
 */
@Composable
fun InteractiveRatingBar(
    currentRating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 32.dp,
    starColor: Color = Color(0xFFFFB300),
    enabled: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starRating = index + 1
            Icon(
                imageVector = if (starRating <= currentRating) {
                    Icons.Filled.Star
                } else {
                    Icons.Filled.StarBorder
                },
                contentDescription = "Star $starRating",
                tint = if (enabled) starColor else starColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(starSize)
                    .clickable(enabled = enabled) {
                        onRatingChanged(starRating)
                    }
            )
        }
    }
}

/**
 * Componente para mostrar distribución de calificaciones con barras
 */
@Composable
fun RatingDistribution(
    distribution: Map<Int, Int>,
    totalReviews: Int,
    modifier: Modifier = Modifier,
    onRatingClick: ((Int) -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (5 downTo 1).forEach { rating ->
            val count = distribution[rating] ?: 0
            val percentage = if (totalReviews > 0) {
                (count.toFloat() / totalReviews * 100).toInt()
            } else 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onRatingClick != null) {
                            Modifier.clickable { onRatingClick(rating) }
                        } else Modifier
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Número de estrellas
                Text(
                    text = "$rating",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(16.dp)
                )

                // Icono de estrella
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(16.dp)
                )

                // Barra de progreso
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                ) {
                    // Fondo de la barra
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 8.dp)
                    ) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawRoundRect(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                        }

                        // Barra de progreso rellena
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(percentage / 100f)
                        ) {
                            drawRoundRect(
                                color = Color(0xFFFFB300),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                }

                // Porcentaje
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

/**
 * Componente compacto de rating para listas de productos
 */
@Composable
fun CompactRatingBar(
    rating: Double,
    reviewCount: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 14.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFFFB300),
            modifier = Modifier.size(starSize)
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "($reviewCount)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}