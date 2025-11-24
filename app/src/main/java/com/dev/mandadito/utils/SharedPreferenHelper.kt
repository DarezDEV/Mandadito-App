package com.dev.mandadito.utils

import android.content.Context
import android.content.SharedPreferences
import com.dev.mandadito.data.models.Role

class SharedPreferenHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "mandadito_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    data class UserSession(
        val userId: String?,
        val email: String?,
        val userName: String?,
        val role: Role?,
        val sessionToken: String?
    )

    /**
     * Guarda la sesión del usuario
     */
    fun saveUserSession(
        email: String,
        role: Role,
        userId: String,
        userName: String,
        sessionToken: String
    ) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ROLE, role.value)
            putString(KEY_SESSION_TOKEN, sessionToken)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Obtiene la sesión actual del usuario
     */
    fun getUserSession(): UserSession {
        return UserSession(
            userId = prefs.getString(KEY_USER_ID, null),
            email = prefs.getString(KEY_USER_EMAIL, null),
            userName = prefs.getString(KEY_USER_NAME, null),
            role = prefs.getString(KEY_USER_ROLE, null)?.let { Role.fromString(it) },
            sessionToken = prefs.getString(KEY_SESSION_TOKEN, null)
        )
    }

    /**
     * Verifica si el usuario está logueado
     */
    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Obtiene el rol del usuario
     */
    fun getUserRole(): Role? {
        val roleValue = prefs.getString(KEY_USER_ROLE, null)
        return roleValue?.let { Role.fromString(it) }
    }

    /**
     * Obtiene el ID del usuario
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Obtiene el email del usuario
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Obtiene el nombre del usuario
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Limpia la sesión del usuario
     */
    fun clearUserSession() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROLE)
            remove(KEY_SESSION_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    /**
     * Actualiza el nombre del usuario
     */
    fun updateUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    /**
     * Actualiza el rol del usuario
     */
    fun updateUserRole(role: Role) {
        prefs.edit().putString(KEY_USER_ROLE, role.value).apply()
    }
}