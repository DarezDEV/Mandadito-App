package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.data.network.SupabaseErrorHandler
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


class AddressRepository(
    private val context: Context
) {

    private val supabase = SupabaseClient.client

    companion object {
        private const val TAG = "AddressRepository"
    }

    // ========== SUPABASE CRUD ==========

    suspend fun getAddresses(userId: String): Result<List<Address>> {
        return try {
            Log.d(TAG, "Obteniendo direcciones de Supabase para usuario: $userId")
            val addresses = supabase.from("addresses")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order(column = "is_default", order = Order.DESCENDING)
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<Address>()

            Log.d(TAG, "✅ ${addresses.size} direcciones obtenidas")
            Result.success(addresses)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo direcciones", e)
            val errorMessage = SupabaseErrorHandler.getUserFriendlyMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun createAddress(address: Address): Result<Address> {
        return try {
            Log.d(TAG, "Creando dirección en Supabase...")
            val created = supabase.from("addresses")
                .insert(address) {
                    select()
                }
                .decodeSingle<Address>()

            Log.d(TAG, "✅ Dirección creada: ${created.id}")
            Result.success(created)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando dirección", e)
            val errorMessage = SupabaseErrorHandler.getUserFriendlyMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun updateAddress(id: String, address: Address): Result<Address> {
        return try {
            Log.d(TAG, "Actualizando dirección: $id")
            val updated = supabase.from("addresses")
                .update(address) {
                    filter {
                        eq("id", id)
                    }
                    select()
                }
                .decodeSingle<Address>()

            Log.d(TAG, "✅ Dirección actualizada: ${updated.id}")
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando dirección", e)
            val errorMessage = SupabaseErrorHandler.getUserFriendlyMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun setDefaultAddress(id: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Estableciendo dirección predeterminada: $id")

            supabase.from("addresses")
                .update(mapOf("is_default" to false)) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            supabase.from("addresses")
                .update(mapOf("is_default" to true)) {
                    filter {
                        eq("id", id)
                    }
                }

            Log.d(TAG, "✅ Dirección predeterminada establecida")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error estableciendo dirección predeterminada", e)
            val errorMessage = SupabaseErrorHandler.getUserFriendlyMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun deleteAddress(id: String): Result<Unit> {
        return try {
            Log.d(TAG, "Eliminando dirección: $id")
            supabase.from("addresses")
                .delete {
                    filter {
                        eq("id", id)
                    }
                }

            Log.d(TAG, "✅ Dirección eliminada")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando dirección", e)
            val errorMessage = SupabaseErrorHandler.getUserFriendlyMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun geocodeAddress(address: String): Result<Pair<Double, Double>> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d(TAG, "🌍 Geocodificando: $address")

                val cleanedAddress = address
                    .replace("Repblica Dominicana", "")
                    .replace("Republica Dominicana", "")
                    .replace("República Dominicana", "")
                    .replace(Regex(",\\s*"), ", ")
                    .replace(Regex(",\\s*,+"), ",")
                    .trim()

                val queries = listOf(
                    cleanedAddress,
                    cleanedAddress.replace("1", ""),
                    cleanedAddress.replace(", Santo Domingo,", ", Distrito Nacional,")
                )

                for (query in queries) {
                    val encodedAddress = java.net.URLEncoder.encode(query, "UTF-8")
                    val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress&countrycodes=DO&limit=1"

                    val response = java.net.URL(url).readText()
                    Log.d(TAG, "✅ Respuesta Nominatim para '$query': ${response.take(200)}")

                    val json = kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                    }

                    val jsonArray = json.parseToJsonElement(response).jsonArray

                    if (!jsonArray.isEmpty()) {
                        val lat = jsonArray.get(0)?.jsonObject?.get("lat")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        val lon = jsonArray.get(0)?.jsonObject?.get("lon")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0

                        if (lat != 0.0 || lon != 0.0) {
                            Log.d(TAG, "✅ Coordenadas obtenidas: $lat, $lon")
                            return@withContext Result.success(kotlin.Pair(lat, lon))
                        }
                    }
                }

                Log.e(TAG, "❌ No se encontró la dirección después de intentar múltiples consultas")
                return@withContext Result.failure(Exception(
                    "💡 No encontramos esa dirección.\n\n" +
                    "Sugerencias:\n" +
                    "• Escribe la dirección más completa (calle, número, ciudad)\n" +
                    "• Verifica la ortografía de la dirección\n" +
                    "• Ejemplo: Av. Abraham Lincoln 123, Santo Domingo"
                ))

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error geocodificando: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
    }
}
