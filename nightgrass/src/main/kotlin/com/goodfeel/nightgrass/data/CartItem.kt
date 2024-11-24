package com.goodfeel.nightgrass.data

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_cart_item")
data class CartItem(
    @Id
    val itemId: Long? = null,
    val cartId: Long,
    val productId: Long,
    var quantity: Int,
    var properties: String = "",
    var isSelected: Boolean = true
) {

    companion object {
        private val objectMapper = ObjectMapper()
    }

    fun getPropertiesAsMap(): Map<String, String> {
        return objectMapper.readValue(
            properties,
            object : com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {})
    }

    fun setPropertiesFromMap(map: Map<String, String>) {
        properties = objectMapper.writeValueAsString(map)
    }
}
