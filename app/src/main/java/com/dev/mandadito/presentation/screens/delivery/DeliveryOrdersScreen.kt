package com.dev.mandadito.presentation.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            Order("PED1", "Colmado Rey", "5 km", "nuevo", 156.0),
            Order("PED2", "Colmado El Men", "3 km", "preparado", 376.0),
            Order("PED3", "Colmado La Esquina", "4 km", "listo", 196.0)
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
            // Filtros
            TabRow(
                selectedTabIndex = when (selectedFilter) {
                    "nuevo" -> 0
                    "preparado" -> 1
                    else -> 2
                }
            ) {
                Tab(
                    selected = selectedFilter == "nuevo",
                    onClick = { selectedFilter = "nuevo" },
                    text = { Text("Nuevos") }
                )
                Tab(
                    selected = selectedFilter == "preparado",
                    onClick = { selectedFilter = "preparado" },
                    text = { Text("Preparados") }
                )
                Tab(
                    selected = selectedFilter == "listo",
                    onClick = { selectedFilter = "listo" },
                    text = { Text("Listos") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid de pedidos
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOrderSelected(order.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = order.colmado.take(1),
                                    modifier = Modifier.align(Alignment.Center),
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = order.colmado,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = order.distance,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "RD$ ${order.total}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}