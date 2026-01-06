package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dev.mandadito.presentation.components.RatingBar
import com.dev.mandadito.presentation.components.ReviewCard
import com.dev.mandadito.presentation.viewmodels.client.ProductReviewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewsScreen(
    productId: String,
    productName: String,
    userId: String?,
    navController: NavController,
    viewModel: ProductReviewsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadReviews(productId, userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Reseñas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            productName,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.reviews.isNotEmpty()) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Badge(
                                containerColor = if (uiState.selectedRatingFilter != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filtrar",
                                    tint = if (uiState.selectedRatingFilter != null)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Solo mostrar FAB si hay userId y no tiene reseña
            val canAddReview = userId != null && uiState.userReview == null
            if (canAddReview) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("product_name", productName)
                        navController.navigate("add_review/$productId")
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Agregar reseña") }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = uiState.error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadReviews(productId, userId) }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            uiState.reviews.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            "No hay reseñas aún",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Sé el primero en compartir tu opinión",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Solo mostrar botón si hay userId
                        if (userId != null && uiState.userReview == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    navController.currentBackStackEntry?.savedStateHandle?.set("product_name", productName)
                                    navController.navigate("add_review/$productId")
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escribir reseña")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Resumen de estadísticas
                    if (uiState.stats.totalReviews > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = String.format("%.1f", uiState.stats.averageRating),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    RatingBar(
                                        rating = uiState.stats.averageRating,
                                        starSize = 24.dp
                                    )
                                    Text(
                                        text = "${uiState.stats.totalReviews} reseñas",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Título de sección
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.selectedRatingFilter != null)
                                    "Reseñas con ${uiState.selectedRatingFilter} estrellas"
                                else
                                    "Todas las reseñas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.selectedRatingFilter != null) {
                                TextButton(
                                    onClick = { viewModel.filterByRating(productId, null, userId) }
                                ) {
                                    Text("Limpiar filtro")
                                }
                            }
                        }
                    }

                    // Lista de reseñas
                    items(uiState.reviews) { review ->
                        ReviewCard(
                            review = review,
                            currentUserId = userId,
                            onEdit = {
                                navController.currentBackStackEntry?.savedStateHandle?.set("product_name", productName)
                                navController.navigate(
                                    "edit_review/$productId/${review.id}/${review.rating}/${review.title ?: ""}/${review.comment ?: ""}"
                                )
                            },
                            onDelete = {
                                userId?.let { uid ->
                                    viewModel.deleteReview(
                                        reviewId = review.id,
                                        productId = productId,
                                        userId = uid,
                                        onSuccess = { },
                                        onError = { }
                                    )
                                }
                            },
                            onMarkHelpful = {
                                viewModel.markAsHelpful(review.id, productId, userId)
                            }
                        )
                    }

                    // Espaciado para el FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialog de filtro
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filtrar por calificación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 4, 3, 2, 1).forEach { rating ->
                        FilterChip(
                            selected = uiState.selectedRatingFilter == rating,
                            onClick = {
                                viewModel.filterByRating(productId, rating, userId)
                                showFilterDialog = false
                            },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(rating) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text("$rating estrellas")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}