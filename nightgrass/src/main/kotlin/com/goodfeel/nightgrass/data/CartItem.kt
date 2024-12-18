package com.goodfeel.nightgrass.data

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import com.fasterxml.jackson.core.type.TypeReference

@Table("e_mall_cart_item")
data class CartItem(
    @Id
    val itemId: Long? = null,
    val cartId: Long,
    val productId: Long,
    var quantity: Int,
    var properties: String? = null,
    var isSelected: Boolean = true
) {

    companion object {
        private val objectMapper = ObjectMapper()
    }

    fun getPropertiesAsMap(): Map<String, String> {
        return properties?.let {
            objectMapper.readValue(
                it,
                object : TypeReference<Map<String, String>>() {})
        } ?: emptyMap()

    }

    fun setPropertiesFromMap(map: Map<String, String>) {
        properties = objectMapper.writeValueAsString(map)
    }
}
