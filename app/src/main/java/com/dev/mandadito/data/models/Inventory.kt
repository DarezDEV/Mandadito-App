package com.dev.mandadito.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Inventory(
    @SerialName("id") val id: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("category") val category: String? = null,
    @SerialName("current_stock") val currentStock: Int = 0,
    @SerialName("min_stock") val minStock: Int = 5,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("total_value") val totalValue: Double = 0.0,
    @SerialName("location") val location: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: String = "IN_STOCK",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)