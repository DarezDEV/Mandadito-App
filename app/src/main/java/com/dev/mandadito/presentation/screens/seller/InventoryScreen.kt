package com.dev.mandadito.presentation.screens.seller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.presentation.viewmodels.seller.ProductViewModel
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.utils.SharedPreferenHelper
import com.dev.mandadito.utils.NotificationHelper


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { ProductViewModel(context) }
    val sharedPrefs = remember { SharedPreferenHelper(context) }
    val colmadoId = sharedPrefs.getColmadoId()

    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Obtener productos del estado
    val myProducts = remember(uiState.productsState, colmadoId) {
        when (val state = uiState.productsState) {
            is UiState.Success -> {
                if (colmadoId != null) {
                    state.data.filter { it.colmadoId == colmadoId }
                } else {
                    state.data
                }
            }
            else -> emptyList()
        }
    }

    // NOTIFICACIONES LOCALES: Chequear stock y mostrar si es necesario
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val notificationHelper = NotificationHelper(context)
            val lowStockItems = myProducts.filter { it.stock <= it.minStock && it.stock > 0 }.map { it.name }
            val outOfStockItems = myProducts.filter { it.stock == 0 }.map { it.name }
            notificationHelper.showLowStockNotification(lowStockItems)
            notificationHelper.showOutOfStockNotification(outOfStockItems)
        }
    }

    LaunchedEffect(myProducts) {
        val lastShown = sharedPrefs.getLong("last_stock_notification", 0L)

        if (System.currentTimeMillis() - lastShown < 3600000) {
            return@LaunchedEffect
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@LaunchedEffect
            }
        }

        val notificationHelper = NotificationHelper(context)
        val lowStockItems = myProducts.filter { it.stock <= it.minStock && it.stock > 0 }.map { it.name }
        val outOfStockItems = myProducts.filter { it.stock == 0 }.map { it.name }
        notificationHelper.showLowStockNotification(lowStockItems)
        notificationHelper.showOutOfStockNotification(outOfStockItems)

        sharedPrefs.putLong("last_stock_notification", System.currentTimeMillis())
    }

    // Calcular estadísticas
    val stats = remember(myProducts) {
        InventoryStats(
            totalProducts = myProducts.size,
            totalValue = myProducts.sumOf { it.price * it.stock },
            lowStockCount = myProducts.count { it.stock <= it.minStock && it.stock > 0 },
            outOfStockCount = myProducts.count { it.stock == 0 }
        )
    }

    val filteredList = remember(myProducts, searchQuery) {
        myProducts.filter { product ->
            product.name.contains(searchQuery, ignoreCase = true) ||
                    product.description?.contains(searchQuery, ignoreCase = true) == true ||
                    product.categories.any { cat -> cat.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val isLoading = uiState.productsState is UiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Inventario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Estadísticas rápidas
            InventoryStatsRow(stats = stats)

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

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                filteredList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No hay productos" else "No se encontraron productos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn {
                        items(filteredList) { product ->
                            ProductInventoryCard(
                                product = product,
                                onClick = { onNavigateToDetail(product.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class InventoryStats(
    val totalProducts: Int,
    val totalValue: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int
)

@Composable
fun InventoryStatsRow(stats: InventoryStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "Productos",
            value = stats.totalProducts.toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Valor",
            value = "$${String.format("%.0f", stats.totalValue)}",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Bajo",
            value = stats.lowStockCount.toString(),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Agotado",
            value = stats.outOfStockCount.toString(),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ProductInventoryCard(product: ProductWithCategories, onClick: () -> Unit) {
    val stockStatus = when {
        product.stock == 0 -> "SIN STOCK"
        product.stock <= product.minStock -> "STOCK BAJO"
        else -> "EN STOCK"
    }

    val statusColor = when {
        product.stock == 0 -> MaterialTheme.colorScheme.error
        product.stock <= product.minStock -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

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
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (product.categories.isNotEmpty()) {
                    Text(
                        product.categories.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    "Stock: ${product.stock}${if (product.minStock > 0) " (Mín: ${product.minStock})" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stockStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}