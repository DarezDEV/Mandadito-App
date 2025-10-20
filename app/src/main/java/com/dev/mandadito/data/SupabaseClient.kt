package com.dev.mandadito.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest



val supabase = createSupabaseClient(
    supabaseUrl = "https://rkejuidtojjrymbnoxxi.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJrZWp1aWR0b2pqcnltYm5veHhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAxMDA3NTgsImV4cCI6MjA3NTY3Njc1OH0.fulb-bsSlyHufwID_gNLaxzYcbpRhTm3a9-f2yvmMZY"
) {
    install(Postgrest)
}