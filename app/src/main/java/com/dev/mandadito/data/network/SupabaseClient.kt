package com.dev.mandadito.data.network

import android.util.Log
import com.dev.mandadito.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth

object SupabaseClient {

    private const val TAG = "SupabaseClient"

    val client: SupabaseClient by lazy {
        try {
            Log.d(TAG, "Inicializando cliente Supabase...")
            createSupabaseClient(
                supabaseUrl = AppConfig.SUPABASE_URL,
                supabaseKey = AppConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
            }.also {
                Log.d(TAG, "Cliente Supabase inicializado correctamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar Supabase: ${e.message}")
            throw e
        }
    }
}
