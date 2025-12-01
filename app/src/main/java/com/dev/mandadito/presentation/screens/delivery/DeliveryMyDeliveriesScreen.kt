package com.dev.mandadito.presentation.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryMyDeliveriesScreen(
    navController: NavController,
    onDeliverySelected: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }  // 0: Todos, 1: Pendientes, 2: En camino

    val tabs = listOf("Todos", "Pendientes", "En camino")

    val deliveries = remember {
        listOf(
            Delivery("PED1", "Colmado Rey", 156.0, "En camino"),
            Delivery("PED2", "Colmado El Men", 376.0, "Pendiente"),
            Delivery("PED3", "Colmado La Esquina", 196.0, "En camino")
        )
    }

    val filteredDeliveries = deliveries.filter { delivery ->
        when (selectedTab) {
            0 -> true  // Todos
            1 -> delivery.status == "Pendiente"
            2 -> delivery.status == "En camino"
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis entregas") },
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDeliveries) { delivery ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeliverySelected(delivery.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = delivery.colmado.take(1),
                                    modifier = Modifier.align(Alignment.Center),
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = delivery.colmado,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "RD$ ${delivery.total}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = delivery.status,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class Delivery(
    val id: String,
    val colmado: String,
    val total: Double,
    val status: String
)