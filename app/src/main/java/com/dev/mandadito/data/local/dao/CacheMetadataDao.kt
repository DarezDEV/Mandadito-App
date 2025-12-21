package com.dev.mandadito.data.local.dao

import androidx.room.*
import com.dev.mandadito.data.local.entities.CacheMetadata

@Dao
interface CacheMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: CacheMetadata)

    @Query("SELECT * FROM cache_metadata WHERE key = :key")
    suspend fun getMetadata(key: String): CacheMetadata?

    @Query("DELETE FROM cache_metadata WHERE key = :key")
    suspend fun deleteMetadata(key: String)

    @Query("DELETE FROM cache_metadata")
    suspend fun deleteAll()

    @Query("DELETE FROM cache_metadata WHERE timestamp < :expirationTime")
    suspend fun deleteExpired(expirationTime: Long)
}
