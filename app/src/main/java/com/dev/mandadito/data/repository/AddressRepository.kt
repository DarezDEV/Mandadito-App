package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.config.AddressFeatureFlags
import com.dev.mandadito.data.local.mock.MockAddressData
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.data.network.SupabaseErrorHandler
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AddressRepository(
    private val placesClient: PlacesClient?,
    private val context: Context
) {

    private val supabase = SupabaseClient.client
    private var sessionToken = AutocompleteSessionToken.newInstance()

    companion object {
        private const val TAG = "AddressRepository"
    }

    // ========== GOOGLE PLACES ==========

    suspend fun searchPlaces(query: String): Result<List<PlacePrediction>> {
        return if (AddressFeatureFlags.hasGoogleMapsApiKey && placesClient != null) {
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(sessionToken)
                    .setQuery(query)
                    .setCountry("DO")
                    .build()

                val response = placesClient.findAutocompletePredictions(request).await()

                val predictions = response.autocompletePredictions.map { prediction ->
                    PlacePrediction(
                        placeId = prediction.placeId ?: "",
                        primaryText = prediction.getPrimaryText(null)?.toString() ?: "",
                        secondaryText = prediction.getSecondaryText(null)?.toString() ?: "",
                        fullText = prediction.getFullText(null)?.toString() ?: ""
                    )
                }

                Result.success(predictions)
            } catch (e: Exception) {
                Log.e(TAG, "Error en búsqueda de Google Places", e)
                Result.failure(e)
            }
        } else {
            delay(300)
            Result.success(MockAddressData.getMockPredictions(query))
        }
    }

    suspend fun getPlaceDetails(placeId: String): Result<PlaceDetails> {
        return if (AddressFeatureFlags.hasGoogleMapsApiKey && placesClient != null) {
            try {
                val placeFields = listOf(
                    Place.Field.ID,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS_COMPONENTS
                )

                val request = FetchPlaceRequest.builder(placeId, placeFields)
                    .setSessionToken(sessionToken)
                    .build()

                val response = placesClient.fetchPlace(request).await()
                val place = response.place

                sessionToken = AutocompleteSessionToken.newInstance()

                val addressComponentsList = place.addressComponents?.asList() ?: emptyList()

                val street = buildString {
                    val route = addressComponentsList.firstOrNull { component ->
                        component.types.contains("route")
                    }?.name
                    val number = addressComponentsList.firstOrNull { component ->
                        component.types.contains("street_number")
                    }?.name
                    if (!number.isNullOrBlank()) append(number).append(" ")
                    if (!route.isNullOrBlank()) append(route)
                }.trim()

                val city = addressComponentsList.firstOrNull { component ->
                    component.types.contains("locality") ||
                            component.types.contains("administrative_area_level_2")
                }?.name ?: ""

                val postalCode = addressComponentsList.firstOrNull { component ->
                    component.types.contains("postal_code")
                }?.name ?: ""

                val latLng = place.latLng ?: LatLng(0.0, 0.0)

                Result.success(
                    PlaceDetails(
                        placeId = place.id ?: placeId,
                        formattedAddress = place.address ?: "",
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        street = street,
                        city = city,
                        postalCode = postalCode
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo detalles del lugar", e)
                Result.failure(e)
            }
        } else {
            delay(400)
            val details = MockAddressData.getMockPlaceDetails(placeId)
            if (details != null) {
                Result.success(details)
            } else {
                Result.failure(Exception("Place not found"))
            }
        }
    }

    // ========== SUPABASE CRUD ==========

    suspend fun getAddresses(userId: String): Result<List<Address>> {
        return if (AddressFeatureFlags.hasSupabaseConfig) {
            try {
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
        } else {
            delay(300)
            Result.success(MockAddressData.getAllAddresses(userId))
        }
    }

    suspend fun createAddress(address: Address): Result<Address> {
        return if (AddressFeatureFlags.hasSupabaseConfig) {
            try {
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
        } else {
            delay(500)
            Result.success(MockAddressData.addAddress(address))
        }
    }

    suspend fun updateAddress(id: String, address: Address): Result<Address> {
        return if (AddressFeatureFlags.hasSupabaseConfig) {
            try {
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
        } else {
            delay(500)
            val updated = MockAddressData.updateAddress(id, address)
            if (updated != null) {
                Result.success(updated)
            } else {
                Result.failure(Exception("Dirección no encontrada"))
            }
        }
    }

    suspend fun setDefaultAddress(id: String, userId: String): Result<Unit> {
        return if (AddressFeatureFlags.hasSupabaseConfig) {
            try {
                Log.d(TAG, "Estableciendo dirección predeterminada: $id")

                // Primero, quitar is_default de todas las direcciones del usuario
                supabase.from("addresses")
                    .update(mapOf("is_default" to false)) {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                // Luego, marcar la dirección seleccionada como predeterminada
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
        } else {
            delay(300)
            val success = MockAddressData.setDefaultAddress(id, userId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Dirección no encontrada"))
            }
        }
    }

    suspend fun deleteAddress(id: String): Result<Unit> {
        return if (AddressFeatureFlags.hasSupabaseConfig) {
            try {
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
        } else {
            delay(300)
            val deleted = MockAddressData.removeAddress(id)
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Dirección no encontrada"))
            }
        }
    }
}