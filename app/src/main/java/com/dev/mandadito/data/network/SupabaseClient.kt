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
                    install(Auth) {
                        autoLoadFromStorage = true
                        alwaysAutoRefresh = true
                    }
                    install(Postgrest)
                }.also {
                    Log.d(TAG, "Cliente Supabase inicializado correctamente")
                    // La sesión se carga automáticamente con autoLoadFromStorage = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar Supabase: ${e.message}")
                throw e
            }
        }

        /**
         * Cliente admin de Supabase con SERVICE_ROLE_KEY
         * IMPORTANTE: Solo usar para operaciones administrativas que requieren permisos elevados
         * NUNCA exponer la SERVICE_ROLE_KEY en el código público
         */
        val adminClient: SupabaseClient? by lazy {
            try {
                val serviceKey = AppConfig.SUPABASE_SERVICE_ROLE_KEY
                if (serviceKey.isBlank()) {
                    Log.w(TAG, "SERVICE_ROLE_KEY no configurada - operaciones admin no disponibles")
                    return@lazy null
                }

                Log.d(TAG, "Inicializando cliente admin Supabase...")
                createSupabaseClient(
                    supabaseUrl = AppConfig.SUPABASE_URL,
                    supabaseKey = serviceKey
                ) {
                    install(Auth)
                    install(Postgrest)
                }.also {
                    Log.d(TAG, "Cliente admin Supabase inicializado correctamente")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar cliente admin Supabase: ${e.message}")
                null
            }
        }
    }
