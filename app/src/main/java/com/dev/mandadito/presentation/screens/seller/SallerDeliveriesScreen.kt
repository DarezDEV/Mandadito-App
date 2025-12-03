package com.dev.mandadito.presentation.screens.seller

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.mandadito.presentation.screens.seller.components.CreateDeliveryDialog
import com.dev.mandadito.presentation.screens.seller.components.DeliveryCard
import com.dev.mandadito.presentation.screens.seller.components.EditDeliveryDialog
import com.dev.mandadito.presentation.viewmodels.seller.DeliveriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDeliveriesScreen(
    viewModel: DeliveriesViewModel? = null
) {
    val context = LocalContext.current
    val deliveriesViewModel = viewModel ?: remember { DeliveriesViewModel(context) }
    val uiState by deliveriesViewModel.uiState.collectAsStateWithLifecycle()
    val filteredDeliveries = deliveriesViewModel.filteredDeliveries

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<com.dev.mandadito.data.models.DeliveryUser?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showToggleDialog by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Calcular estadísticas
    val activeCount = uiState.deliveries.count { it.activo }
    val inactiveCount = uiState.deliveries.count { !it.activo }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            deliveriesViewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            deliveriesViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar Delivery",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Mejorado con Estadísticas
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Título y Subtitle
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Equipo de Deliveries",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gestiona tu equipo de repartidores",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Estadísticas Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = activeCount.toString(),
                            label = "Activos",
                            icon = Icons.Outlined.CheckCircle,
                            color = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = inactiveCount.toString(),
                            label = "Inactivos",
                            icon = Icons.Outlined.Cancel,
                            color = MaterialTheme.colorScheme.error,
                            backgroundColor = MaterialTheme.colorScheme.errorContainer
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = uiState.deliveries.size.toString(),
                            label = "Total",
                            icon = Icons.Outlined.Groups,
                            color = MaterialTheme.colorScheme.tertiary,
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }

                    // Búsqueda
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { deliveriesViewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar delivery...") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { deliveriesViewModel.setSearchQuery("") }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Limpiar",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Filtros Chips Mejorados
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !uiState.showInactiveOnly,
                            onClick = { deliveriesViewModel.setShowInactiveOnly(false) },
                            label = { Text("Activos", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                AnimatedVisibility(visible = !uiState.showInactiveOnly) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        FilterChip(
                            selected = uiState.showInactiveOnly,
                            onClick = { deliveriesViewModel.setShowInactiveOnly(true) },
                            label = { Text("Inactivos", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                AnimatedVisibility(visible = uiState.showInactiveOnly) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }

            // Contenido Principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading && uiState.deliveries.isEmpty() -> {
                        LoadingState()
                    }

                    uiState.error != null && uiState.deliveries.isEmpty() && !uiState.isLoading -> {
                        ErrorState(error = uiState.error ?: "Error desconocido")
                    }

                    filteredDeliveries.isEmpty() -> {
                        EmptyState(
                            hasSearchQuery = uiState.searchQuery.isNotBlank(),
                            onAddClick = { showAddDialog = true }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredDeliveries,
                                key = { it.id }
                            ) { delivery ->
                                DeliveryCard(
                                    delivery = delivery,
                                    onEdit = { showEditDialog = delivery },
                                    onToggleActive = {
                                        showToggleDialog = Pair(delivery.id, delivery.activo)
                                    },
                                    onRemove = { showDeleteDialog = delivery.id }
                                )
                            }

                            // Espacio al final para el FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogos
    if (showAddDialog) {
        CreateDeliveryDialog(
            onDismiss = { showAddDialog = false },
            onDeliveryCreated = { email, password, nombre, avatarUri ->
                deliveriesViewModel.createDeliveryForColmado(email, password, nombre, avatarUri)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { delivery ->
        EditDeliveryDialog(
            delivery = delivery,
            onDismiss = { showEditDialog = null },
            onDeliveryUpdated = { nombre, email, avatarUri ->
                deliveriesViewModel.updateDelivery(delivery.id, nombre, email, avatarUri)
                showEditDialog = null
            }
        )
    }

    showDeleteDialog?.let { userId ->
        ConfirmationDialog(
            icon = Icons.Outlined.DeleteOutline,
            title = "¿Eliminar delivery?",
            message = "Este delivery será desvinculado de tu colmado. Podrás agregarlo nuevamente más tarde si lo necesitas.",
            confirmText = "Eliminar",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                deliveriesViewModel.removeDeliveryFromColmado(userId)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    showToggleDialog?.let { (userId, isActive) ->
        ConfirmationDialog(
            icon = if (isActive) Icons.Outlined.PersonOff else Icons.Outlined.PersonAddAlt,
            title = if (isActive) "¿Deshabilitar delivery?" else "¿Habilitar delivery?",
            message = if (isActive)
                "Este delivery no podrá realizar entregas hasta que lo habilites nuevamente."
            else
                "Este delivery podrá volver a realizar entregas.",
            confirmText = if (isActive) "Deshabilitar" else "Habilitar",
            confirmColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            onConfirm = {
                if (isActive) {
                    deliveriesViewModel.disableDelivery(userId)
                } else {
                    deliveriesViewModel.enableDelivery(userId)
                }
                showToggleDialog = null
            },
            onDismiss = { showToggleDialog = null }
        )
    }
}

// === COMPONENTES AUXILIARES ===

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Text(
                text = "Cargando deliveries...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Algo salió mal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyState(
    hasSearchQuery: Boolean,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasSearchQuery) Icons.Outlined.SearchOff else Icons.Outlined.PersonAddAlt,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = if (hasSearchQuery)
                    "No se encontraron resultados"
                else
                    "Tu equipo está vacío",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (hasSearchQuery)
                    "Intenta con otro término de búsqueda"
                else
                    "Comienza agregando deliveries a tu equipo para gestionar tus entregas de manera eficiente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!hasSearchQuery) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar Primer Delivery", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = confirmColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}