package com.dev.mandadito

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dev.mandadito.config.AddressFeatureFlags
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.presentation.navigation.AppNavigation
import com.dev.mandadito.ui.theme.MandaditoTheme
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ✨ Inicializar Google Places para el módulo de direcciones
        initializeGooglePlaces()

        // ✨ Log del estado del módulo de direcciones (solo en debug)
        if (BuildConfig.DEBUG) {
            AddressFeatureFlags.logStatus()
        }

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

    /**
     * Inicializa Google Places si la API Key está configurada
     */
    private fun initializeGooglePlaces() {
        if (!Places.isInitialized()) {
            if (AddressFeatureFlags.hasGoogleMapsApiKey) {
                try {
                    Places.initialize(applicationContext, AppConfig.MAPS_API_KEY)
                    Log.d(TAG, "✅ Google Places inicializado correctamente")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error inicializando Google Places: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "⚠️ Google Places no inicializado - API Key no configurada (usando mock data)")
            }
        }
    }
}