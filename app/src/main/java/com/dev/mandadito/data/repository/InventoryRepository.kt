package com.dev.mandadito.data.repository

import com.dev.mandadito.data.models.Inventory
import com.dev.mandadito.data.models.InventoryStats
import com.dev.mandadito.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.from

class InventoryRepository {

    private val supabase = SupabaseClient.client

    suspend fun getInventoryBySeller(sellerId: String): Result<List<Inventory>> {
        return try {
            val result = supabase.from("inventory")
                .select()
                .decodeList<Inventory>()
                .filter { it.sellerId == sellerId }
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getInventoryStats(sellerId: String): Result<InventoryStats> {
        return try {
            val inventory = supabase.from("inventory")
                .select()
                .decodeList<Inventory>()
                .filter { it.sellerId == sellerId }

            val stats = InventoryStats(
                totalProducts = inventory.size,
                totalValue = inventory.sumOf { it.totalValue },
                lowStockCount = inventory.count { it.status == "LOW_STOCK" },
                outOfStockCount = inventory.count { it.status == "OUT_OF_STOCK" }
            )
            Result.success(stats)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun addInventoryItem(item: Inventory): Result<Inventory> {
        return try {
            val result = supabase.from("inventory")
                .insert(item) { select() }
                .decodeSingle<Inventory>()
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // NUEVA FUNCIÓN PARA ACTUALIZAR
    suspend fun updateInventoryItem(item: Inventory): Result<Inventory> {
        return try {
            val result = supabase.from("inventory")
                .update(item) {
                    filter { eq("id", item.id) }
                    select()
                }
                .decodeSingle<Inventory>()
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getLowStockProducts(sellerId: String): Result<List<Inventory>> {
        return try {
            val result = supabase.from("inventory")
                .select()
                .decodeList<Inventory>()
                .filter {
                    it.sellerId == sellerId &&
                            (it.status == "LOW_STOCK" || it.status == "OUT_OF_STOCK")
                }
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}