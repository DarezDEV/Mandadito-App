package com.dev.mandadito.presentation.screens.delivery

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.presentation.viewmodels.delivery.DeliveryOrdersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrdersScreenWithFilters(
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: DeliveryOrdersViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DeliveryOrdersViewModel(context) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf<FilterType>(FilterType.All) }

    val filteredOrders = when (selectedFilter) {
        FilterType.All -> uiState.orders
        FilterType.InDelivery -> uiState.orders.filter {
            it.order.status == OrderStatus.IN_DELIVERY
        }

        FilterType.Delivered -> uiState.orders.filter {
            it.order.status == OrderStatus.DELIVERED
        }
    }

    val inDeliveryCount = uiState.orders.count { it.order.status == OrderStatus.IN_DELIVERY }
    val deliveredCount = uiState.orders.count { it.order.status == OrderStatus.DELIVERED }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            com.dev.mandadito.presentation.components.CustomSnackbarHost(
                hostState = snackbarHostState
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Compact Stats Row - Reemplaza las StatCards grandes
                CompactStatsRow(
                    inDeliveryCount = inDeliveryCount,
                    deliveredCount = deliveredCount
                )

                // Minimal Filter Tabs
                MinimalFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    allCount = uiState.orders.size,
                    inDeliveryCount = inDeliveryCount,
                    deliveredCount = deliveredCount
                )

                // Orders List
                AnimatedContent(
                    targetState = filteredOrders.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "orders_content"
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyDeliveryState(hasFilter = selectedFilter != FilterType.All)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredOrders, key = { it.order.id }) { order ->
                                MinimalOrderCard(
                                    order = order,
                                    onClick = {
                                        navController.navigate("delivery_order_detail/${order.order.id}")
                                    }
                                )
                            }
                        }
                    }
                }

                // Loading overlay
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Cargando...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
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
}

// ============================================
// COMPACT STATS ROW (Reemplaza StatCards)
// ============================================
@Composable
private fun CompactStatsRow(
    inDeliveryCount: Int,
    deliveredCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CompactStat(
                count = inDeliveryCount,
                label = "En Camino",
                icon = Icons.Outlined.LocalShipping,
                color = Color(0xFF06B6D4),
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            CompactStat(
                count = deliveredCount,
                label = "Entregados",
                icon = Icons.Outlined.CheckCircle,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactStat(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================
// MINIMAL FILTER TABS
// ============================================
@Composable
private fun MinimalFilterTabs(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    allCount: Int,
    inDeliveryCount: Int,
    deliveredCount: Int
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 20.dp,
        indicator = { tabPositions ->
            if (selectedFilter.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilter.ordinal]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        divider = {}
    ) {
        Tab(
            selected = selectedFilter == FilterType.All,
            onClick = { onFilterSelected(FilterType.All) },
            text = {
                Text(
                    "Todos ($allCount)",
                    fontWeight = if (selectedFilter == FilterType.All)
                        FontWeight.Bold else FontWeight.Medium
                )
            }
        )

        if (inDeliveryCount > 0) {
            Tab(
                selected = selectedFilter == FilterType.InDelivery,
                onClick = { onFilterSelected(FilterType.InDelivery) },
                text = {
                    Text(
                        "En Camino ($inDeliveryCount)",
                        fontWeight = if (selectedFilter == FilterType.InDelivery)
                            FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }

        if (deliveredCount > 0) {
            Tab(
                selected = selectedFilter == FilterType.Delivered,
                onClick = { onFilterSelected(FilterType.Delivered) },
                text = {
                    Text(
                        "Entregados ($deliveredCount)",
                        fontWeight = if (selectedFilter == FilterType.Delivered)
                            FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

// ============================================
// MINIMAL ORDER CARD
// ============================================
@Composable
private fun MinimalOrderCard(
    order: com.dev.mandadito.data.models.OrderWithDetails,
    onClick: () -> Unit
) {
    val statusColor = when (order.order.status) {
        OrderStatus.IN_DELIVERY -> Color(0xFF06B6D4)
        OrderStatus.DELIVERED -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Order # + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = order.order.status.toDisplayString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            // Colmado Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Store,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.colmado?.name ?: "Colmado",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${order.items.size} producto${if (order.items.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${String.format("%.2f", order.order.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Action Button - Solo si está en delivery
            if (order.order.status == OrderStatus.IN_DELIVERY) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirmar entrega",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ============================================
// ESTADO VACÍO
// ============================================
@Composable
private fun EmptyDeliveryState(hasFilter: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasFilter) Icons.Outlined.SearchOff
                    else Icons.Outlined.DeliveryDining,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (hasFilter) {
                        "No hay pedidos"
                    } else {
                        "Sin entregas asignadas"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (hasFilter) {
                        "Intenta con otro filtro"
                    } else {
                        "Los pedidos aparecerán aquí cuando te sean asignados"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
// ============================================
// TIPOS DE FILTRO
// ============================================
private enum class FilterType {
    All, InDelivery, Delivered
}