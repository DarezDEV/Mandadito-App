package com.dev.mandadito.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.dev.mandadito.presentation.screens.seller.OrderDetailScreen
import com.dev.mandadito.presentation.screens.seller.StripeOnboardingScreen
import com.dev.mandadito.presentation.viewmodels.seller.StripeOnboardingViewModel
import com.dev.mandadito.presentation.viewmodels.client.ProductReviewsViewModel
import com.dev.mandadito.presentation.screens.admin.AdminScaffold
import com.dev.mandadito.presentation.navigation.addressNavGraph
import kotlinx.coroutines.launch

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
            com.dev.mandadito.data.models.Role.entries.find { it.value == roleStr }
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

    // Extraer el userId para usarlo en las rutas
    val currentUserId = uiState.userRole as? String

    // Determinar destino inicial según el estado de sesión
    val startDestination = remember(uiState.isLoggedIn, uiState.userRole, uiState.stripeConfigured) {
        if (uiState.isLoggedIn && uiState.userRole != null) {
            when (uiState.userRole) {
                com.dev.mandadito.data.models.Role.CLIENT -> "client_home"
                com.dev.mandadito.data.models.Role.SELLER -> {
                    // Para sellers con sesión previa, ir directo al home
                    // (la verificación de Stripe solo se hace en login activo)
                    "seller_home"
                }
                com.dev.mandadito.data.models.Role.DELIVERY -> "delivery_home"
                com.dev.mandadito.data.models.Role.ADMIN -> "admin_home"
                else -> "welcome"
            }
        } else {
            "welcome"
        }
    }

    // Usar key para forzar recreación del NavHost cuando cambia la sesión
    // Esto asegura que el startDestination se respete correctamente
    key(uiState.isLoggedIn, uiState.userRole) {
        val navController = rememberNavController()

        // Solo auto-navegar si el usuario acaba de iniciar sesión durante la sesión actual
        // No auto-navegar cuando la app inicia con sesión ya establecida
        var shouldAutoNavigate by remember {
            mutableStateOf(!sessionAlreadyChecked || !hasActiveSession)
        }

        LaunchedEffect(uiState.isLoggedIn, uiState.userRole, uiState.stripeConfigured) {
            // Solo navegar automáticamente si:
            // 1. shouldAutoNavigate es true (no venimos de SplashActivity con sesión)
            // 2. El usuario está logueado
            // 3. Tenemos un rol válido
            // 4. Estamos en una pantalla de autenticación
            if (shouldAutoNavigate && uiState.isLoggedIn && uiState.userRole != null) {
                val currentRoute = navController.currentDestination?.route
                val isAuthScreen = currentRoute in listOf("welcome", "login", "register")

                if (isAuthScreen) {
                    val destination = when (uiState.userRole) {
                        com.dev.mandadito.data.models.Role.CLIENT -> "client_home"
                        com.dev.mandadito.data.models.Role.SELLER -> {
                            // Para sellers, verificar estado de Stripe
                            when (uiState.stripeConfigured) {
                                true -> "seller_home"  // Stripe configurado
                                false -> "stripe_onboarding"  // Stripe NO configurado
                                null -> null  // Aún verificando, no navegar aún
                            }
                        }
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
                // Habilitar auto-navegación para futuros logins
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

            // ===========================
            // STRIPE ONBOARDING (SELLERS)
            // ===========================

            composable("stripe_onboarding") {
                val context = LocalContext.current
                val viewModel = remember { StripeOnboardingViewModel(context) }

                // Verificar estado al cargar la pantalla
                LaunchedEffect(Unit) {
                    viewModel.checkStripeStatus()
                }

                StripeOnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingComplete = {
                        navController.navigate("seller_home") {
                            popUpTo("stripe_onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("seller_home") {
                SellerHomeScreen(navController)
            }

            // ===========================
            // NAVEGACIÓN DE VENDEDOR
            // ===========================

            composable(
                route = "seller_order_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                val context = LocalContext.current
                val viewModel = remember { com.dev.mandadito.presentation.viewmodels.seller.SellerOrdersViewModel(context) }
                OrderDetailScreen(
                    orderId = orderId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("delivery_home") {
                DeliveryHomeScreen(navController)
            }

            composable(
                route = "delivery_order_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                val context = LocalContext.current
                val viewModel = remember { com.dev.mandadito.presentation.viewmodels.delivery.DeliveryOrdersViewModel(context) }
                DeliveryOrderDetailScreen(
                    orderId = orderId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
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

            addressNavGraph(navController)

            // =====================================================
            // CARRITO DEL CLIENTE (fuera del scaffold)
            // =====================================================
            composable("client_cart") {
                ClientCartScreen(navController = navController)
            }

            // =====================================================
            // EDITAR PERFIL DEL CLIENTE
            // =====================================================
            composable("edit_profile") {
                val context = LocalContext.current
                val viewModel = remember { com.dev.mandadito.presentation.viewmodels.client.ClientProfileViewModel(context) }
                val profileUiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
                val scope = rememberCoroutineScope()

                profileUiState.userProfile?.let { userProfile ->
                    com.dev.mandadito.presentation.screens.client.EditProfileScreen(
                        userProfile = userProfile,
                        onBack = { navController.popBackStack() },
                        onSave = { nombre, email, avatarUri ->
                            viewModel.updateProfile(
                                nombre = nombre,
                                email = email,
                                avatarUri = avatarUri,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Perfil actualizado correctamente")
                                    }
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            )
                        },
                        onChangePassword = { currentPassword, newPassword ->
                            viewModel.changePassword(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Contraseña cambiada correctamente")
                                    }
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            )
                        },
                        isLoading = profileUiState.isUpdating,
                        errorMessage = profileUiState.updateError
                    )
                }
            }

            // =====================================================
            // SELECCIÓN DE DIRECCIÓN PARA PEDIDO
            // =====================================================
            composable(
                route = "select_address/{cartId}/{subtotal}",
                arguments = listOf(
                    navArgument("cartId") { type = NavType.StringType },
                    navArgument("subtotal") { type = NavType.FloatType }
                )
            ) { backStackEntry ->
                val cartId = backStackEntry.arguments?.getString("cartId") ?: ""
                val subtotal = backStackEntry.arguments?.getFloat("subtotal")?.toDouble() ?: 0.0

                // Obtener el ID de la dirección recién creada del savedStateHandle
                val newAddressId = backStackEntry.savedStateHandle.get<String?>("new_address_id")

                SelectAddressForOrderScreen(
                    cartId = cartId,
                    subtotal = subtotal,
                    newAddressId = newAddressId,
                    onAddressSelected = { addressId ->
                        // Limpiar el savedStateHandle antes de navegar
                        backStackEntry.savedStateHandle.remove<String>("new_address_id")
                        // Navegar al checkout con la dirección seleccionada
                        navController.navigate("checkout/$cartId/$addressId/$subtotal")
                    },
                    onAddNewAddress = {
                        // Navegar a la pantalla de agregar dirección
                        navController.navigateToAddAddress()
                    },
                    onBack = {
                        // Limpiar el savedStateHandle antes de volver
                        backStackEntry.savedStateHandle.remove<String>("new_address_id")
                        navController.popBackStack()
                    }
                )
            }

            // =====================================================
            // PEDIDOS DEL CLIENTE
            // =====================================================
            composable("client/orders") {
                ClientOrdersScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { orderId ->
                        navController.navigate("client/order_detail/$orderId")
                    }
                )
            }

            composable(
                route = "client/order_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                val context = LocalContext.current
                val viewModel = remember { com.dev.mandadito.presentation.viewmodels.client.ClientOrdersViewModel(context) }
                ClientOrderDetailScreen(
                    orderId = orderId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // =====================================================
            // CHECKOUT - PANTALLA DE PAGO CON STRIPE
            // =====================================================
            composable(
                route = "checkout/{cartId}/{addressId}/{subtotal}",
                arguments = listOf(
                    navArgument("cartId") { type = NavType.StringType },
                    navArgument("addressId") { type = NavType.StringType },
                    navArgument("subtotal") { type = NavType.FloatType }
                )
            ) { backStackEntry ->
                CheckoutScreen(
                    cartId = backStackEntry.arguments?.getString("cartId") ?: "",
                    addressId = backStackEntry.arguments?.getString("addressId") ?: "",
                    subtotal = backStackEntry.arguments?.getFloat("subtotal")?.toDouble() ?: 0.0,
                    deliveryFee = 50.0,
                    onPaymentSuccess = { orderId ->
                        // Navegar al detalle del pedido recién creado
                        navController.navigate("client/order_detail/$orderId") {
                            // Limpiar el stack hasta el home del cliente
                            popUpTo("client_home") {
                                inclusive = false
                            }
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // =====================================================
            // RESEÑAS
            // =====================================================

            composable(
                route = "product_reviews/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                val productName = backStackEntry.savedStateHandle.get<String>("product_name") ?: "Producto"
                val reviewViewModel: ProductReviewsViewModel = viewModel()

                ProductReviewsScreen(
                    productId = productId,
                    productName = productName,
                    userId = currentUserId,
                    navController = navController,
                    viewModel = reviewViewModel
                )
            }

            composable(
                route = "add_review/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                val productName = backStackEntry.savedStateHandle.get<String>("product_name") ?: "Producto"
                val reviewViewModel: ProductReviewsViewModel = viewModel()

                if (currentUserId != null) {
                    AddReviewScreen(
                        productId = productId,
                        productName = productName,
                        userId = currentUserId,
                        navController = navController,
                        viewModel = reviewViewModel
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate("login") {
                            popUpTo("add_review/$productId") { inclusive = true }
                        }
                    }
                }
            }

            composable(
                route = "edit_review/{productId}/{reviewId}/{rating}/{title}/{comment}",
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType },
                    navArgument("reviewId") { type = NavType.StringType },
                    navArgument("rating") { type = NavType.IntType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("comment") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                val reviewId = backStackEntry.arguments?.getString("reviewId") ?: return@composable
                val rating = backStackEntry.arguments?.getInt("rating") ?: 0
                val title = backStackEntry.arguments?.getString("title")?.takeIf { it.isNotEmpty() }
                val comment = backStackEntry.arguments?.getString("comment")?.takeIf { it.isNotEmpty() }
                val productName = backStackEntry.savedStateHandle.get<String>("product_name") ?: "Producto"
                val reviewViewModel: ProductReviewsViewModel = viewModel()

                if (currentUserId != null) {
                    AddReviewScreen(
                        productId = productId,
                        productName = productName,
                        userId = currentUserId,
                        navController = navController,
                        existingReviewId = reviewId,
                        existingRating = rating,
                        existingTitle = title,
                        existingComment = comment,
                        viewModel = reviewViewModel
                    )
                }
            }
        } // Fin NavHost
    } // Fin key()
}