package com.mandadito.components.seller

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.Inventory
import com.dev.mandadito.data.models.InventoryStats
import com.dev.mandadito.data.repository.InventoryRepository
import com.dev.mandadito.presentation.screens.seller.components.AddInventoryDialog
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    sellerId: String,
    sellerName: String,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repository = remember { InventoryRepository() }

    var inventoryList by remember { mutableStateOf<List<Inventory>>(emptyList()) }
    var stats by remember { mutableStateOf<InventoryStats?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sellerId) {
        isLoading = true
        scope.launch {
            repository.getInventoryBySeller(sellerId).onSuccess {
                inventoryList = it
            }
            repository.getInventoryStats(sellerId).onSuccess {
                stats = it
            }
            isLoading = false
        }
    }

    val filteredList = inventoryList.filter {
        it.productName.contains(searchQuery, ignoreCase = true) ||
                (it.category?.contains(searchQuery, ignoreCase = true) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Inventario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Agregar producto")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Agregar")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Estadísticas rápidas
            stats?.let {
                InventoryStatsRow(stats = it)
            }

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar productos...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(filteredList) { item ->
                        InventoryItemCard(
                            inventory = item,
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddInventoryDialog(
                sellerId = sellerId,
                sellerName = sellerName,
                onDismiss = { showAddDialog = false },
                onSuccess = {
                    showAddDialog = false
                    // Recargar lista
                    scope.launch {
                        repository.getInventoryBySeller(sellerId).onSuccess {
                            inventoryList = it
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun InventoryStatsRow(stats: InventoryStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatCard("Productos", stats.totalProducts.toString(), MaterialTheme.colorScheme.primary)
        StatCard("Valor Total", "$${stats.totalValue}", MaterialTheme.colorScheme.secondary)
        StatCard("Bajo Stock", stats.lowStockCount.toString(), MaterialTheme.colorScheme.error)
        StatCard("Sin Stock", stats.outOfStockCount.toString(), MaterialTheme.colorScheme.error)
    }
}

@Composable
fun StatCard(title: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun InventoryItemCard(inventory: Inventory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(inventory.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                inventory.category?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Text("Stock: ${inventory.currentStock}", style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$${inventory.unitPrice}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                val statusColor = when (inventory.status) {
                    "OUT_OF_STOCK" -> MaterialTheme.colorScheme.error
                    "LOW_STOCK" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                Text(inventory.status.replace("_", " "), color = statusColor)
            }
        }
    }
}