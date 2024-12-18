package com.goodfeel.nightgrass.data

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("e_mall_order_item")
data class OrderItem(
    @Id
    val orderItemId: Long? = null,
    val orderId: Long,
    val productName: String,
    val imageUrl: String,
    val quantity: Int,
    var properties: String? = null,
    val unitPrice: BigDecimal
) {
    fun getPropertiesAsMap(objectMapper: ObjectMapper): Map<String, String> {
        return objectMapper.readValue(
            properties,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {})
    }

    fun setPropertiesFromMap(objectMapper: ObjectMapper, map: Map<String, String>) {
        properties = objectMapper.writeValueAsString(map)
    }
}
