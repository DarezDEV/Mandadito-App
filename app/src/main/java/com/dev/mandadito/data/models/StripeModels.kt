package com.dev.mandadito.data.models

import com.google.gson.annotations.SerializedName

/**
 * Request para crear una cuenta Stripe Connect
 */
data class CreateStripeAccountRequest(
    @SerializedName("colmado_id")
    val colmadoId: String,
    val email: String,
    @SerializedName("business_name")
    val businessName: String
)

/**
 * Response al crear o actualizar una cuenta Stripe
 */
data class StripeAccountResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("account_id")
    val accountId: String? = null,
    @SerializedName("onboarding_url")
    val onboardingUrl: String? = null
)

/**
 * Request para verificar estado de Stripe
 */
data class CheckStripeStatusRequest(
    @SerializedName("colmado_id")
    val colmadoId: String
)

/**
 * Response del estado de la cuenta Stripe
 */
data class StripeStatusResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("has_account")
    val hasAccount: Boolean? = false,
    @SerializedName("onboarding_completed")
    val onboardingCompleted: Boolean? = false,
    @SerializedName("charges_enabled")
    val chargesEnabled: Boolean? = false,
    @SerializedName("payouts_enabled")
    val payoutsEnabled: Boolean? = false,
    @SerializedName("account_id")
    val accountId: String? = null,
    @SerializedName("onboarding_url")
    val onboardingUrl: String? = null
)
