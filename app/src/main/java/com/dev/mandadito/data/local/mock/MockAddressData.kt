package com.dev.mandadito.data.local.mock

import com.dev.mandadito.data.models.Address
import com.dev.mandadito.data.models.PlaceDetails
import com.dev.mandadito.data.models.PlacePrediction
import java.util.UUID

object MockAddressData {

    private val dominicanLocations = listOf(
        Triple("Av. Winston Churchill", "Piantini, Santo Domingo", Pair(18.4692, -69.9293)),
        Triple("Plaza Sambil", "Av. John F. Kennedy, Santo Domingo", Pair(18.4689, -69.9406)),
        Triple("Malecón de Santo Domingo", "Av. George Washington", Pair(18.4655, -69.9017)),
        Triple("Calle El Conde", "Zona Colonial, Santo Domingo", Pair(18.4764, -69.8933)),
        Triple("Blue Mall", "Av. Winston Churchill, Santo Domingo", Pair(18.4712, -69.9402)),
        Triple("Ágora Mall", "Av. John F. Kennedy, Santo Domingo", Pair(18.4734, -69.9424)),
        Triple("Parque Mirador del Sur", "Av. Anacaona", Pair(18.4583, -69.9478))
    )

    private const val MOCK_USER_ID = "mock-user-id"

    private val temporaryStorage = mutableListOf(
        Address(
            id = UUID.randomUUID().toString(),
            userId = MOCK_USER_ID,
            firstName = "Juan",
            lastName = "Pérez",
            phone = "8091234567",
            formattedAddress = "Av. Abraham Lincoln 123, Piantini",
            latitude = 18.4721,
            longitude = -69.9374,
            street = "Av. Abraham Lincoln 123",
            city = "Santo Domingo",
            postalCode = "10147",
            isManual = false,
            isDefault = true
        ),
        Address(
            id = UUID.randomUUID().toString(),
            userId = MOCK_USER_ID,
            firstName = "María",
            lastName = "González",
            phone = "8299876543",
            formattedAddress = "Calle El Conde 456, Zona Colonial",
            latitude = 18.4764,
            longitude = -69.8933,
            street = "Calle El Conde 456",
            addressExtra = "Edificio Colonial, Apto 3B",
            city = "Santo Domingo",
            isManual = false,
            isDefault = false
        )
    )

    fun getMockPredictions(query: String): List<PlacePrediction> {
        if (query.length < 2) return emptyList()

        return dominicanLocations
            .filter { (primary, secondary, _) ->
                primary.contains(query, ignoreCase = true) ||
                        secondary.contains(query, ignoreCase = true)
            }
            .mapIndexed { index, (primary, secondary, _) ->
                PlacePrediction(
                    placeId = "mock_pred_${query.hashCode()}_$index",
                    primaryText = primary,
                    secondaryText = secondary,
                    fullText = "$primary, $secondary"
                )
            }
            .take(5)
    }

    fun getMockPlaceDetails(placeId: String): PlaceDetails? {
        val indexStr = placeId.substringAfterLast("_")
        val index = indexStr.toIntOrNull() ?: 0

        val location = dominicanLocations.getOrNull(index) ?: dominicanLocations.first()
        val (primary, secondary, coords) = location

        return PlaceDetails(
            placeId = placeId,
            formattedAddress = "$primary, $secondary",
            latitude = coords.first,
            longitude = coords.second,
            street = primary,
            city = "Santo Domingo",
            postalCode = (10100..10999).random().toString()
        )
    }

    fun getAllAddresses(userId: String = MOCK_USER_ID): List<Address> {
        return temporaryStorage
            .filter { it.userId == userId }
            .sortedWith(compareByDescending<Address> { it.isDefault }.thenByDescending { it.createdAt })
            .toList()
    }

    fun addAddress(address: Address): Address {
        val newAddress = address.copy(
            id = UUID.randomUUID().toString(),
            userId = address.userId ?: MOCK_USER_ID,
            createdAt = java.time.Instant.now().toString()
        )
        temporaryStorage.add(newAddress)
        return newAddress
    }

    fun updateAddress(id: String, address: Address): Address? {
        val index = temporaryStorage.indexOfFirst { it.id == id }
        return if (index != -1) {
            val updated = address.copy(
                id = id,
                updatedAt = java.time.Instant.now().toString()
            )
            temporaryStorage[index] = updated
            updated
        } else {
            null
        }
    }

    fun setDefaultAddress(id: String, userId: String): Boolean {
        val addressToUpdate = temporaryStorage.find { it.id == id && it.userId == userId }
        if (addressToUpdate == null) return false

        // Quitar is_default de todas las direcciones del usuario
        temporaryStorage.forEachIndexed { index, address ->
            if (address.userId == userId && address.isDefault) {
                temporaryStorage[index] = address.copy(isDefault = false)
            }
        }

        // Marcar la dirección seleccionada como predeterminada
        val targetIndex = temporaryStorage.indexOfFirst { it.id == id }
        if (targetIndex != -1) {
            temporaryStorage[targetIndex] = temporaryStorage[targetIndex].copy(isDefault = true)
            return true
        }

        return false
    }

    fun removeAddress(id: String): Boolean {
        return temporaryStorage.removeIf { it.id == id }
    }
}