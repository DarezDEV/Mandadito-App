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
    
    // Determinar la pantalla inicial basada en la sesión
    val startDestination = remember(uiState.isLoggedIn) {
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