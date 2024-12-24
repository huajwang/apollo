package com.goodfeel.nightgrass.dto.admin

import com.goodfeel.nightgrass.util.ProductCategory
import java.math.BigDecimal

data class AdminProductDto(
    val productId: Long? = null,
    var productName: String? = null,
    var description: String? = null,
    var imageUrl: String? = null,
    var price: BigDecimal? = null,
    var additionalInfoMap: Map<String, String> = emptyMap(),
    var category: ProductCategory = ProductCategory.NONE
)
