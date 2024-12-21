package com.goodfeel.nightgrass.dto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.goodfeel.nightgrass.util.DiscountType
import com.goodfeel.nightgrass.util.ProductCategory
import java.math.BigDecimal
import java.math.RoundingMode

data class ProductDto(
    val productId: Long,
    val productName: String,
    val description: String,
    val imageUrl: String,
    val price: BigDecimal,
    var additionalInfo: String? = null,
    var additionalInfoMap: Map<String, String> = emptyMap(),
    val category: ProductCategory,
    val discountType: DiscountType? = null,
    val discountValue: BigDecimal? = null,
    var discountedPrice: BigDecimal? = null,
    var isPercentageDiscounted: Boolean = false,
    var isFlatDiscounted: Boolean = false,
    var showNewProductBadge: Boolean = false
) {
    companion object {
        private val objectMapper = ObjectMapper()
    }

    /**
     * Calculates the discounted price based on discount type and value.
     * Updates the discountedPrice field if discount details are available.
     */
    fun processProductDto() {
        if (discountType != null && discountValue == null)
            throw IllegalStateException("discountValue is null for discounted product")
        discountedPrice = when (discountType) {
            DiscountType.PERCENTAGE -> {
                isPercentageDiscounted = true
                val discountPercentage = discountValue!!.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
                price.subtract(price.multiply(discountPercentage))
                    .setScale(2, RoundingMode.HALF_UP)
            }
            DiscountType.FLAT -> {
                isFlatDiscounted = true
                price.subtract(discountValue).setScale(2, RoundingMode.HALF_UP)
            }
            else -> null
        }
        if (discountType == null) {
            showNewProductBadge = category == ProductCategory.NEW
        }

        additionalInfoMap = additionalInfo?.let {
            objectMapper.readValue(it, object : TypeReference<Map<String, String>>() {})
        } ?: emptyMap()
    }

}
