package com.dev.mandadito.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.GoTrue

private const val SUPABASE_URL = "https://dkksstidwdtzmkmevwod.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRra3NzdGlkd2R0em1rbWV2d29kIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA4ODY3NjIsImV4cCI6MjA3NjQ2Mjc2Mn0.GwE_DGsh5Yr8znFWy1dcGk-vh9XPHUQGsi20e-iJP4Q"


object SupabaseClientProvider {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
    }
}