package com.dev.mandadito.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Maneja la visualización de mensajes de error y éxito mediante Snackbar
 *
 * @param snackbarHostState Estado del SnackbarHost
 * @param errorMessage Mensaje de error a mostrar (null si no hay error)
 * @param successMessage Mensaje de éxito a mostrar (null si no hay éxito)
 * @param onErrorDismiss Callback cuando se descarta el error
 * @param onSuccessDismiss Callback cuando se descarta el éxito
 */
@Composable
fun SnackbarHandler(
    snackbarHostState: SnackbarHostState,
    errorMessage: String?,
    successMessage: String?,
    onErrorDismiss: () -> Unit,
    onSuccessDismiss: () -> Unit
) {
    // Mostrar error
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            onErrorDismiss()
        }
    }

    // Mostrar éxito
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            onSuccessDismiss()
        }
    }
}

/**
 * SnackbarHost personalizado con diseño minimalista
 */
@Composable
fun CustomSnackbarHost(
    hostState: SnackbarHostState,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.inversePrimary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        }
    )
}
