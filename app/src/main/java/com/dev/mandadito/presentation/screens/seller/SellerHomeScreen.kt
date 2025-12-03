package com.dev.mandadito.presentation.screens.seller

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dev.mandadito.data.network.AuthRepository
import com.dev.mandadito.presentation.viewmodels.seller.CategoryViewModel
import com.dev.mandadito.presentation.viewmodels.seller.DeliveriesViewModel
import com.dev.mandadito.presentation.viewmodels.seller.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerHomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Mantener los ViewModels aquí evita recargas al cambiar de pestañas
    val productViewModel = remember(context) { ProductViewModel(context) }
    val categoryViewModel = remember(context) { CategoryViewModel(context) }
    val deliveriesViewModel = remember(context) { DeliveriesViewModel(context) }

    val onLogout: () -> Unit = {
        coroutineScope.launch {
            authRepository.logout()
            navController.navigate("welcome") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Header del drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Mandadito",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Panel del Vendedor",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Opciones del menú
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("Productos") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    label = { Text("Categorías") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DeliveryDining, contentDescription = null) },
                    label = { Text("Deliveries") },
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Yo") },
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Opción de cerrar sesión
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            onLogout()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                when (selectedTab) {
                                    0 -> "Panel del Vendedor"
                                    1 -> "Productos"
                                    2 -> "Categorías"
                                    3 -> "Deliveries"
                                    4 -> "Mi Perfil"
                                    else -> "Panel del Vendedor"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedTab == 0) {
                                Text(
                                    "Bienvenido de vuelta",
                                    style = MaterialTheme.typography.bodySmall,
                                    //color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú"
                            )
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(targetState = selectedTab, label = "seller_tabs") { tab ->
                    when (tab) {
                        0 -> SellerDashboardScreen()
                        1 -> SellerProductsScreen(viewModel = productViewModel)
                        2 -> SellerCategoriesScreen(viewModel = categoryViewModel)
                        3 -> SellerDeliveriesScreen(viewModel = deliveriesViewModel)
                        4 -> SellerProfileScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerDashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Sección de estadísticas
        Text(
            text = "Resumen del Negocio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Grid de tarjetas estadísticas
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Productos",
                    value = "0",
                    subtitle = "En catálogo",
                    icon = Icons.Outlined.ShoppingBag,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Categorías",
                    value = "0",
                    subtitle = "Activas",
                    icon = Icons.Default.Category,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Stock Total",
                    value = "0",
                    subtitle = "Unidades",
                    icon = Icons.Outlined.Inventory2,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Ventas",
                    value = "$0.00",
                    subtitle = "Este mes",
                    icon = Icons.Outlined.MonetizationOn,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Acciones rápidas
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Agregar Producto",
                description = "Añade un nuevo producto a tu catálogo",
                icon = Icons.Default.ShoppingCart,
                onClick = { /* Navegar a productos */ }
            )

            QuickActionCard(
                title = "Crear Categoría",
                description = "Organiza tus productos en categorías",
                icon = Icons.Default.Category,
                onClick = { /* Navegar a categorías */ }
            )

            QuickActionCard(
                title = "Ver Reportes",
                description = "Analiza el rendimiento de tu negocio",
                icon = Icons.Default.TrendingUp,
                onClick = { /* Próximamente */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = contentColor
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}