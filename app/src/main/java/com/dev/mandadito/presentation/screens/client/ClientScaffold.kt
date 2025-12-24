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
                        onClick = { viewModel.updateSelectedTab(0) }
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
                        onClick = { viewModel.updateSelectedTab(1) }
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
                        onClick = { viewModel.updateSelectedTab(2) }
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
                1 -> ClientCartScreen(navController = navController)
                2 -> ClientProfileScreen(navController = navController)
            }
        }
    }
}
