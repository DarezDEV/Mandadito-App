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
import com.dev.mandadito.data.models.OrderStatus
import com.dev.mandadito.data.models.OrderWithDetails
import com.dev.mandadito.presentation.viewmodels.seller.SellerOrdersViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersScreen(
    viewModel: SellerOrdersViewModel? = null,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val ordersViewModel = viewModel ?: remember { SellerOrdersViewModel(context) }
    val uiState by ordersViewModel.uiState.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf<FilterType>(FilterType.All) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOrders = remember(uiState.orders, selectedFilter, searchQuery) {
        uiState.orders.filter { orderWithDetails ->
            val matchesFilter = when (selectedFilter) {
                FilterType.All -> true
                FilterType.Active -> orderWithDetails.order.status in listOf(
                    OrderStatus.PAID,
                    OrderStatus.PREPARING,
                    OrderStatus.IN_DELIVERY
                )
                FilterType.Completed -> orderWithDetails.order.status == OrderStatus.DELIVERED
                FilterType.Cancelled -> orderWithDetails.order.status == OrderStatus.CANCELLED
            }

            val matchesSearch = searchQuery.isEmpty() ||
                    orderWithDetails.order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    orderWithDetails.items.any { it.productName.contains(searchQuery, ignoreCase = true) }

            matchesFilter && matchesSearch
        }
    }

    val activeCount = uiState.orders.count {
        it.order.status in listOf(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.IN_DELIVERY)
    }
    val completedCount = uiState.orders.count { it.order.status == OrderStatus.DELIVERED }
    val todayRevenue = uiState.orders
        .filter { it.order.status == OrderStatus.DELIVERED && isSameDay(it.order.createdAt) }
        .sumOf { it.order.total }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            com.dev.mandadito.presentation.components.CustomSnackbarHost(
                hostState = snackbarHostState
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Compact Stats Header
            CompactStatsHeader(
                activeCount = activeCount,
                completedCount = completedCount,
                todayRevenue = todayRevenue
            )

            // Search Bar
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )

            // Minimal Filter Tabs
            MinimalFilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                allCount = uiState.orders.size,
                activeCount = activeCount,
                completedCount = completedCount
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
                    EmptyOrdersState(
                        hasFilters = selectedFilter != FilterType.All || searchQuery.isNotEmpty(),
                        onClearFilters = {
                            selectedFilter = FilterType.All
                            searchQuery = ""
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredOrders,
                            key = { it.order.id }
                        ) { orderWithDetails ->
                            MinimalOrderCard(
                                orderWithDetails = orderWithDetails,
                                onViewDetails = {
                                    onNavigateToDetail?.invoke(orderWithDetails.order.id)
                                }
                            )
                        }
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

    // Manejar notificaciones de error y éxito
    com.dev.mandadito.presentation.components.SnackbarHandler(
        snackbarHostState = snackbarHostState,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onErrorDismiss = { ordersViewModel.clearError() },
        onSuccessDismiss = { ordersViewModel.clearSuccess() }
    )
}

// ============================================
// COMPACT STATS HEADER
// ============================================
@Composable
private fun CompactStatsHeader(
    activeCount: Int,
    completedCount: Int,
    todayRevenue: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CompactStat(
                    count = activeCount,
                    label = "Activos",
                    icon = Icons.Outlined.Receipt,
                    color = Color(0xFF1C49C0),
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                CompactStat(
                    count = completedCount,
                    label = "Completados",
                    icon = Icons.Outlined.CheckCircle,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            // Revenue Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ingresos de Hoy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format("%.2f", todayRevenue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
// SEARCH BAR
// ============================================
@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        placeholder = {
            Text("Buscar por número o producto...")
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Limpiar",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

// ============================================
// MINIMAL FILTER TABS
// ============================================
@Composable
private fun MinimalFilterTabs(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    allCount: Int,
    activeCount: Int,
    completedCount: Int
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

        if (activeCount > 0) {
            Tab(
                selected = selectedFilter == FilterType.Active,
                onClick = { onFilterSelected(FilterType.Active) },
                text = {
                    Text(
                        "Activos ($activeCount)",
                        fontWeight = if (selectedFilter == FilterType.Active)
                            FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }

        if (completedCount > 0) {
            Tab(
                selected = selectedFilter == FilterType.Completed,
                onClick = { onFilterSelected(FilterType.Completed) },
                text = {
                    Text(
                        "Completados ($completedCount)",
                        fontWeight = if (selectedFilter == FilterType.Completed)
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
    orderWithDetails: OrderWithDetails,
    onViewDetails: () -> Unit
) {
    val order = orderWithDetails.order
    val items = orderWithDetails.items
    val statusColor = getStatusColor(order.status)

    Card(
        onClick = onViewDetails,
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
            // Header: Order # + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.orderNumber}",
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
                            text = order.status.toDisplayString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            // Info Row
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
                        Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${items.size} producto${if (items.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTime(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Action Button - Solo si necesita acción
            if (order.status in listOf(OrderStatus.PAID, OrderStatus.PREPARING)) {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = getActionButtonColor(order.status)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = getActionIcon(order.status),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getActionText(order.status),
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
private fun EmptyOrdersState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
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
                    if (hasFilters) Icons.Outlined.SearchOff
                    else Icons.Outlined.ShoppingBag,
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
                    text = if (hasFilters) {
                        "No se encontraron pedidos"
                    } else {
                        "No hay pedidos"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (hasFilters) {
                        "Intenta ajustar los filtros"
                    } else {
                        "Los pedidos aparecerán aquí"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (hasFilters) {
                Button(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FilterAltOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Limpiar Filtros")
                }
            }
        }
    }
}

// ============================================
// TIPOS DE FILTRO
// ============================================
private enum class FilterType {
    All, Active, Completed, Cancelled
}

// ============================================
// FUNCIONES AUXILIARES
// ============================================
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

private fun getActionButtonColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.PAID -> Color(0xFF1C49C0)
        OrderStatus.PREPARING -> Color(0xFF06B6D4)
        else -> Color(0xFF5558A3)
    }
}

private fun getActionIcon(status: OrderStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        OrderStatus.PAID -> Icons.Default.Restaurant
        OrderStatus.PREPARING -> Icons.Default.DeliveryDining
        else -> Icons.Default.Visibility
    }
}

private fun getActionText(status: OrderStatus): String {
    return when (status) {
        OrderStatus.PAID -> "Comenzar preparación"
        OrderStatus.PREPARING -> "Asignar delivery"
        else -> "Ver detalle"
    }
}

private fun formatTime(isoDateString: String): String {
    return try {
        val instant = Instant.parse(isoDateString)
        val now = Instant.now()
        val diff = now.epochSecond - instant.epochSecond
        val minutes = diff / 60
        val hours = diff / 3600
        val days = diff / 86400

        when {
            minutes < 1 -> "Ahora"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("dd/MM")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        "---"
    }
}

private fun isSameDay(isoDateString: String): Boolean {
    return try {
        val instant = Instant.parse(isoDateString)
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = java.time.LocalDate.now()
        date == today
    } catch (e: Exception) {
        false
    }
}