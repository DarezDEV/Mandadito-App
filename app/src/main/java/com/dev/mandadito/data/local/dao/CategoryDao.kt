package com.dev.mandadito.data.local.dao

import androidx.room.*
import com.dev.mandadito.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ===== INSERTS =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // ===== QUERIES =====

    @Query("SELECT * FROM categories WHERE colmado_id = :colmadoId AND is_active = 1")
    fun getActiveCategoriesFlow(colmadoId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE colmado_id = :colmadoId AND is_active = 1")
    suspend fun getActiveCategories(colmadoId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE colmado_id = :colmadoId")
    suspend fun getAllCategories(colmadoId: String): List<CategoryEntity>

    // ===== DELETE =====

    @Query("DELETE FROM categories WHERE colmado_id = :colmadoId")
    suspend fun deleteCategoriesByColmado(colmadoId: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    // ===== COUNT =====

    @Query("SELECT COUNT(*) FROM categories WHERE colmado_id = :colmadoId")
    suspend fun getCategoryCount(colmadoId: String): Int
}
