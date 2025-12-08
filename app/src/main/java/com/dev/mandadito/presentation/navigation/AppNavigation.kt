package com.dev.mandadito.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
fun AppNavigation() {
    val navController = rememberNavController()
    val composeContext = LocalContext.current
    val appContext = remember { composeContext.applicationContext as android.app.Application }
    val authViewModel = remember { AuthViewModel(appContext) }

    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = "welcome"
    var shouldAutoNavigate by remember { mutableStateOf(true) }

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

        // Confirmación de pago
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

        // Tracking de pedidos
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
        // NAVEGACIÓN DE DELIVERY (DINÁMICA)
        // ===========================

        // Detalle de pedido para delivery
        composable(
            route = "delivery_order_details/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            DeliveryOrderDetailsScreen(
                navController = navController,
                orderId = orderId
            )
        }

        // Pantalla de pedidos activos del delivery
        composable("delivery_orders") {
            DeliveryOrdersScreen(navController = navController)
        }

        // Historial de entregas
        composable("delivery_history") {
            DeliveryHistoryScreen(navController = navController)
        }

        // Tiempo estimado de entrega
        composable(
            route = "delivery_estimated_time/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            DeliveryEstimatedTimeScreen(
                navController = navController,
                orderId = orderId
            )
        }

        // Confirmación de pago de delivery
        composable(
            route = "delivery_payment_confirmation/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            DeliveryPaymentConfirmationScreen(
                navController = navController,
                orderId = orderId
            )
        }
    }
}