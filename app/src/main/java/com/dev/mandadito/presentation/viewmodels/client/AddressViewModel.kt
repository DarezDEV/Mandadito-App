package com.dev.mandadito.presentation.viewmodels.client

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.*
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.data.repository.AddressRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddressViewModel(
    private val repository: AddressRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AddressViewModel"
    }

    private val _addressesState = MutableStateFlow<UiState<List<Address>>>(UiState.Idle)
    val addressesState: StateFlow<UiState<List<Address>>> = _addressesState.asStateFlow()

    private val _formState = MutableStateFlow(AddressFormState())
    val formState: StateFlow<AddressFormState> = _formState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Address>>(UiState.Idle)
    val saveState: StateFlow<UiState<Address>> = _saveState.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteState: StateFlow<UiState<Unit>> = _deleteState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Address>>(UiState.Idle)
    val updateState: StateFlow<UiState<Address>> = _updateState.asStateFlow()

    private val _editingAddressId = MutableStateFlow<String?>(null)
    val editingAddressId: StateFlow<String?> = _editingAddressId.asStateFlow()

    private val _setDefaultState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val setDefaultState: StateFlow<UiState<Unit>> = _setDefaultState.asStateFlow()

    private val supabase = SupabaseClient.client

    init {
        loadAddresses()
    }

    private fun getUserId(): String {
        return supabase.auth.currentUserOrNull()?.id ?: "mock-user-id"
    }

    fun updateFirstName(value: String) {
        _formState.update { it.copy(firstName = value) }
    }

    fun updateLastName(value: String) {
        _formState.update { it.copy(lastName = value) }
    }

    fun updatePhone(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '+' }
        _formState.update { it.copy(phone = cleaned) }
    }

    fun updateStreet(value: String) {
        _formState.update { it.copy(street = value) }
    }

    fun updateAddressExtra(value: String) {
        _formState.update { it.copy(addressExtra = value) }
    }

    fun updateCity(value: String) {
        _formState.update { it.copy(city = value) }
    }

    fun updatePostalCode(value: String) {
        val cleaned = value.filter { it.isDigit() }
        _formState.update { it.copy(postalCode = cleaned) }
    }

    fun saveAddress() {
        viewModelScope.launch {
            val form = _formState.value

            if (!form.isValid) {
                _saveState.value = UiState.Error("Completa todos los campos obligatorios")
                return@launch
            }

            _saveState.value = UiState.Loading

            val userId = getUserId()
            val fullAddress = "${form.street}, ${form.city}, República Dominicana"

            Log.d(TAG, "🌍 Geocodificando dirección: $fullAddress")

            repository.geocodeAddress(fullAddress).fold(
                onSuccess = { coords ->
                    val address = Address(
                        userId = userId,
                        firstName = form.firstName,
                        lastName = form.lastName,
                        phone = form.phone,
                        formattedAddress = fullAddress,
                        latitude = coords.first,
                        longitude = coords.second,
                        street = form.street,
                        addressExtra = form.addressExtra.takeIf { it.isNotBlank() },
                        city = form.city,
                        postalCode = form.postalCode.takeIf { it.isNotBlank() },
                        isManual = true
                    )

                    repository.createAddress(address).fold(
                        onSuccess = { savedAddress ->
                            _saveState.value = UiState.Success(savedAddress)
                            resetForm()
                            loadAddresses()
                            Log.d(TAG, "✅ Dirección guardada: ${savedAddress.id}")
                        },
                        onFailure = { error ->
                            _saveState.value = UiState.Error(error.message ?: "Error al guardar")
                            Log.e(TAG, "Error guardando dirección", error)
                        }
                    )
                },
                onFailure = { error ->
                    _saveState.value = UiState.Error(
                        "💡 No encontramos esa dirección.\n\n" +
                        "Sugerencias:\n" +
                        "• Escribe la dirección más completa (calle, número, ciudad)\n" +
                        "• Verifica la ortografía de la dirección\n" +
                        "• Ejemplo: Av. Abraham Lincoln 123, Santo Domingo"
                    )
                    Log.e(TAG, "Error geocodificando: ${error.message}")
                }
            )
        }
    }

    fun loadAddresses() {
        viewModelScope.launch {
            _addressesState.value = UiState.Loading

            val userId = getUserId()
            repository.getAddresses(userId).fold(
                onSuccess = { addresses ->
                    _addressesState.value = UiState.Success(addresses)
                    Log.d(TAG, "✅ ${addresses.size} direcciones cargadas")
                },
                onFailure = { error ->
                    _addressesState.value = UiState.Error(error.message ?: "Error al cargar direcciones")
                    Log.e(TAG, "Error cargando direcciones", error)
                }
            )
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading

            repository.deleteAddress(id).fold(
                onSuccess = {
                    _deleteState.value = UiState.Success(Unit)
                    loadAddresses()
                    Log.d(TAG, "✅ Dirección eliminada")
                },
                onFailure = { error ->
                    _deleteState.value = UiState.Error(error.message ?: "Error al eliminar")
                    Log.e(TAG, "Error eliminando dirección", error)
                }
            )
        }
    }

    fun resetForm() {
        _formState.value = AddressFormState()
    }

    fun resetSaveState() {
        _saveState.value = UiState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = UiState.Idle
    }

    fun loadAddressForEditing(id: String) {
        viewModelScope.launch {
            val userId = getUserId()
            repository.getAddresses(userId).fold(
                onSuccess = { addresses ->
                    val address = addresses.find { it.id == id }
                    if (address != null) {
                        _editingAddressId.value = id
                        _formState.value = AddressFormState(
                            firstName = address.firstName,
                            lastName = address.lastName,
                            phone = address.phone,
                            street = address.street ?: "",
                            addressExtra = address.addressExtra ?: "",
                            city = address.city ?: "",
                            postalCode = address.postalCode ?: "",
                            isManualMode = address.isManual
                        )
                        Log.d(TAG, "✅ Dirección cargada para edición: $id")
                    } else {
                        Log.e(TAG, "Dirección no encontrada: $id")
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error cargando dirección para edición", error)
                }
            )
        }
    }

    fun updateAddress() {
        viewModelScope.launch {
            val form = _formState.value
            val addressId = _editingAddressId.value

            if (addressId == null) {
                _updateState.value = UiState.Error("ID de dirección no válido")
                return@launch
            }

            if (!form.isValid) {
                _updateState.value = UiState.Error("Completa todos los campos obligatorios")
                return@launch
            }

            _updateState.value = UiState.Loading

            val userId = getUserId()
            val fullAddress = "${form.street}, ${form.city}, República Dominicana"

            Log.d(TAG, "🌍 Geocodificando dirección: $fullAddress")

            repository.geocodeAddress(fullAddress).fold(
                onSuccess = { coords ->
                    val address = Address(
                        id = addressId,
                        userId = userId,
                        firstName = form.firstName,
                        lastName = form.lastName,
                        phone = form.phone,
                        formattedAddress = fullAddress,
                        latitude = coords.first,
                        longitude = coords.second,
                        street = form.street,
                        addressExtra = form.addressExtra.takeIf { it.isNotBlank() },
                        city = form.city,
                        postalCode = form.postalCode.takeIf { it.isNotBlank() },
                        isManual = true
                    )

                    repository.updateAddress(addressId, address).fold(
                        onSuccess = { updatedAddress ->
                            _updateState.value = UiState.Success(updatedAddress)
                            resetForm()
                            _editingAddressId.value = null
                            loadAddresses()
                            Log.d(TAG, "✅ Dirección actualizada: ${updatedAddress.id}")
                        },
                        onFailure = { error ->
                            _updateState.value = UiState.Error(error.message ?: "Error al actualizar")
                            Log.e(TAG, "Error actualizando dirección", error)
                        }
                    )
                },
                onFailure = { error ->
                    _updateState.value = UiState.Error(
                        "💡 No encontramos esa dirección.\n\n" +
                        "Sugerencias:\n" +
                        "• Escribe la dirección más completa (calle, número, ciudad)\n" +
                        "• Verifica la ortografía de la dirección\n" +
                        "• Ejemplo: Av. Abraham Lincoln 123, Santo Domingo"
                    )
                    Log.e(TAG, "Error geocodificando: ${error.message}")
                }
            )
        }
    }

    fun setDefaultAddress(id: String) {
        viewModelScope.launch {
            _setDefaultState.value = UiState.Loading

            val userId = getUserId()
            repository.setDefaultAddress(id, userId).fold(
                onSuccess = {
                    _setDefaultState.value = UiState.Success(Unit)

                    val currentState = _addressesState.value
                    if (currentState is UiState.Success) {
                        val updatedAddresses = currentState.data.map { address ->
                            when {
                                address.id == id -> address.copy(isDefault = true)
                                address.isDefault -> address.copy(isDefault = false)
                                else -> address
                            }
                        }
                        _addressesState.value = UiState.Success(updatedAddresses)
                    }

                    Log.d(TAG, "✅ Dirección predeterminada establecida")
                },
                onFailure = { error ->
                    _setDefaultState.value = UiState.Error(error.message ?: "Error al establecer predeterminada")
                    Log.e(TAG, "Error estableciendo dirección predeterminada", error)
                }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UiState.Idle
    }

    fun resetSetDefaultState() {
        _setDefaultState.value = UiState.Idle
    }

    fun cancelEditing() {
        _editingAddressId.value = null
        resetForm()
    }
}
