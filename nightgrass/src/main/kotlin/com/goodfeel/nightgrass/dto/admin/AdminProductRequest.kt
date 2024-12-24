package com.goodfeel.nightgrass.dto.admin

import com.goodfeel.nightgrass.util.ProductCategory
import java.math.BigDecimal


data class AdminProductRequest (
    val productId: Long? = null,
    var productName: String,
    var description: String,
    var imageUrl: String,
    var price: BigDecimal,
    var additionalInfoMap: Map<String, String> = mutableMapOf(),
    var category: ProductCategory = ProductCategory.NONE
)
