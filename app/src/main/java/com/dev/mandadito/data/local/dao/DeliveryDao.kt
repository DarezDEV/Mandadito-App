package com.dev.mandadito.data.local.dao

import androidx.room.*
import com.dev.mandadito.data.local.entities.DeliveryUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryUser(user: DeliveryUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryUsers(users: List<DeliveryUserEntity>)

    @Query("SELECT * FROM delivery_users WHERE id = :userId")
    suspend fun getDeliveryUserById(userId: String): DeliveryUserEntity?

    @Query("SELECT * FROM delivery_users WHERE colmado_id = :colmadoId AND activo = 1")
    suspend fun getActiveDeliveryUsersByColmado(colmadoId: String): List<DeliveryUserEntity>

    @Query("SELECT * FROM delivery_users WHERE colmado_id = :colmadoId")
    suspend fun getAllDeliveryUsersByColmado(colmadoId: String): List<DeliveryUserEntity>

    @Query("SELECT * FROM delivery_users WHERE email = :email")
    suspend fun getDeliveryUserByEmail(email: String): DeliveryUserEntity?

    @Query("DELETE FROM delivery_users WHERE id = :userId")
    suspend fun deleteDeliveryUser(userId: String)

    @Query("DELETE FROM delivery_users WHERE colmado_id = :colmadoId")
    suspend fun deleteDeliveryUsersByColmado(colmadoId: String)

    @Query("DELETE FROM delivery_users")
    suspend fun clearAllDeliveryUsers()

    /**
     * Observar cambios en deliveries de un colmado en tiempo real
     */
    @Query("SELECT * FROM delivery_users WHERE colmado_id = :colmadoId AND activo = 1 ORDER BY nombre ASC")
    fun observeActiveDeliveryUsers(colmadoId: String): Flow<List<DeliveryUserEntity>>
}
