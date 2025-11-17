package com.dev.mandadito.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.UserProfile
import com.dev.mandadito.presentation.viewmodels.admin.AdminUsersViewModel
import com.dev.mandadito.presentation.components.InternetErrorAlert
import com.dev.mandadito.presentation.screens.admin.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminUsersViewModel? = null
) {
    val context = LocalContext.current
    val adminViewModel = viewModel ?: remember { AdminUsersViewModel(context) }
    val uiState by adminViewModel.uiState.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<UserProfile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<UserProfile?>(null) }
    var isCreatingUser by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isInternetError = uiState.error != null &&
            uiState.error!!.contains("cargar los usuarios", ignoreCase = true)
    var showInternetAlert by remember { mutableStateOf(false) }

    // Effects
    AdminUsersEffects(
        uiState = uiState,
        isInternetError = isInternetError,
        snackbarHostState = snackbarHostState,
        onInternetErrorChange = { showInternetAlert = it },
        onClearSuccess = { adminViewModel.clearSuccess() },
        onClearError = { adminViewModel.clearError() }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Alerta de error de internet
            InternetErrorAlert(
                isVisible = showInternetAlert,
                onRetry = {
                    adminViewModel.clearError()
                    adminViewModel.loadUsers()
                },
                onDismiss = {
                    showInternetAlert = false
                    adminViewModel.clearError()
                },
                message = "No se pudieron cargar los usuarios. Verifique su conexión a internet."
            )

            // Header
            AdminUsersHeader()

            // Botón y búsqueda
            AdminUsersActions(
                searchQuery = uiState.searchQuery,
                isCreatingUser = isCreatingUser,
                onSearchChange = { adminViewModel.setSearchQuery(it) },
                onAddUserClick = { showDialog = true }
            )

            // Diálogos
            AdminUsersDialogs(
                showDialog = showDialog,
                showEditDialog = showEditDialog,
                showDeleteConfirm = showDeleteConfirm,
                onDismissAddDialog = {
                    showDialog = false
                    isCreatingUser = false
                },
                onUserAdded = { email, password, nombre, telefono, role, avatarUri ->1
                    isCreatingUser = true
                    adminViewModel.createUser(email, password, nombre, role, avatarUri)
                    showDialog = false
                    kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(2000)
                        isCreatingUser = false
                    }
                },
                onDismissEditDialog = { showEditDialog = null },
                onUserUpdated = { nombre, telefono ->
                    showEditDialog?.let { user ->
                        adminViewModel.updateUserProfile(user.id, nombre)
                        showEditDialog = null
                    }
                },
                onDismissDeleteConfirm = { showDeleteConfirm = null },
                onDeleteConfirmed = {
                    showDeleteConfirm?.let { user ->
                        adminViewModel.deleteUser(user.id)
                        showDeleteConfirm = null
                    }
                }
            )

            // Tabs
            AdminUsersTabs(
                uiState = uiState,
                onShowDisabledChange = { adminViewModel.setShowDisabledOnly(it) },
                onRoleFilterChange = { adminViewModel.setRoleFilter(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de usuarios
            AdminUsersList(
                filteredUsers = adminViewModel.filteredUsers,
                isLoading = uiState.isLoading,
                isInternetError = isInternetError,
                showInternetAlert = showInternetAlert,
                showDisabledOnly = uiState.showDisabledOnly,
                onEdit = { showEditDialog = it },
                onDisable = { adminViewModel.disableUser(it.id) },
                onActivate = { adminViewModel.enableUser(it.id) },
                onDelete = { showDeleteConfirm = it }
            )
        }
    }
}