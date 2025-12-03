package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScaffold(navController: NavController) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mandadito",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                authRepository.logout()
                                navController.navigate("welcome") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Inicio"
                            )
                        },
                        label = { Text("Inicio") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Carrito"
                            )
                        },
                        label = { Text("Carrito") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil"
                            )
                        },
                        label = { Text("Perfil") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> ClientHomeScreen(
                    navController = navController,
                    onStoreSelected = { colmadoId ->
                        navController.navigate("client_store_products/$colmadoId")
                    }
                )
                1 -> CartPlaceholder()
                2 -> ClientProfileScreen()
            }
        }
    }
}

@Composable
private fun CartPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Carrito de Compras",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Próximamente disponible",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}