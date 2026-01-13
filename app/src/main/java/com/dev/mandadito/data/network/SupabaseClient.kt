package com.dev.mandadito.data.network

import android.util.Log
import com.dev.mandadito.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime


object SupabaseClient {

    private const val TAG = "SupabaseClient"

    val client: SupabaseClient by lazy {
        try {
            Log.d(TAG, "Inicializando cliente Supabase con ANON_KEY...")
            createSupabaseClient(
                supabaseUrl = AppConfig.SUPABASE_URL,
                supabaseKey = AppConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth) {
                    autoLoadFromStorage = true
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Storage)
                install(Realtime) {
                    // Configuración de Realtime para actualizaciones en tiempo real
                }
            }.also {
                Log.d(TAG, "✅ Cliente Supabase inicializado correctamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar Supabase: ${e.message}")
            throw e
        }
    }

}