package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val phone: String,
    @SerialName("formatted_address")
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("place_id")
    val placeId: String? = null,
    val street: String? = null,
    @SerialName("address_extra")
    val addressExtra: String? = null,
    val city: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    @SerialName("is_manual")
    val isManual: Boolean = false,
    @SerialName("is_default")
    val isDefault: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

data class PlaceDetails(
    val placeId: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val street: String,
    val city: String,
    val postalCode: String
)

data class AddressFormState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val street: String = "",
    val addressExtra: String = "",
    val city: String = "",
    val postalCode: String = "",
    val isManualMode: Boolean = false,
    val selectedPlaceDetails: PlaceDetails? = null
) {
    val isValid: Boolean
        get() = firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                phone.isNotBlank() &&
                (if (isManualMode) {
                    street.isNotBlank() && city.isNotBlank()
                } else {
                    selectedPlaceDetails != null
                })
}

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}