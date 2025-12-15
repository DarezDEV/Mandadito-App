package com.dev.mandadito.data.models

data class Inventory(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productName: String = "",
    val category: String? = null,
    val currentStock: Int = 0,
    val minStock: Int = 5,
    val unitPrice: Double = 0.0,
    val totalValue: Double = 0.0,
    val location: String? = null,
    val description: String? = null,
    val status: String = "IN_STOCK", // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    val createdAt: String = "",
    val updatedAt: String = ""
)