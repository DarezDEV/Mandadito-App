package com.dev.mandadito.presentation.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// import com.dev.mandadito.data.network.AuthRepository
import com.dev.mandadito.presentation.viewmodels.AuthViewModel
import com.dev.mandadito.presentation.screens.auth.WelcomeScreen
import com.dev.mandadito.presentation.screens.auth.LoginScreen
import com.dev.mandadito.presentation.screens.auth.RegisterScreen
import com.dev.mandadito.presentation.screens.client.ClientHomeScreen
import com.dev.mandadito.presentation.screens.delivery.DeliveryHomeScreen
import com.dev.mandadito.presentation.screens.seller.SellerHomeScreen
import com.dev.mandadito.presentation.screens.admin.AdminHomeScreen

@Composable
fun AppNavigation(context: Context? = null) {
    val navController = rememberNavController()
    val composeContext = LocalContext.current
    val appContext = remember { composeContext.applicationContext as android.app.Application }
    val authViewModel = remember { AuthViewModel(appContext) }
    
    // Observar el estado de autenticación
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    
    // Fijar un startDestination estable y navegar por efecto según rol
    val startDestination = "welcome"
    
    // Variable para evitar navegación automática cuando el usuario explícitamente navega a login
    var shouldAutoNavigate by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.isLoggedIn, uiState.userRole) {
        // Solo navegar automáticamente si:
        // 1. El usuario está logueado
        // 2. Hay un rol definido
        // 3. Está habilitada la navegación automática
        // 4. No estamos ya en una pantalla de autenticación
        if (shouldAutoNavigate && uiState.isLoggedIn && uiState.userRole != null) {
            val currentRoute = navController.currentDestination?.route
            val isAuthScreen = currentRoute in listOf("welcome", "login", "register")
            
            // Si estamos en una pantalla de autenticación, navegar al home correspondiente
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
                        popUpTo("welcome") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        } else if (!uiState.isLoggedIn) {
            // Si el usuario no está logueado, permitir navegación automática nuevamente
            // Esto permite que funcione el login normal
            shouldAutoNavigate = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ============================================
        // PANTALLAS DE AUTENTICACIÓN
        // ============================================

        composable("welcome") {
            WelcomeScreen(
                onLoginClick = { navController.navigate("login") },
                onRegisterClick = { navController.navigate("register") },
                onGoogleClick = { /* TODO: Implementar Google Sign-In */ },
                onFacebookClick = { /* TODO: Implementar Facebook Sign-In */ },
                navController = navController
            )
        }

        composable("login") {
            // Deshabilitar navegación automática cuando el usuario explícitamente va a login
            LaunchedEffect(Unit) {
                shouldAutoNavigate = false
            }
            LoginScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }


        // ============================================
        // PANTALLAS PRINCIPALES POR ROL
        // ============================================

        // Cliente - Rol: 'client'
        composable("client_home") {
            ClientHomeScreen(navController = navController)
        }

        // Vendedor/Colmadero - Rol: 'seller'
        composable("seller_home") {
            SellerHomeScreen(navController = navController)
        }

        // Repartidor - Rol: 'delivery'
        composable("delivery_home") {
            DeliveryHomeScreen(navController = navController)
        }

        // Administrador - Rol: 'admin'
        composable("admin_home") {
            AdminHomeScreen(navController = navController)
        }

        // ============================================
        // AQUÍ PUEDES AGREGAR MÁS RUTAS
        // ============================================
        // Por ejemplo:
        // - Pantallas de perfil
        // - Pantallas de pedidos
        // - Pantallas de productos
        // - etc.
    }
}