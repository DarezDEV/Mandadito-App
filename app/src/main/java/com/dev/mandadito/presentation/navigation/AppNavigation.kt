package com.dev.mandadito.presentation.navigation

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dev.mandadito.presentation.viewmodels.auth.AuthViewModel
import com.dev.mandadito.presentation.screens.auth.WelcomeScreen
import com.dev.mandadito.presentation.screens.auth.LoginScreen
import com.dev.mandadito.presentation.screens.auth.RegisterScreen
import com.dev.mandadito.presentation.screens.client.ClientHomeScreen
import com.dev.mandadito.presentation.screens.delivery.DeliveryHomeScreen
import com.dev.mandadito.presentation.screens.seller.SellerHomeScreen
import com.dev.mandadito.presentation.screens.admin.AdminScaffold

@Composable
fun AppNavigation(context: Context? = null) {
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
                        popUpTo("welcome") { inclusive = true }
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
        // ============================================
        // PANTALLAS DE AUTENTICACIÓN
        // ============================================

        composable(
            route = "welcome",
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            WelcomeScreen(
                onLoginClick = { navController.navigate("login") },
                onRegisterClick = { navController.navigate("register") },
                onGoogleClick = { /* TODO */ },
                onFacebookClick = { /* TODO */ },
                navController = navController
            )
        }

        composable(
            route = "login",
            enterTransition = {
                // Desliza desde la derecha y se desvanece
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                // Desliza hacia la izquierda y se desvanece
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                // Al volver, entra desde la izquierda
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                // Al salir para volver, sale hacia la derecha
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            LaunchedEffect(Unit) {
                shouldAutoNavigate = false
            }
            LoginScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        composable(
            route = "register",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            RegisterScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // ============================================
        // PANTALLAS PRINCIPALES POR ROL
        // ============================================

        composable(
            route = "client_home",
            enterTransition = {
                // Zoom in con fade para transición entre autenticación y home
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(500))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(500))
            }
        ) {
            ClientHomeScreen(navController = navController)
        }

        composable(
            route = "seller_home",
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(500))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(500))
            }
        ) {
            SellerHomeScreen(navController = navController)
        }

        composable(
            route = "delivery_home",
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(500))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(500))
            }
        ) {
            DeliveryHomeScreen(navController = navController)
        }

        composable(
            route = "admin_home",
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(500))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(500))
            }
        ) {
            AdminScaffold(navController = navController)
        }
    }
}