package com.goodfeel.nightgrass.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import com.goodfeel.nightgrass.dto.OrderItemDto
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

    companion object {
        private val objectMapper = ObjectMapper()
    }

    fun getPropertiesAsMap(): Map<String, String> {
        return properties?.let {
            objectMapper.readValue(
                properties,
                object : TypeReference<Map<String, String>>() {})
        } ?: mapOf()

    }

    fun setPropertiesFromMap(map: Map<String, String>) {
        properties = objectMapper.writeValueAsString(map)
    }

    fun toDto(): OrderItemDto {
        return OrderItemDto(
            productName = this.productName,
            imageUrl = this.imageUrl,
            quantity = this.quantity,
            properties = this.properties,
            unitPrice = this.unitPrice
        ).apply {
            this.processProperties()
        }
    }
}
