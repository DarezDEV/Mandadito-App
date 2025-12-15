package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.repository.*
import kotlinx.coroutines.launch
import java.util.Locale

data class DashboardCard(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboard(
    sellerId: String,
    sellerName: String,
    onNavigateToInventory: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // Repositorios
    val inventoryRepo = remember { InventoryRepository() }
    val notificationRepo = remember { NotificationRepository() }

    // Estados
    var inventoryStats by remember { mutableStateOf<InventoryStats?>(null) }
    var unreadNotifications by remember { mutableIntStateOf(0) }
    var recentNotifications by remember { mutableStateOf(emptyList<Notification>()) }
    var lowStockProducts by remember { mutableStateOf(emptyList<Inventory>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar datos
    LaunchedEffect(sellerId) {
        isLoading = true
        scope.launch {
            // Estadísticas de inventario
            inventoryRepo.getInventoryStats(sellerId).onSuccess {
                inventoryStats = it
            }

            // Notificaciones no leídas
            notificationRepo.getUnreadCount(sellerId).onSuccess {
                unreadNotifications = it
            }

            // Notificaciones recientes
            notificationRepo.getUserNotifications(sellerId, limit = 5).onSuccess {
                recentNotifications = it
            }

            // Productos con stock bajo
            inventoryRepo.getLowStockProducts(sellerId).onSuccess {
                lowStockProducts = it
            }

            isLoading = false
        }
    }

    // Tarjetas del dashboard
    val dashboardCards = listOf(
        DashboardCard(
            "Inventario",
            Icons.Default.Inventory,
            Color(0xFF2196F3),
            "inventory"
        ),
        DashboardCard(
            "Productos",
            Icons.Default.ShoppingBag,
            Color(0xFF4CAF50),
            "products"
        ),
        DashboardCard(
            "Categorías",
            Icons.Default.Category,
            Color(0xFFFF9800),
            "categories"
        ),
        DashboardCard(
            "Usuarios",
            Icons.Default.People,
            Color(0xFF9C27B0),
            "users"
        ),
        DashboardCard(
            "Pedidos",
            Icons.Default.ShoppingCart,
            Color(0xFFF44336),
            "orders"
        ),
        DashboardCard(
            "Reportes",
            Icons.Default.Assessment,
            Color(0xFF00BCD4),
            "reports"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("¡Hola, $sellerName!")
                        Text(
                            text = "Panel de Control",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    // Notificaciones
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge {
                                    Text(unreadNotifications.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, "Notificaciones")
                        }
                    }

                    // Menú
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Más opciones")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Configuración") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Mi Perfil") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                showMenu = false
                                onLogout()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Estadísticas rápidas
                item {
                    inventoryStats?.let { stats ->
                        QuickStatsCard(stats = stats)
                    }
                }

                // Alertas importantes
                if (lowStockProducts.isNotEmpty()) {
                    item {
                        AlertCard(
                            title = "Stock Bajo",
                            message = "${lowStockProducts.size} productos necesitan reabastecimiento",
                            icon = Icons.Default.Warning,
                            color = Color(0xFFFF9800),
                            onClick = onNavigateToInventory
                        )
                    }
                }

                // Acciones rápidas
                item {
                    Text(
                        text = "Acceso Rápido",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(400.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dashboardCards) { card ->
                            DashboardActionCard(
                                card = card,
                                onClick = {
                                    when (card.route) {
                                        "inventory" -> onNavigateToInventory()
                                        "products" -> onNavigateToProducts()
                                        "categories" -> onNavigateToCategories()
                                        "users" -> onNavigateToUsers()
                                        "orders" -> onNavigateToOrders()
                                    }
                                }
                            )
                        }
                    }
                }

                // Notificaciones recientes
                if (recentNotifications.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notificaciones Recientes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToNotifications) {
                                Text("Ver todas")
                            }
                        }
                    }

                    items(recentNotifications.take(3)) { notification ->
                        NotificationPreviewCard(
                            notification = notification,
                            onClick = onNavigateToNotifications
                        )
                    }
                }

                // Productos con stock bajo
                if (lowStockProducts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Productos con Stock Bajo",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToInventory) {
                                Text("Ver todos")
                            }
                        }
                    }

                    items(lowStockProducts.take(3)) { product ->
                        LowStockProductCard(
                            product = product,
                            onClick = onNavigateToInventory
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(stats: InventoryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Resumen de Inventario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatItem(
                    label = "Productos",
                    value = stats.totalProducts.toString(),
                    icon = Icons.Default.Inventory,
                    color = MaterialTheme.colorScheme.primary
                )
                QuickStatItem(
                    label = "Valor Total",
                    value = "$${String.format(Locale.US, "%.0f", stats.totalValue)}",
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFF4CAF50)
                )
                QuickStatItem(
                    label = "Alertas",
                    value = (stats.lowStockCount + stats.outOfStockCount).toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun QuickStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun DashboardActionCard(
    card: DashboardCard,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = card.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(card.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    card.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = card.color
            )
        }
    }
}

@Composable
fun AlertCard(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = color
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color
            )
        }
    }
}

@Composable
fun NotificationPreviewCard(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.Top)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LowStockProductCard(
    product: Inventory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFFFF9800)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Stock: ${product.currentStock} (Mínimo: ${product.minStock})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}