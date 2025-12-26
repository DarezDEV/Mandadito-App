package com.dev.mandadito.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dev.mandadito.data.local.dao.*
import com.dev.mandadito.data.local.entities.*

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        CacheMetadata::class,
        ProductCategoryCrossRef::class,
        // Cart entities
        CartEntity::class,
        CartItemEntity::class,
        CartItemDetailEntity::class,
        CartSummaryEntity::class,
        // Colmado entity
        ColmadoEntity::class,
        // Delivery entity
        DeliveryUserEntity::class
    ],
    version = 2, // Incrementar versión
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MandaditoDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun cartDao(): CartDao
    abstract fun colmadoDao(): ColmadoDao
    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: MandaditoDatabase? = null

        fun getDatabase(context: Context): MandaditoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MandaditoDatabase::class.java,
                    "mandadito_database"
                )
                    .fallbackToDestructiveMigration() // Para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
