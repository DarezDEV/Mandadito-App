package com.dev.mandadito.presentation.screens.client

import androidx.compose.animation.*
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
import com.dev.mandadito.presentation.viewmodels.client.ClientOrdersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientOrderDetailScreen(
    orderId: String,
    viewModel: ClientOrdersViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Recargar órdenes cuando se abre la pantalla para asegurar que la orden recién creada esté disponible
    LaunchedEffect(orderId) {
        viewModel.loadOrders()
    }

    val orderWithDetails = uiState.orders.find { it.order.id == orderId }

    Scaffold(
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
        when {
            // Mostrar loading mientras se cargan las órdenes
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(padding))
            }
            // Si ya cargó y no encontró la orden, mostrar mensaje
            orderWithDetails == null -> {
                EmptyOrderState(modifier = Modifier.padding(padding))
            }
            // Mostrar el detalle de la orden
            else -> {
                OrderDetailContent(
                    orderWithDetails = orderWithDetails,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
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
    modifier: Modifier = Modifier
) {
    val order = orderWithDetails.order
    val items = orderWithDetails.items

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estado del pedido - Minimalista
        item {
            MinimalistStatusCard(status = order.status)
        }

        // Código de verificación (solo cuando está en delivery)
        if (order.status == OrderStatus.IN_DELIVERY && !order.verificationCode.isNullOrBlank()) {
            item {
                VerificationCodeCard(code = order.verificationCode)
            }
        }

        // Timeline del pedido
        item {
            OrderTimelineCard(currentStatus = order.status)
        }

        // Información del colmado
        orderWithDetails.colmado?.let { colmado ->
            item {
                ColmadoInfoCard(colmado = colmado)
            }
        }

        // Lista de productos
        item {
            ProductsCard(items = items)
        }

        // Notas del cliente
        if (!order.customerNotes.isNullOrBlank()) {
            item {
                NotesCard(notes = order.customerNotes)
            }
        }

        // Resumen de precios
        item {
            PriceSummaryCard(order = order)
        }

        // Razón de cancelación
        if (order.status == OrderStatus.CANCELLED && !order.cancellationReason.isNullOrBlank()) {
            item {
                CancellationCard(reason = order.cancellationReason)
            }
        }
    }
}

// ============================================
// ESTADO MINIMALISTA
// ============================================
@Composable
private fun MinimalistStatusCard(status: OrderStatus) {
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
            // Icono de estado
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

            // Información de estado
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
                    text = getStatusDescription(status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================
// CÓDIGO DE VERIFICACIÓN
// ============================================
@Composable
private fun VerificationCodeCard(code: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Código de Verificación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Proporciona este código al repartidor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================
// TIMELINE MINIMALISTA
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
                text = "Seguimiento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val steps = if (currentStatus == OrderStatus.CANCELLED) {
                listOf(
                    Triple(OrderStatus.PAID, "Confirmado", Icons.Outlined.CheckCircle),
                    Triple(OrderStatus.CANCELLED, "Cancelado", Icons.Outlined.Cancel)
                )
            } else {
                listOf(
                    Triple(OrderStatus.PAID, "Confirmado", Icons.Outlined.CheckCircle),
                    Triple(OrderStatus.PREPARING, "Preparando", Icons.Outlined.Restaurant),
                    Triple(OrderStatus.IN_DELIVERY, "En Camino", Icons.Outlined.LocalShipping),
                    Triple(OrderStatus.DELIVERED, "Entregado", Icons.Outlined.TaskAlt)
                )
            }

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
        // Indicador vertical
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Círculo
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

            // Línea conectora
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

        // Texto
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
// INFORMACIÓN DEL COLMADO
// ============================================
@Composable
private fun ColmadoInfoCard(colmado: com.dev.mandadito.data.models.ColmadoInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = colmado.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = colmado.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        // Imagen del producto
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

        // Información del producto
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", item.productPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "×${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Subtotal
        Text(
            text = "$${String.format("%.2f", item.subtotal)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ============================================
// NOTAS
// ============================================
@Composable
private fun NotesCard(notes: String) {
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
                    text = "Tus notas",
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
// CANCELACIÓN
// ============================================
@Composable
private fun CancellationCard(reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Motivo de cancelación",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
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

private fun getStatusDescription(status: OrderStatus): String {
    return when (status) {
        OrderStatus.PENDING -> "Tu pedido está pendiente"
        OrderStatus.PAYMENT_PROCESSING -> "Procesando pago"
        OrderStatus.PAID -> "Pedido confirmado"
        OrderStatus.PREPARING -> "Preparando tu pedido"
        OrderStatus.READY_FOR_PICKUP -> "Listo para recoger"
        OrderStatus.IN_DELIVERY -> "Tu pedido va en camino"
        OrderStatus.DELIVERED -> "Pedido entregado"
        OrderStatus.CANCELLED -> "Pedido cancelado"
        OrderStatus.REFUNDED -> "Pedido reembolsado"
    }
}