package com.dev.mandadito.data.repository

import android.content.Context
import android.util.Log
import com.dev.mandadito.data.network.ApiService
import com.dev.mandadito.data.models.CreateStripeAccountRequest
import com.dev.mandadito.data.models.StripeAccountResponse
import com.dev.mandadito.data.models.CheckStripeStatusRequest
import com.dev.mandadito.data.models.StripeStatusResponse
import com.dev.mandadito.utils.SharedPreferenHelper

class StripeRepository(context: Context) {

    private val apiService = ApiService.create()
    private val sharedPrefsHelper = SharedPreferenHelper(context)
    private val TAG = "StripeRepository"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Crea una cuenta Stripe Connect para el colmado
     */
    suspend fun createStripeAccount(
        colmadoId: String,
        email: String,
        businessName: String
    ): Result<StripeAccountResponse> {
        return try {
            Log.d(TAG, "🔥 Creando cuenta Stripe para colmado: $colmadoId")

            val request = CreateStripeAccountRequest(
                colmadoId = colmadoId,
                email = email,
                businessName = businessName
            )

            val response = apiService.createStripeAccount(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Log.d(TAG, "✅ Cuenta Stripe creada: ${body.accountId}")
                    Result.Success(body)
                } else {
                    Log.e(TAG, "❌ Error creando cuenta: ${body.message}")
                    Result.Error(body.message ?: "Error desconocido")
                }
            } else {
                val error = "Error HTTP: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.Error(error)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al crear cuenta Stripe: ${e.message}", e)
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Verifica el estado de la cuenta Stripe del colmado
     */
    suspend fun checkStripeStatus(colmadoId: String): Result<StripeStatusResponse> {
        return try {
            Log.d(TAG, "🔍 Verificando estado Stripe para colmado: $colmadoId")

            val request = CheckStripeStatusRequest(colmadoId = colmadoId)
            val response = apiService.checkStripeStatus(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Log.d(TAG, "✅ Estado obtenido - Onboarding: ${body.onboardingCompleted}, Charges: ${body.chargesEnabled}")
                    Result.Success(body)
                } else {
                    Log.e(TAG, "❌ Error verificando estado: ${body.message}")
                    Result.Error(body.message ?: "Error desconocido")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val error = "Error HTTP ${response.code()}: $errorBody"
                Log.e(TAG, "❌ $error")
                Result.Error(error)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al verificar estado: ${e.message}", e)
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Refresca el link de onboarding si expiró
     */
    suspend fun refreshOnboardingLink(colmadoId: String): Result<StripeAccountResponse> {
        return try {
            Log.d(TAG, "🔄 Refrescando link de onboarding para: $colmadoId")

            val request = CheckStripeStatusRequest(colmadoId = colmadoId)
            val response = apiService.refreshStripeOnboardingLink(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Log.d(TAG, "✅ Link refrescado")
                    Result.Success(body)
                } else {
                    Log.e(TAG, "❌ Error refrescando link: ${body.message}")
                    Result.Error(body.message ?: "Error desconocido")
                }
            } else {
                val error = "Error HTTP: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.Error(error)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al refrescar link: ${e.message}", e)
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
