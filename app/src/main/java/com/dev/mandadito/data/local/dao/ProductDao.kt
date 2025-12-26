package com.dev.mandadito.data.local.dao

import androidx.room.*
import com.dev.mandadito.data.local.entities.ProductEntity
import com.dev.mandadito.data.local.entities.ProductCategoryCrossRef
import com.dev.mandadito.data.local.entities.ProductWithCategoriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // ===== INSERTS =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductCategoryRefs(refs: List<ProductCategoryCrossRef>)

    // ===== QUERIES =====

    @Transaction
    @Query("SELECT * FROM products WHERE colmado_id = :colmadoId AND is_active = 1")
    fun getActiveProductsFlow(colmadoId: String): Flow<List<ProductWithCategoriesEntity>>

    @Transaction
    @Query("SELECT * FROM products WHERE colmado_id = :colmadoId AND is_active = 1")
    suspend fun getActiveProducts(colmadoId: String): List<ProductWithCategoriesEntity>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): ProductWithCategoriesEntity?

    @Query("SELECT * FROM products WHERE colmado_id = :colmadoId")
    suspend fun getAllProducts(colmadoId: String): List<ProductEntity>

    // ===== DELETE =====

    @Query("DELETE FROM products WHERE colmado_id = :colmadoId")
    suspend fun deleteProductsByColmado(colmadoId: String)

    @Query("DELETE FROM product_category_cross_ref WHERE productId = :productId")
    suspend fun deleteProductCategoryRefs(productId: String)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    // ===== COUNT =====

    @Query("SELECT COUNT(*) FROM products WHERE colmado_id = :colmadoId")
    suspend fun getProductCount(colmadoId: String): Int
}
