package com.dev.mandadito.config

import android.util.Log

object AddressFeatureFlags {

    private const val TAG = "AddressFeatures"

    val hasGoogleMapsApiKey: Boolean
        get() = AppConfig.MAPS_API_KEY.let {
            it != "YOUR_MAPS_API_KEY_HERE" &&
                    it.isNotBlank() &&
                    it.startsWith("AIza")
        }

    val hasSupabaseConfig: Boolean
        get() = AppConfig.SUPABASE_URL.contains(".supabase.co") &&
                AppConfig.SUPABASE_ANON_KEY.isNotBlank()

    val isDevelopmentMode: Boolean
        get() = !hasGoogleMapsApiKey || !hasSupabaseConfig

    fun logStatus() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📋 Módulo de Direcciones")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🗺️  Google Maps: ${if (hasGoogleMapsApiKey) "✅" else "❌"}")
        Log.d(TAG, "🗄️  Supabase: ${if (hasSupabaseConfig) "✅" else "❌"}")
        Log.d(TAG, "🎯 Modo: ${if (isDevelopmentMode) "DESARROLLO 🧪" else "PRODUCCIÓN ✅"}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}