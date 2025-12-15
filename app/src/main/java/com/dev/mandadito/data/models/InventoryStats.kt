package com.dev.mandadito.data.models

data class InventoryStats(
    val totalProducts: Int = 0,
    val totalValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0
)