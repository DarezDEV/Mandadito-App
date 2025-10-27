package com.dev.mandadito.utils

import android.util.Log
import com.dev.mandadito.config.AppConfig

/**
 * Validador de variables de entorno
 * Verifica que todas las variables necesarias estén configuradas
 */
object EnvironmentValidator {
    
    private const val TAG = "EnvironmentValidator"
    
    /**
     * Valida que todas las variables de entorno estén configuradas
     * @return true si todas las variables están configuradas, false en caso contrario
     */
    fun validateEnvironment(): Boolean {
        return try {
            Log.d(TAG, "Validando variables de entorno...")
            
            // Validar SUPABASE_URL
            val supabaseUrl = AppConfig.SUPABASE_URL
            if (supabaseUrl.isBlank()) {
                Log.e(TAG, "SUPABASE_URL no está configurada")
                return false
            }
            Log.d(TAG, "SUPABASE_URL: ${supabaseUrl.take(20)}...")
            
            // Validar SUPABASE_ANON_KEY
            val supabaseKey = AppConfig.SUPABASE_ANON_KEY
            if (supabaseKey.isBlank()) {
                Log.e(TAG, "SUPABASE_ANON_KEY no está configurada")
                return false
            }
            Log.d(TAG, "SUPABASE_ANON_KEY: ${supabaseKey.take(20)}...")
            
            Log.d(TAG, "Todas las variables de entorno están configuradas correctamente")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al validar variables de entorno: ${e.message}")
            false
        }
    }
    
    /**
     * Obtiene información de las variables de entorno para debugging
     * @return Map con información de las variables
     */
    fun getEnvironmentInfo(): Map<String, String> {
        return try {
            mapOf(
                "SUPABASE_URL" to (AppConfig.SUPABASE_URL.take(20) + "..."),
                "SUPABASE_ANON_KEY" to (AppConfig.SUPABASE_ANON_KEY.take(20) + "..."),
                "APP_NAME" to AppConfig.APP_NAME,
                "APP_VERSION" to AppConfig.APP_VERSION
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener información del entorno: ${e.message}")
            mapOf("ERROR" to (e.message ?: "Error desconocido"))
        }
    }
}
