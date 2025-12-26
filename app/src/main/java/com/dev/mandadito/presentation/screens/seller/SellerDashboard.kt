package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.dev.mandadito.presentation.viewmodels.seller.ProductViewModel
import com.dev.mandadito.presentation.viewmodels.seller.CategoryViewModel
import com.dev.mandadito.utils.SharedPreferenHelper

@Composable
fun SellerDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val productViewModel = remember { ProductViewModel(context) }
    val categoryViewModel = remember { CategoryViewModel(context) }
    val sharedPrefs = remember { SharedPreferenHelper(context) }
    val colmadoId = sharedPrefs.getColmadoId()

    val productUiState by productViewModel.uiState.collectAsState()
    val categoryUiState by categoryViewModel.uiState.collectAsState()

    // Calcular estadísticas reales
    val myProducts = remember(productUiState.products, colmadoId) {
        // Los productos ya vienen filtrados por colmado desde el ViewModel
        productUiState.products
    }

    val stats = remember(myProducts, categoryUiState.categories) {
        DashboardStats(
            totalProducts = myProducts.size,
            totalCategories = categoryUiState.categories.size,
            totalStock = myProducts.sumOf { it.stock },
            totalSales = 0.0 // TODO: Implementar ventas reales
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Resumen del Negocio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Productos",
                    value = stats.totalProducts.toString(),
                    subtitle = "En catálogo",
                    icon = Icons.Outlined.ShoppingBag,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Categorías",
                    value = stats.totalCategories.toString(),
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
                    value = stats.totalStock.toString(),
                    subtitle = "Unidades",
                    icon = Icons.Outlined.Inventory2,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Ventas",
                    value = "$${String.format("%.2f", stats.totalSales)}",
                    subtitle = "Este mes",
                    icon = Icons.Outlined.MonetizationOn,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                title = "Ver Inventario",
                description = "Administra tu stock de productos",
                icon = Icons.Outlined.Inventory2,
                onClick = {
                    navController.navigate("seller_inventory")
                }
            )

            QuickActionCard(
                title = "Agregar Producto",
                description = "Añade un nuevo producto a tu catálogo",
                icon = Icons.Default.ShoppingCart,
                onClick = { /* TODO: Abrir diálogo o navegar */ }
            )

            QuickActionCard(
                title = "Crear Categoría",
                description = "Organiza tus productos en categorías",
                icon = Icons.Default.Category,
                onClick = { /* TODO: Abrir diálogo o navegar */ }
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

// Data class para estadísticas del dashboard
private data class DashboardStats(
    val totalProducts: Int,
    val totalCategories: Int,
    val totalStock: Int,
    val totalSales: Double
)

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