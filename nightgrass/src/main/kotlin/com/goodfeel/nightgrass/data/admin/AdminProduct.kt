package com.goodfeel.nightgrass.data.admin

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.goodfeel.nightgrass.util.ProductCategory
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("e_mall_product")
data class AdminProduct(
    @Id
    val productId: Long? = null,
    var productName: String,
    var description: String,
    var imageUrl: String,  // small image. other photos store in another table
    var price: BigDecimal,
    var additionalInfo: String? = null,
    var category: ProductCategory = ProductCategory.NONE
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