package com.dev.mandadito.presentation.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class Order(
    val id: String,
    val colmado: String,
    val distance: String,
    val status: String,  // "nuevo", "preparado", "listo"
    val total: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrdersScreen(
    navController: NavController,
    onOrderSelected: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("nuevo") }

    val orders = remember {
        listOf(
            Order("PED001", "Colmado Rey", "2.5 km", "nuevo", 1256.50),
            Order("PED002", "Colmado El Men", "1.8 km", "nuevo", 876.00),
            Order("PED003", "Colmado La Esquina", "3.2 km", "preparado", 1596.50),
            Order("PED004", "Super Colmado", "1.2 km", "preparado", 2341.00),
            Order("PED005", "Colmado Central", "4.5 km", "listo", 956.00),
            Order("PED006", "Mini Market", "2.1 km", "listo", 1876.50)
        )
    }

    val filteredOrders = orders.filter { it.status == selectedFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedidos disponibles") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "nuevo",
                    onClick = { selectedFilter = "nuevo" },
                    label = {
                        Text(
                            "Nuevos",
                            fontWeight = if (selectedFilter == "nuevo") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (selectedFilter == "nuevo") {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                )

                FilterChip(
                    selected = selectedFilter == "preparado",
                    onClick = { selectedFilter = "preparado" },
                    label = {
                        Text(
                            "Preparados",
                            fontWeight = if (selectedFilter == "preparado") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (selectedFilter == "preparado") {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                )

                FilterChip(
                    selected = selectedFilter == "listo",
                    onClick = { selectedFilter = "listo" },
                    label = {
                        Text(
                            "Listos",
                            fontWeight = if (selectedFilter == "listo") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (selectedFilter == "listo") {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                )
            }

            // Count info
            Text(
                text = "${filteredOrders.size} pedidos disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Orders List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(filteredOrders) { order ->
                    OrderCard(
                        order = order,
                        onClick = { onOrderSelected(order.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    val statusColor = when (order.status) {
        "nuevo" -> MaterialTheme.colorScheme.errorContainer
        "preparado" -> MaterialTheme.colorScheme.secondaryContainer
        "listo" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusTextColor = when (order.status) {
        "nuevo" -> MaterialTheme.colorScheme.onErrorContainer
        "preparado" -> MaterialTheme.colorScheme.onSecondaryContainer
        "listo" -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (order.status) {
        "nuevo" -> "Nuevo"
        "preparado" -> "Preparado"
        "listo" -> "Listo para entrega"
        else -> order.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = order.colmado,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pedido #${order.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = statusTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Distance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.distance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Total
                Text(
                    text = "RD$ ${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
