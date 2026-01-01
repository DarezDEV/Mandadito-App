package com.dev.mandadito.presentation.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryMyProfileScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Header con avatar y nombre
        ProfileHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // Estadísticas del delivery
        DeliveryStats()

        Spacer(modifier = Modifier.height(24.dp))

        // Opciones del perfil
        ProfileOptions(navController)
    }
}

@Composable
private fun ProfileHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Nombre
        Text(
            text = "Juan Pérez",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Email
        Text(
            text = "juan.perez@delivery.com",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Estado
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = "Disponible",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DeliveryStats() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Estadísticas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "145",
                    label = "Entregas"
                )

                StatItem(
                    icon = Icons.Default.Star,
                    value = "4.8",
                    label = "Rating"
                )

                StatItem(
                    icon = Icons.Default.Timer,
                    value = "18 min",
                    label = "Promedio"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileOptions(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ProfileOptionItem(
            icon = Icons.Default.Person,
            title = "Editar Perfil",
            onClick = { /* TODO: Navegar a editar perfil */ }
        )

        ProfileOptionItem(
            icon = Icons.Default.DirectionsCar,
            title = "Información del Vehículo",
            onClick = { /* TODO: Navegar a vehículo */ }
        )

        ProfileOptionItem(
            icon = Icons.Default.Notifications,
            title = "Notificaciones",
            onClick = { /* TODO: Navegar a notificaciones */ }
        )

        ProfileOptionItem(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Métodos de Pago",
            onClick = { /* TODO: Navegar a pagos */ }
        )

        ProfileOptionItem(
            icon = Icons.Default.History,
            title = "Historial de Ganancias",
            onClick = { /* TODO: Navegar a ganancias */ }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProfileOptionItem(
            icon = Icons.Default.Help,
            title = "Ayuda y Soporte",
            onClick = { /* TODO: Navegar a ayuda */ }
        )

        ProfileOptionItem(
            icon = Icons.Default.Info,
            title = "Acerca de",
            onClick = { /* TODO: Navegar a acerca de */ }
        )
    }
}

@Composable
private fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}