package com.dev.mandadito.config

import android.util.Log
import com.dev.mandadito.BuildConfig

/**
 * Configuración de la aplicación
 * Centraliza el acceso a las variables de entorno y configuración
 */
object AppConfig {

    private const val TAG = "AppConfig"

    // Variables de entorno de Supabase con validación
    val SUPABASE_URL: String by lazy {
        val url = BuildConfig.SUPABASE_URL
        if (url.isBlank()) {
            Log.w(TAG, "SUPABASE_URL no está configurada en local.properties - usando valor por defecto")
            "https://placeholder.supabase.co" // Valor por defecto para evitar crash
        } else {
            Log.d(TAG, "SUPABASE_URL configurada correctamente")
            url
        }
    }

    val SUPABASE_ANON_KEY: String by lazy {
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (key.isBlank()) {
            Log.w(TAG, "SUPABASE_ANON_KEY no está configurada en local.properties - usando valor por defecto")
            "placeholder_key" // Valor por defecto para evitar crash
        } else {
            Log.d(TAG, "SUPABASE_ANON_KEY configurada correctamente")
            key
        }
    }

    // Configuración de la aplicación
    const val APP_NAME = "Mandadito"
    const val APP_VERSION = "1.0.0"

    // Configuración del splash screen
    const val SPLASH_DURATION = 2500L // 2.5 segundos
    const val SPLASH_TITLE = "Mandadito"
    const val SPLASH_SUBTITLE = "Tu delivery de confianza"

    // Configuración de la base de datos
    const val DATABASE_NAME = "mandadito_db"
    const val DATABASE_VERSION = 1

    // URLs de la API
    const val API_BASE_URL = "https://api.mandadito.com"
    const val API_VERSION = "v1"

    // Configuración de la UI
    const val ANIMATION_DURATION = 1000L
    const val LOGO_SIZE = 180
    const val TITLE_SIZE = 32
    const val SUBTITLE_SIZE = 16
}
