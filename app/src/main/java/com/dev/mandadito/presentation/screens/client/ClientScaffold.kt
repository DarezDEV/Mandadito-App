package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.dev.mandadito.data.repository.AuthRepository
import com.dev.mandadito.presentation.viewmodels.client.ClientScaffoldViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScaffold(navController: NavHostController) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val viewModel: ClientScaffoldViewModel = viewModel()
    val selectedTab by viewModel.selectedTab.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Mandadito",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showLogoutDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1C49C0),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF1A1B1F),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Inicio"
                        )
                    },
                    label = {
                        Text(
                            text = "Inicio",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = selectedTab == 0,
                    onClick = { viewModel.updateSelectedTab(0) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1C49C0),
                        selectedTextColor = Color(0xFF1C49C0),
                        indicatorColor = Color(0xFFD8E2FF),
                        unselectedIconColor = Color(0xFF75777F),
                        unselectedTextColor = Color(0xFF75777F)
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                            contentDescription = "Carrito"
                        )
                    },
                    label = {
                        Text(
                            text = "Carrito",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = selectedTab == 1,
                    onClick = {
                        navController.navigate("client_cart")
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1C49C0),
                        selectedTextColor = Color(0xFF1C49C0),
                        indicatorColor = Color(0xFFD8E2FF),
                        unselectedIconColor = Color(0xFF75777F),
                        unselectedTextColor = Color(0xFF75777F)
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Perfil"
                        )
                    },
                    label = {
                        Text(
                            text = "Perfil",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = selectedTab == 2,
                    onClick = { viewModel.updateSelectedTab(2) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1C49C0),
                        selectedTextColor = Color(0xFF1C49C0),
                        indicatorColor = Color(0xFFD8E2FF),
                        unselectedIconColor = Color(0xFF75777F),
                        unselectedTextColor = Color(0xFF75777F)
                    )
                )
            }
        },
        containerColor = Color(0xFFF5F7FA)
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
                2 -> ClientProfileScreen(navController = navController)
            }
        }
    }

    // Diálogo de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFF1C49C0),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1B1F)
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas cerrar sesión?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF44464F)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                            navController.navigate("welcome") {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBA1A1A)
                    )
                ) {
                    Text(
                        text = "Cerrar sesión",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        text = "Cancelar",
                        color = Color(0xFF44464F),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}