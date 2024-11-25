package com.goodfeel.nightgrass.dto

import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class CartItemDto(
    val itemId: Long? = null,
    val productId: Long,
    val imageUrl: String,
    val productName: String,
    val description: String,
    val quantity: Int,
    val properties: String,
    var formattedProperties: Map<String, String> = mapOf(),
    val price: BigDecimal,
    val formattedPrice: String? = null,
    val isSelected: Boolean = true
) {
    companion object {
       private val objectMapper = ObjectMapper()
    }

    fun processProperties() {
        formattedProperties = try {
            objectMapper.readValue(properties, Map::class.java) as Map<String, String>
        } catch (e: Exception) {
            mapOf()
        }
    }
}
