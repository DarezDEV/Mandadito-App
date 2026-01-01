package com.dev.mandadito.data.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Deserializador personalizado para OrderStatus
 * Convierte el string del JSON al enum OrderStatus
 */
class OrderStatusDeserializer : JsonDeserializer<OrderStatus> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): OrderStatus {
        val statusString = json?.asString ?: "pending"
        return OrderStatus.fromString(statusString)
    }
}
