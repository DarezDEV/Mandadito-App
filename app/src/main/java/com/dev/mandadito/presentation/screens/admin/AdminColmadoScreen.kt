package com.dev.mandadito.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.ColmadoWithOwner
import com.dev.mandadito.presentation.viewmodels.admin.AdminColmadosViewModel
import com.dev.mandadito.presentation.components.InternetErrorAlert
import com.dev.mandadito.presentation.screens.admin.components.*
import kotlinx.coroutines.launch

@Composable
fun AdminColmadoScreen(
    viewModel: AdminColmadosViewModel? = null
) {
    val context = LocalContext.current
    val adminViewModel = viewModel ?: remember { AdminColmadosViewModel(context) }
    val uiState by adminViewModel.uiState.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf<ColmadoWithOwner?>(null) }
    var showDeactivateConfirm by remember { mutableStateOf<ColmadoWithOwner?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isInternetError = uiState.error != null &&
            uiState.error!!.contains("cargar los colmados", ignoreCase = true)
    var showInternetAlert by remember { mutableStateOf(false) }

    // Effects para mostrar mensajes
    LaunchedEffect(isInternetError) {
        showInternetAlert = isInternetError
    }

    // Success Snackbar
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                adminViewModel.clearSuccess()
            }
        }
    }

    // Error Snackbar
    LaunchedEffect(key1 = uiState.error) {
        if (uiState.error != null && !isInternetError) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = uiState.error!!,
                    duration = SnackbarDuration.Long
                )
                adminViewModel.clearError()
            }
        }
    }

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
                    adminViewModel.loadColmados()
                },
                onDismiss = {
                    showInternetAlert = false
                    adminViewModel.clearError()
                },
                message = "No se pudieron cargar los colmados. Verifique su conexión a internet."
            )

            // Header
            ColmadosHeader()

            // Búsqueda
            ColmadosSearch(
                searchQuery = uiState.searchQuery,
                onSearchChange = { adminViewModel.setSearchQuery(it) }
            )

            // Diálogos de confirmación
            ColmadosDialogs(
                showDeleteConfirm = showDeleteConfirm,
                showDeactivateConfirm = showDeactivateConfirm,
                onDismissDeleteConfirm = { showDeleteConfirm = null },
                onDeleteConfirmed = {
                    showDeleteConfirm?.let { colmado ->
                        adminViewModel.deleteColmado(colmado.id)
                        showDeleteConfirm = null
                    }
                },
                onDismissDeactivateConfirm = { showDeactivateConfirm = null },
                onDeactivateConfirmed = {
                    showDeactivateConfirm?.let { colmado ->
                        adminViewModel.deactivateColmado(colmado.id)
                        showDeactivateConfirm = null
                    }
                }
            )

            // Tabs
            ColmadosTabs(
                showInactiveOnly = uiState.showInactiveOnly,
                onShowInactiveChange = { adminViewModel.setShowInactiveOnly(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de colmados
            ColmadosList(
                filteredColmados = adminViewModel.filteredColmados,
                isLoading = uiState.isLoading,
                isInternetError = isInternetError,
                showInternetAlert = showInternetAlert,
                showInactiveOnly = uiState.showInactiveOnly,
                onDeactivate = { showDeactivateConfirm = it },
                onActivate = { adminViewModel.activateColmado(it.id) },
                onDelete = { showDeleteConfirm = it }
            )
        }
    }
}

@Composable
private fun ColmadosHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Gestión de Colmados",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Administra todos los colmados del sistema",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ColmadosSearch(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(20.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            placeholder = {
                Text(
                    "Buscar por nombre, dirección, teléfono...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun ColmadosDialogs(
    showDeleteConfirm: ColmadoWithOwner?,
    showDeactivateConfirm: ColmadoWithOwner?,
    onDismissDeleteConfirm: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDismissDeactivateConfirm: () -> Unit,
    onDeactivateConfirmed: () -> Unit
) {
    // Diálogo de eliminación
    showDeleteConfirm?.let { colmado ->
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Confirmar eliminación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Estás seguro de que deseas eliminar permanentemente el colmado \"${colmado.name}\"?")
                    Text(
                        "⚠️ ADVERTENCIA: Esto también eliminará todos los usuarios asociados a este colmado (dueño y deliveries). Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDeleteConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirm) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Diálogo de desactivación
    showDeactivateConfirm?.let { colmado ->
        AlertDialog(
            onDismissRequest = onDismissDeactivateConfirm,
            icon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Confirmar desactivación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Deseas desactivar el colmado \"${colmado.name}\"?")
                    Text(
                        "Esto también desactivará temporalmente a todos los usuarios asociados (dueño y deliveries). Podrán volver a acceder cuando el colmado sea activado nuevamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDeactivateConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("Desactivar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeactivateConfirm) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ColmadosTabs(
    showInactiveOnly: Boolean,
    onShowInactiveChange: (Boolean) -> Unit
) {
    TabRow(
        selectedTabIndex = if (showInactiveOnly) 1 else 0,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (showInactiveOnly) 1 else 0]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Tab(
            selected = !showInactiveOnly,
            onClick = { onShowInactiveChange(false) },
            text = {
                Text(
                    text = "Activos",
                    fontWeight = if (!showInactiveOnly) FontWeight.Bold else FontWeight.Normal
                )
            }
        )
        Tab(
            selected = showInactiveOnly,
            onClick = { onShowInactiveChange(true) },
            text = {
                Text(
                    text = "Inactivos",
                    fontWeight = if (showInactiveOnly) FontWeight.Bold else FontWeight.Normal
                )
            }
        )
    }
}

@Composable
private fun ColmadosList(
    filteredColmados: List<ColmadoWithOwner>,
    isLoading: Boolean,
    isInternetError: Boolean,
    showInternetAlert: Boolean,
    showInactiveOnly: Boolean,
    onDeactivate: (ColmadoWithOwner) -> Unit,
    onActivate: (ColmadoWithOwner) -> Unit,
    onDelete: (ColmadoWithOwner) -> Unit
) {
    val showSkeleton = isLoading || (isInternetError && !showInternetAlert)

    when {
        showSkeleton && filteredColmados.isEmpty() -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    SkeletonUserCard()
                }
            }
        }
        filteredColmados.isEmpty() -> {
            EmptyColmadosState()
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredColmados,
                    key = { it.id }
                ) { colmado ->
                    ColmadoCard(
                        colmado = colmado,
                        isInactiveSection = showInactiveOnly,
                        onDeactivate = { onDeactivate(colmado) },
                        onActivate = { onActivate(colmado) },
                        onDelete = { onDelete(colmado) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyColmadosState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Storefront,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "No se encontraron colmados",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Intenta con otros filtros",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}