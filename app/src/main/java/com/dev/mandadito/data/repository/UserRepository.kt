package com.dev.mandadito.data.repository

import com.dev.mandadito.data.models.UserProfile

class UserRepository {

    suspend fun getAllUsers(): Result<List<UserProfile>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, user: UserProfile): Result<UserProfile> {
        return try {
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}