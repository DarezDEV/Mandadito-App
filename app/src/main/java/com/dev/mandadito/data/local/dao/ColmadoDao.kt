package com.dev.mandadito.data.local.dao

import androidx.room.*
import com.dev.mandadito.data.local.entities.ColmadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColmadoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColmado(colmado: ColmadoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColmados(colmados: List<ColmadoEntity>)

    @Query("SELECT * FROM colmados WHERE id = :colmadoId")
    suspend fun getColmadoById(colmadoId: String): ColmadoEntity?

    @Query("SELECT * FROM colmados WHERE seller_id = :sellerId")
    suspend fun getColmadosBySeller(sellerId: String): List<ColmadoEntity>

    @Query("SELECT * FROM colmados WHERE is_active = 1")
    suspend fun getActiveColmados(): List<ColmadoEntity>

    @Query("SELECT * FROM colmados")
    suspend fun getAllColmados(): List<ColmadoEntity>

    @Query("DELETE FROM colmados WHERE id = :colmadoId")
    suspend fun deleteColmado(colmadoId: String)

    @Query("DELETE FROM colmados WHERE seller_id = :sellerId")
    suspend fun deleteColmadosBySeller(sellerId: String)

    @Query("DELETE FROM colmados")
    suspend fun clearAllColmados()

    /**
     * Observar cambios en un colmado en tiempo real
     */
    @Query("SELECT * FROM colmados WHERE id = :colmadoId")
    fun observeColmado(colmadoId: String): Flow<ColmadoEntity?>

    /**
     * Observar todos los colmados activos
     */
    @Query("SELECT * FROM colmados WHERE is_active = 1 ORDER BY name ASC")
    fun observeActiveColmados(): Flow<List<ColmadoEntity>>
}
