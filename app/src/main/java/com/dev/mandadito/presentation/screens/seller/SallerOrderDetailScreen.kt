package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.presentation.screens.seller.components.AssignDeliveryDialog
import com.dev.mandadito.presentation.viewmodels.seller.SellerOrdersViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: SellerOrdersViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val orderWithDetails = uiState.orders.find { it.order.id == orderId }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            com.dev.mandadito.presentation.components.CustomSnackbarHost(
                hostState = snackbarHostState
            )
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = orderWithDetails?.order?.orderNumber?.let { "#$it" } ?: "Pedido",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { padding ->
        if (orderWithDetails == null) {
            EmptyOrderState(modifier = Modifier.padding(padding))
        } else {
            OrderDetailContent(
                orderWithDetails = orderWithDetails,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }

    // Manejar notificaciones de error y éxito
    com.dev.mandadito.presentation.components.SnackbarHandler(
        snackbarHostState = snackbarHostState,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onErrorDismiss = { viewModel.clearError() },
        onSuccessDismiss = { viewModel.clearSuccess() }
    )
}

@Composable
private fun EmptyOrderState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Pedido no encontrado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrderDetailContent(
    orderWithDetails: OrderWithDetails,
    viewModel: SellerOrdersViewModel,
    modifier: Modifier = Modifier
) {
    val order = orderWithDetails.order
    val items = orderWithDetails.items
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAssignDeliveryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estado + Fecha
        item {
            OrderStatusCard(
                status = order.status,
                createdAt = order.createdAt
            )
        }

        // Acciones principales según estado
        item {
            OrderActionsCard(
                currentStatus = order.status,
                verificationCode = order.verificationCode,
                onUpdateStatus = { newStatus ->
                    viewModel.updateOrderStatus(order.id, newStatus)
                },
                onAssignDelivery = { showAssignDeliveryDialog = true }
            )
        }

        // Timeline del proceso
        item {
            OrderTimelineCard(currentStatus = order.status)
        }

        // Productos
        item {
            ProductsCard(items = items)
        }

        // Notas del cliente
        if (!order.customerNotes.isNullOrBlank()) {
            item {
                CustomerNotesCard(notes = order.customerNotes)
            }
        }

        // Resumen de precios
        item {
            PriceSummaryCard(order = order)
        }
    }

    // Diálogo de asignación de delivery
    if (showAssignDeliveryDialog) {
        LaunchedEffect(Unit) {
            viewModel.loadAvailableDeliveries()
        }

        AssignDeliveryDialog(
            deliveries = uiState.availableDeliveries,
            onDismiss = { showAssignDeliveryDialog = false },
            onDeliverySelected = { deliveryUserId ->
                viewModel.assignDeliveryToOrder(order.id, deliveryUserId)
                showAssignDeliveryDialog = false
            },
            isLoading = uiState.isLoading,
            isLoadingDeliveries = uiState.isLoadingDeliveries
        )
    }
}

// ============================================
// ESTADO + FECHA
// ============================================
@Composable
private fun OrderStatusCard(
    status: OrderStatus,
    createdAt: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = getStatusColor(status).copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(getStatusColor(status).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getStatusIcon(status),
                    contentDescription = null,
                    tint = getStatusColor(status),
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = status.toDisplayString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatOrderDate(createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================
// ACCIONES DEL PEDIDO
// ============================================
@Composable
private fun OrderActionsCard(
    currentStatus: OrderStatus,
    verificationCode: String?,
    onUpdateStatus: (OrderStatus) -> Unit,
    onAssignDelivery: () -> Unit
) {
    when (currentStatus) {
        OrderStatus.PAID -> {
            ActionButton(
                text = "Comenzar Preparación",
                icon = Icons.Default.Restaurant,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onUpdateStatus(OrderStatus.PREPARING) }
            )
        }

        OrderStatus.PREPARING -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    text = "Asignar Delivery",
                    icon = Icons.Default.DeliveryDining,
                    color = Color(0xFF06B6D4),
                    onClick = onAssignDelivery
                )
            }
        }

        OrderStatus.IN_DELIVERY -> {
            if (!verificationCode.isNullOrBlank()) {
                VerificationCodeCard(code = verificationCode)
            }
        }

        else -> {
            // No hay acciones para otros estados
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VerificationCodeCard(code: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10B981).copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Código de Verificación",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981),
                    letterSpacing = 4.sp
                )
            }
        }
    }
}

// ============================================
// TIMELINE DEL PEDIDO
// ============================================
@Composable
private fun OrderTimelineCard(currentStatus: OrderStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Progreso del Pedido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val steps = listOf(
                Triple(OrderStatus.PAID, "Pagado", Icons.Outlined.CheckCircle),
                Triple(OrderStatus.PREPARING, "Preparando", Icons.Outlined.Restaurant),
                Triple(OrderStatus.IN_DELIVERY, "En Camino", Icons.Outlined.LocalShipping),
                Triple(OrderStatus.DELIVERED, "Entregado", Icons.Outlined.TaskAlt)
            )

            steps.forEachIndexed { index, (status, label, icon) ->
                TimelineStep(
                    status = status,
                    label = label,
                    icon = icon,
                    currentStatus = currentStatus,
                    isLast = index == steps.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TimelineStep(
    status: OrderStatus,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentStatus: OrderStatus,
    isLast: Boolean
) {
    val statusIndex = getStatusIndex(status)
    val currentIndex = getStatusIndex(currentStatus)
    val isCompleted = currentIndex > statusIndex
    val isCurrent = currentStatus == status
    val isActive = isCompleted || isCurrent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================
// PRODUCTOS
// ============================================
@Composable
private fun ProductsCard(items: List<com.dev.mandadito.data.models.OrderItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Productos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items.forEach { item ->
                ProductItem(item = item)
                if (item != items.last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun ProductItem(item: com.dev.mandadito.data.models.OrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (item.productImageUrl != null) {
                AsyncImage(
                    model = item.productImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Outlined.Fastfood,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$${String.format("%.2f", item.productPrice)} × ${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "$${String.format("%.2f", item.subtotal)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ============================================
// NOTAS DEL CLIENTE
// ============================================
@Composable
private fun CustomerNotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.StickyNote2,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Notas del Cliente",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// ============================================
// RESUMEN DE PRECIOS
// ============================================
@Composable
private fun PriceSummaryCard(order: com.dev.mandadito.data.models.Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            PriceRow("Subtotal", order.subtotal)
            PriceRow("Envío", order.deliveryFee)
            PriceRow("Comisión", order.platformFee)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================
// FUNCIONES AUXILIARES
// ============================================
private fun getStatusIndex(status: OrderStatus): Int {
    return when (status) {
        OrderStatus.PENDING, OrderStatus.PAYMENT_PROCESSING, OrderStatus.PAID -> 0
        OrderStatus.PREPARING -> 1
        OrderStatus.IN_DELIVERY -> 2
        OrderStatus.DELIVERED -> 3
        OrderStatus.READY_FOR_PICKUP -> -1
        OrderStatus.CANCELLED -> -1
        OrderStatus.REFUNDED -> -2
    }
}

private fun getStatusColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.PENDING -> Color(0xFFF59E0B)
        OrderStatus.PAYMENT_PROCESSING -> Color(0xFF1C49C0)
        OrderStatus.PAID -> Color(0xFF1C49C0)
        OrderStatus.PREPARING -> Color(0xFF8B5CF6)
        OrderStatus.READY_FOR_PICKUP -> Color(0xFF06B6D4)
        OrderStatus.IN_DELIVERY -> Color(0xFF06B6D4)
        OrderStatus.DELIVERED -> Color(0xFF10B981)
        OrderStatus.CANCELLED -> Color(0xFFEF4444)
        OrderStatus.REFUNDED -> Color(0xFFF97316)
    }
}

private fun getStatusIcon(status: OrderStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        OrderStatus.PENDING -> Icons.Outlined.Schedule
        OrderStatus.PAYMENT_PROCESSING -> Icons.Outlined.Payment
        OrderStatus.PAID -> Icons.Outlined.CheckCircle
        OrderStatus.PREPARING -> Icons.Outlined.Restaurant
        OrderStatus.READY_FOR_PICKUP -> Icons.Outlined.TaskAlt
        OrderStatus.IN_DELIVERY -> Icons.Outlined.LocalShipping
        OrderStatus.DELIVERED -> Icons.Outlined.TaskAlt
        OrderStatus.CANCELLED -> Icons.Outlined.Cancel
        OrderStatus.REFUNDED -> Icons.Outlined.MoneyOff
    }
}

private fun formatOrderDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' hh:mm a")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        dateString
    }
}