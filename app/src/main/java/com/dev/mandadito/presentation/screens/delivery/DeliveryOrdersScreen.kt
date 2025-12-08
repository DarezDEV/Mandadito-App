package com.dev.mandadito.presentation.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class OrderCard(
    val id: String,
    val colmado: String,
    val distance: String,
    val status: OrderStatus,
    val total: Double,
    val duration: String
)

enum class OrderStatus {
    NUEVO,      // Needs accept/reject
    ACEPTADO,   // Accepted, ready to pick up
    EN_PROCESO  // Being prepared
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrdersScreen(
    navController: NavController,
    onOrderAccepted: (String) -> Unit = {},
    onOrderRejected: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(OrderStatus.NUEVO) }

    val orders = remember {
        listOf(
            OrderCard("Pedido #1", "Colmado Rey", "5 km", OrderStatus.NUEVO, 156.0, "2 minutos"),
            OrderCard("Pedido #2", "Colmado El Men", "3 km", OrderStatus.ACEPTADO, 376.0, "5 minutos"),
            OrderCard("Pedido #3", "Colmado La Esquina", "4 km", OrderStatus.ACEPTADO, 196.0, "3 minutos"),
            OrderCard("Pedido #4", "Colmado Rosa", "2.5 km", OrderStatus.EN_PROCESO, 222.0, "12 minutos"),
            OrderCard("Pedido #5", "Colmado Azul", "1.5 km", OrderStatus.EN_PROCESO, 322.0, "8 minutos")
        )
    }

    val filteredOrders = orders.filter { it.status == selectedFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedidos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Tabs
            TabRow(
                selectedTabIndex = when (selectedFilter) {
                    OrderStatus.NUEVO -> 0
                    OrderStatus.ACEPTADO -> 1
                    OrderStatus.EN_PROCESO -> 2
                }
            ) {
                Tab(
                    selected = selectedFilter == OrderStatus.NUEVO,
                    onClick = { selectedFilter = OrderStatus.NUEVO },
                    text = { Text("Nuevos") }
                )
                Tab(
                    selected = selectedFilter == OrderStatus.ACEPTADO,
                    onClick = { selectedFilter = OrderStatus.ACEPTADO },
                    text = { Text("Preparados") }
                )
                Tab(
                    selected = selectedFilter == OrderStatus.EN_PROCESO,
                    onClick = { selectedFilter = OrderStatus.EN_PROCESO },
                    text = { Text("Listos") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Orders Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    OrderGridCard(
                        order = order,
                        onAccept = { onOrderAccepted(order.id) },
                        onReject = { onOrderRejected(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderGridCard(
    order: OrderCard,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (order.status) {
                            OrderStatus.NUEVO -> MaterialTheme.colorScheme.surfaceVariant
                            OrderStatus.ACEPTADO -> MaterialTheme.colorScheme.primary
                            OrderStatus.EN_PROCESO -> MaterialTheme.colorScheme.tertiary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (order.status) {
                    OrderStatus.NUEVO -> {
                        Text(
                            text = order.colmado.take(1),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OrderStatus.ACEPTADO -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    OrderStatus.EN_PROCESO -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Order ID
            Text(
                text = order.id,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Colmado Name
            Text(
                text = order.colmado,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            // Distance
            Text(
                text = order.distance,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Duration
            Text(
                text = order.duration,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Total
            Text(
                text = "RD$ ${order.total.toInt()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons based on status
            when (order.status) {
                OrderStatus.NUEVO -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Rechazar", fontSize = 11.sp)
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Aceptar", fontSize = 11.sp)
                        }
                    }
                }
                OrderStatus.ACEPTADO -> {
                    Button(
                        onClick = { /* Navigate to order detail */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recoger", fontSize = 12.sp)
                    }
                }
                OrderStatus.EN_PROCESO -> {
                    OutlinedButton(
                        onClick = { /* Check status */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Produciendo", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}