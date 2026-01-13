package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dev.mandadito.presentation.components.InteractiveRatingBar
import com.dev.mandadito.presentation.viewmodels.client.ProductReviewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(
    productId: String,
    productName: String,
    userId: String,
    navController: NavController,
    existingReviewId: String? = null,
    existingRating: Int? = null,
    existingTitle: String? = null,
    existingComment: String? = null,
    viewModel: ProductReviewsViewModel = viewModel()
) {
    var rating by remember { mutableStateOf(existingRating ?: 0) }
    var title by remember { mutableStateOf(existingTitle ?: "") }
    var comment by remember { mutableStateOf(existingComment ?: "") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val isEditing = existingReviewId != null
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Editar reseña" else "Escribir reseña",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Nombre del producto
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Producto",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Rating
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Calificación *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                InteractiveRatingBar(
                    currentRating = rating,
                    onRatingChanged = {
                        rating = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    starSize = 44.dp
                )

                if (rating > 0) {
                    Text(
                        text = getRatingLabel(rating),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Título
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Título de tu reseña (opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 200) title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Resumen de tu experiencia") },
                    supportingText = {
                        Text(
                            "${title.length}/200",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Comentario
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tu reseña (opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = { Text("Comparte tu experiencia con otros compradores...") },
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "¿Qué te gustó o qué mejorarías?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón enviar
            Button(
                onClick = {
                    if (rating == 0) {
                        showError = true
                        errorMessage = "Por favor selecciona una calificación"
                        return@Button
                    }

                    isSubmitting = true

                    if (isEditing && existingReviewId != null) {
                        viewModel.updateReview(
                            reviewId = existingReviewId,
                            productId = productId,
                            userId = userId,
                            rating = rating,
                            title = title.ifBlank { null },
                            comment = comment.ifBlank { null },
                            onSuccess = {
                                navController.popBackStack()
                            },
                            onError = { error ->
                                isSubmitting = false
                                showError = true
                                errorMessage = error
                            }
                        )
                    } else {
                        viewModel.createReview(
                            productId = productId,
                            userId = userId,
                            rating = rating,
                            title = title.ifBlank { null },
                            comment = comment.ifBlank { null },
                            onSuccess = {
                                navController.popBackStack()
                            },
                            onError = { error ->
                                isSubmitting = false
                                showError = true
                                errorMessage = error
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = rating > 0 && !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        if (isEditing) "Actualizar reseña" else "Publicar reseña",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mensaje de error
            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Consejos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Consejos para tu reseña:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Sé específico sobre lo que te gustó o no",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Menciona la calidad, precio y servicio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Ayuda a otros compradores con tu experiencia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getRatingLabel(rating: Int): String {
    return when (rating) {
        1 -> "⭐ Muy malo"
        2 -> "⭐⭐ Malo"
        3 -> "⭐⭐⭐ Regular"
        4 -> "⭐⭐⭐⭐ Bueno"
        5 -> "⭐⭐⭐⭐⭐ Excelente"
        else -> ""
    }
}