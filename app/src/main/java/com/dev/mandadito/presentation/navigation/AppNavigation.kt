package com.dev.mandadito.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dev.mandadito.presentation.viewmodels.auth.AuthViewModel
import com.dev.mandadito.presentation.screens.auth.WelcomeScreen
import com.dev.mandadito.presentation.screens.auth.LoginScreen
import com.dev.mandadito.presentation.screens.auth.RegisterScreen
import com.dev.mandadito.presentation.screens.client.*
import com.dev.mandadito.presentation.screens.delivery.*
import com.dev.mandadito.presentation.screens.seller.SellerHomeScreen
import com.dev.mandadito.presentation.screens.admin.AdminScaffold

@Composable
fun AppNavigation(
    sessionAlreadyChecked: Boolean = false,
    hasActiveSession: Boolean = false,
    userRoleString: String? = null
) {
    val composeContext = LocalContext.current
    val appContext = remember { composeContext.applicationContext as android.app.Application }

    // Convertir el string del rol a enum
    val userRole = remember(userRoleString) {
        userRoleString?.let { roleStr ->
            com.dev.mandadito.data.models.Role.values().find { it.value == roleStr }
        }
    }

    val authViewModel = remember {
        AuthViewModel(
            application = appContext,
            sessionAlreadyChecked = sessionAlreadyChecked,
            hasActiveSession = hasActiveSession,
            initialUserRole = userRole
        )
    }

    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Determinar destino inicial según el estado de sesión
    val startDestination = remember(uiState.isLoggedIn, uiState.userRole) {
        if (uiState.isLoggedIn && uiState.userRole != null) {
            when (uiState.userRole) {
                com.dev.mandadito.data.models.Role.CLIENT -> "client_home"
                com.dev.mandadito.data.models.Role.SELLER -> "seller_home"
                com.dev.mandadito.data.models.Role.DELIVERY -> "delivery_home"
                com.dev.mandadito.data.models.Role.ADMIN -> "admin_home"
                else -> "welcome"
            }
        } else {
            "welcome"
        }
    }

    // Usar key para forzar recreación del NavHost cuando cambia la sesión
    key(uiState.isLoggedIn, uiState.userRole) {
        val navController = rememberNavController()

        var shouldAutoNavigate by remember {
            mutableStateOf(!sessionAlreadyChecked || !hasActiveSession)
        }

        LaunchedEffect(uiState.isLoggedIn, uiState.userRole) {
            if (shouldAutoNavigate && uiState.isLoggedIn && uiState.userRole != null) {
                val currentRoute = navController.currentDestination?.route
                val isAuthScreen = currentRoute in listOf("welcome", "login", "register")

                if (isAuthScreen) {
                    val destination = when (uiState.userRole) {
                        com.dev.mandadito.data.models.Role.CLIENT -> "client_home"
                        com.dev.mandadito.data.models.Role.SELLER -> "seller_home"
                        com.dev.mandadito.data.models.Role.DELIVERY -> "delivery_home"
                        com.dev.mandadito.data.models.Role.ADMIN -> "admin_home"
                        else -> null
                    }

                    destination?.let {
                        navController.navigate(it) {
                            popUpTo(route = "welcome") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            } else if (!uiState.isLoggedIn) {
                shouldAutoNavigate = true
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            // ===========================
            // AUTENTICACIÓN
            // ===========================

            composable("welcome") {
                WelcomeScreen(
                    onLoginClick = { navController.navigate("login") },
                    onRegisterClick = { navController.navigate("register") },
                    onGoogleClick = {},
                    onFacebookClick = {},
                    navController = navController
                )
            }

            composable("login") {
                LaunchedEffect(Unit) { shouldAutoNavigate = false }
                LoginScreen(authViewModel = authViewModel, navController = navController)
            }

            composable("register") {
                RegisterScreen(authViewModel = authViewModel, navController = navController)
            }

            // ===========================
            // HOME POR ROL
            // ===========================

            composable("client_home") {
                ClientScaffold(navController)
            }

            composable("seller_home") {
                SellerHomeScreen(navController)
            }

            composable("delivery_home") {
                DeliveryScaffold(navController)
            }

            composable("admin_home") {
                AdminScaffold(navController)
            }

            // ===========================
            // NAVEGACIÓN DE CLIENTE (DINÁMICA)
            // ===========================

            // Lista de tiendas con productos
            composable(
                route = "client_store_products/{colmadoId}",
                arguments = listOf(navArgument("colmadoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val colmadoId = backStackEntry.arguments?.getString("colmadoId") ?: return@composable
                ClientStoreProductsScreen(
                    colmadoId = colmadoId,
                    navController = navController,
                    onProductSelected = { productId ->
                        navController.navigate("client_product_detail/$productId")
                    }
                )
            }

            // Detalle de producto
            composable(
                route = "client_product_detail/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                ClientProductDetailScreen(
                    productoId = productId,
                    navController = navController
                )
            }

            // ===========================
            // CONFIRMACIÓN DE PAGO
            // ===========================

            composable(
                route = "payment_confirmation?total={total}",
                arguments = listOf(navArgument("total") {
                    type = NavType.FloatType
                    defaultValue = 0f
                })
            ) { backStackEntry ->
                val total = backStackEntry.arguments?.getFloat("total")?.toDouble() ?: 0.0
                ClientPaymentConfirmationScreen(
                    navController = navController,
                    total = total,
                    onViewDetails = { navController.navigate("order_tracking/PED-123") },
                    onGoHome = {
                        navController.navigate("client_home") {
                            popUpTo("payment_confirmation") { inclusive = true }
                        }
                    }
                )
            }

            // ===========================
            // TRACKING DE PEDIDOS
            // ===========================

            composable(
                route = "order_tracking/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: "PED-123"
                ClientOrderTrackingScreen(
                    navController = navController,
                    orderId = orderId
                )
            }

            // ===========================
            // NAVEGACIÓN DE DELIVERY
            // ===========================

            // Pantalla de pedidos disponibles
            composable("delivery_orders") {
                DeliveryOrdersScreen(
                    navController = navController,
                    onOrderAccepted = { orderId ->
                        // Navegar a detalles del pedido cuando se acepta
                        navController.navigate("delivery_order_details/$orderId")
                    },
                    onOrderRejected = { orderId ->
                        // Manejar rechazo de pedido
                    }
                )
            }

            // Mis entregas en progreso
            composable("delivery_my_deliveries") {
                DeliveryMyDeliveriesScreen(
                    navController = navController,
                    onDeliverySelected = { orderId ->
                        navController.navigate("delivery_order_details/$orderId")
                    }
                )
            }

            // Historial de entregas
            composable("delivery_history") {
                DeliveryHistoryScreen(navController = navController)
            }

            // Detalles de un pedido específico
            composable(
                route = "delivery_order_details/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: "PED1"
                DeliveryOrderDetailsScreen(
                    navController = navController,
                    orderId = orderId
                )
            }

            // Tiempo estimado de entrega
            composable(
                route = "delivery_estimated_time/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: "PED1"
                DeliveryEstimatedTimeScreen(
                    navController = navController,
                    orderId = orderId
                )
            }

            // Confirmación de pago del delivery
            composable(
                route = "delivery_payment_confirmation/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: "PED1"
                DeliveryPaymentConfirmationScreen(
                    navController = navController,
                    orderId = orderId
                )
            }

        } // Fin NavHost
    } // Fin key()
}