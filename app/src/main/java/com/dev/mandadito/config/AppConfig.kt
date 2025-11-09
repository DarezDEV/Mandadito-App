package com.dev.mandadito.config

import android.util.Log
import com.dev.mandadito.BuildConfig

/**
 * Configuración de la aplicación SEGURA
 * Centraliza el acceso a las variables de entorno
 */
object AppConfig {

    private const val TAG = "AppConfig"

    // Variable de URL de Supabase con validación
    val SUPABASE_URL: String by lazy {
        val url = BuildConfig.SUPABASE_URL
        if (url.isBlank()) {
            Log.w(TAG, "⚠️ SUPABASE_URL no está configurada - usando valor por defecto")
            "https://placeholder.supabase.co"
        } else {
            Log.d(TAG, "✅ SUPABASE_URL configurada correctamente")
            url
        }
    }

    // Variable de ANON_KEY de Supabase con validación
    val SUPABASE_ANON_KEY: String by lazy {
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (key.isBlank()) {
            Log.w(TAG, "⚠️ SUPABASE_ANON_KEY no está configurada - usando valor por defecto")
            "placeholder_key"
        } else {
            Log.d(TAG, "✅ SUPABASE_ANON_KEY configurada correctamente")
            key
        }
    }

    /**
     * ⚠️ SERVICE_ROLE_KEY REMOVIDA POR SEGURIDAD
     *
     * La SERVICE_ROLE_KEY NO debe estar expuesta en el código del cliente.
     * Las operaciones administrativas ahora se realizan mediante Edge Functions
     * que se ejecutan en el servidor de Supabase de forma segura.
     *
     * Si necesitas realizar operaciones administrativas:
     * 1. Crea una Edge Function en Supabase
     * 2. Llama a esa función desde tu app usando ANON_KEY + token de autenticación
     * 3. La Edge Function usará SERVICE_ROLE_KEY en el servidor (seguro)
     */

    // Configuración de la aplicación
    const val APP_NAME = "Mandadito"
    const val APP_VERSION = "1.0.0"

    // Configuración del splash screen
    const val SPLASH_DURATION = 2500L // 2.5 segundos
    const val ANIMATION_DURATION = 1000L
}