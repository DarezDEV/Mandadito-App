package com.mandadito.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.Notification
import com.dev.mandadito.presentation.viewmodels.NotificationViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { NotificationViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var notificationToDelete by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Mostrar errores
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Aquí podrías mostrar un SnackBar
            viewModel.clearError()
        }
    }

    // Mostrar mensajes de éxito
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            // Aquí podrías mostrar un SnackBar
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notificaciones")
                        if (uiState.unreadCount > 0) {
                            Text(
                                "${uiState.unreadCount} sin leer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Filtro
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            if (uiState.filterType != null || uiState.showOnlyUnread)
                                Icons.Default.FilterAlt
                            else
                                Icons.Default.FilterAltOff,
                            "Filtrar"
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = {
                                viewModel.setFilterType(null)
                                viewModel.setShowOnlyUnread(false)
                                showFilterMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Notifications, null) }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Solo no leídas") },
                            onClick = {
                                viewModel.setShowOnlyUnread(!uiState.showOnlyUnread)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (uiState.showOnlyUnread)
                                    Icon(Icons.Default.CheckCircle, null)
                                else
                                    Icon(Icons.Default.Circle, null)
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Stock bajo") },
                            onClick = {
                                viewModel.setFilterType("LOW_STOCK")
                                showFilterMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Inventory, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Sin stock") },
                            onClick = {
                                viewModel.setFilterType("OUT_OF_STOCK")
                                showFilterMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.RemoveCircle, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Pedidos") },
                            onClick = {
                                viewModel.setFilterType("NEW_ORDER")
                                showFilterMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.ShoppingCart, null) }
                        )
                    }

                    // Marcar todas como leídas
                    if (uiState.unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(Icons.Default.DoneAll, "Marcar todas como leídas")
                        }
                    }

                    // Eliminar leídas
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, "Eliminar leídas")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                viewModel.filteredNotifications.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            null,
                            Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            if (uiState.filterType != null || uiState.showOnlyUnread)
                                "No hay notificaciones con este filtro"
                            else
                                "No hay notificaciones",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (uiState.filterType != null || uiState.showOnlyUnread) {
                            TextButton(onClick = {
                                viewModel.setFilterType(null)
                                viewModel.setShowOnlyUnread(false)
                            }) {
                                Text("Limpiar filtros")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.filteredNotifications, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    if (!notification.isRead) {
                                        viewModel.markAsRead(notification.id)
                                    }
                                },
                                onDelete = {
                                    notificationToDelete = notification.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar todas las leídas
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, null) },
            title = { Text("Eliminar leídas") },
            text = { Text("¿Eliminar todas las notificaciones leídas?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReadNotifications()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para eliminar una notificación específica
    notificationToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { notificationToDelete = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Eliminar notificación") },
            text = { Text("¿Estás seguro de eliminar esta notificación?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNotification(id)
                        notificationToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { notificationToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Indicador de no leída
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.Top)
                )
            }

            // Icono
            NotificationIcon(type = notification.type)

            // Contenido
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (notification.isPush) {
                        Icon(
                            Icons.Default.Notifications,
                            null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notification.isRead)
                        MaterialTheme.colorScheme.outline
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(notification.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Eliminar",
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationIcon(type: String) {
    val (icon, color) = when (type) {
        "INFO" -> Icons.Default.Info to Color(0xFF2196F3)
        "WARNING" -> Icons.Default.Warning to Color(0xFFFF9800)
        "ALERT" -> Icons.Default.Error to Color(0xFFF44336)
        "LOW_STOCK" -> Icons.Default.Inventory to Color(0xFFFF9800)
        "OUT_OF_STOCK" -> Icons.Default.RemoveCircle to Color(0xFFF44336)
        "NEW_ORDER" -> Icons.Default.ShoppingCart to Color(0xFF4CAF50)
        "ORDER_DELIVERED" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        "PAYMENT" -> Icons.Default.AttachMoney to Color(0xFF4CAF50)
        else -> Icons.Default.Notifications to Color(0xFF9E9E9E)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            Modifier.size(24.dp),
            tint = color
        )
    }
}

// Función para formatear timestamps
private fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val diff = now.epochSecond - instant.epochSecond

        when {
            diff < 60 -> "Ahora"
            diff < 3600 -> "${diff / 60} min"
            diff < 86400 -> "${diff / 3600} h"
            diff < 604800 -> "${diff / 86400} d"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}