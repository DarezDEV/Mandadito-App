package com.dev.mandadito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dev.mandadito.presentation.navigation.AppNavigation
import com.dev.mandadito.ui.theme.MandaditoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Recibir el estado de verificación de sesión desde SplashActivity
        val sessionAlreadyChecked = intent.getBooleanExtra("SESSION_ALREADY_CHECKED", false)
        val hasActiveSession = intent.getBooleanExtra("HAS_ACTIVE_SESSION", false)
        val userRoleString = intent.getStringExtra("USER_ROLE")

        setContent {
            MandaditoTheme {
                AppNavigation(
                    sessionAlreadyChecked = sessionAlreadyChecked,
                    hasActiveSession = hasActiveSession,
                    userRoleString = userRoleString
                )
            }
        }
    }
}



