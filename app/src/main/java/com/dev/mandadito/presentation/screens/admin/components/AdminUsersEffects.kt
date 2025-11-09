package com.dev.mandadito.presentation.screens.admin.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.dev.mandadito.presentation.viewmodels.admin.AdminUsersUiState
import kotlinx.coroutines.launch

@Composable
fun AdminUsersEffects(
    uiState: AdminUsersUiState,
    isInternetError: Boolean,
    snackbarHostState: SnackbarHostState,
    onInternetErrorChange: (Boolean) -> Unit,
    onClearSuccess: () -> Unit,
    onClearError: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Mostrar alerta cuando hay error de internet
    LaunchedEffect(isInternetError) {
        onInternetErrorChange(isInternetError)
    }

    // Success Snackbar
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                onClearSuccess()
            }
        }
    }

    // General Error Snackbar
    LaunchedEffect(key1 = uiState.error) {
        if (uiState.error != null && !isInternetError) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = uiState.error!!,
                    duration = SnackbarDuration.Long
                )
                onClearError()
            }
        }
    }
}
