package com.goodfeel.nightgrass.dto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class OrderItemDto(
    val productName: String,
    val imageUrl: String,
    val quantity: Int,
    val properties: String? = null,
    val unitPrice: BigDecimal,
    var formattedProperties: Map<String, String> = mapOf()
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
