package com.dev.mandadito.data.network

import android.util.Log
import com.dev.mandadito.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.storage.Storage

/**
 * Cliente de Supabase SEGURO
 * Solo usa ANON_KEY - Las operaciones administrativas se hacen via Edge Functions
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"

    /**
     * Cliente principal de Supabase con ANON_KEY
     * Este es el ÚNICO cliente que debe usarse en la aplicación
     */
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
            }.also {
                Log.d(TAG, "✅ Cliente Supabase inicializado correctamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar Supabase: ${e.message}")
            throw e
        }
    }

    /**
     * ⚠️ ELIMINADO: adminClient
     *
     * El cliente admin con SERVICE_ROLE_KEY ha sido ELIMINADO por seguridad.
     * Las operaciones administrativas ahora se hacen a través de Edge Functions:
     *
     * - Crear usuarios: POST /functions/v1/create-user
     * - Otras operaciones admin: crear más Edge Functions según sea necesario
     *
     * NUNCA expongas la SERVICE_ROLE_KEY en el código del cliente.
     */
}