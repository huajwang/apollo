package com.goodfeel.nightgrass.dto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class CartItemDto(
    val itemId: Long? = null,
    val productId: Long,
    val imageUrl: String,
    val productName: String,
    val description: String,
    val quantity: Int,
    val properties: String? = null,
    var formattedProperties: Map<String, String> = mapOf(),
    val price: BigDecimal,
    val discountedPrice: BigDecimal,
    val formattedPrice: String? = null,
    val isSelected: Boolean = true
) {
    companion object {
       private val objectMapper = ObjectMapper()
    }

    fun processProperties() {
        formattedProperties = properties?.let {
            objectMapper.readValue(it, object : TypeReference<Map<String, String>>() {})
        } ?: mapOf()
    }
}
