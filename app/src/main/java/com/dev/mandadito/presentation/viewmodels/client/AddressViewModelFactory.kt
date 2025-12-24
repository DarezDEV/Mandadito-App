package com.dev.mandadito.presentation.viewmodels.client

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dev.mandadito.data.repository.AddressRepository
import com.google.android.libraries.places.api.Places

class AddressViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddressViewModel::class.java)) {
            // Crear PlacesClient (será null si Places no está inicializado)
            val placesClient = try {
                Places.createClient(context)
            } catch (e: Exception) {
                null // Usará datos mock
            }

            // Crear el repositorio con las dependencias
            val repository = AddressRepository(placesClient, context)

            // Retornar el ViewModel
            return AddressViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}