package com.goodfeel.nightgrass.dto

import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class OrderItemDto(
    val productName: String,
    val imageUrl: String,
    val quantity: Int,
    val properties: String,
    val unitPrice: BigDecimal,
    var formattedProperties: Map<String, String> = mapOf()
) {
    companion object {
        private val objectMapper = ObjectMapper()
    }

    fun processProperties() {
        formattedProperties = try {
            objectMapper.readValue(properties, Map::class.java) as Map<String, String>
        } catch (e: Exception) {
            mapOf("Error" to "Invalid JSON")
        }
    }
}
