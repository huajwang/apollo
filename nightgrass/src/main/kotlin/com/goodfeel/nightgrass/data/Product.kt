package com.goodfeel.nightgrass.data

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.goodfeel.nightgrass.util.ProductCategory
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("e_mall_product")
data class Product(
    @Id
    val productId: Long? = null,
    val productName: String,
    val description: String,
    val imageUrl: String,  // small image. other photos store in another table
    val price: BigDecimal,
    var additionalInfo: String? = null,
    val category: ProductCategory = ProductCategory.NONE
) {
    companion object {
        private val objectMapper = ObjectMapper()
    }

    fun setAdditionalInfoFromMap(map: Map<String, String>) {
        additionalInfo = objectMapper.writeValueAsString(map)
    }

    fun getAdditionalInfoAsMap(): Map<String, String> {
        return additionalInfo?.let {
            objectMapper.readValue(it, object : TypeReference<Map<String, String>>() {})
        } ?: emptyMap()
    }
}
