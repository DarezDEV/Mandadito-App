package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.dev.mandadito.data.network.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.material3.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerHomeScreen(
    navController: NavController
) {

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mandadito - Vemdedor") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            // Cerrar sesión usando AuthRepository
                            authRepository.logout()
                            // Navegar a la pantalla de bienvenida
                            navController.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
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
        }
    ) { paddingValues ->
        // Contenido de la pantalla principal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pantalla de Vendedor",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }

}