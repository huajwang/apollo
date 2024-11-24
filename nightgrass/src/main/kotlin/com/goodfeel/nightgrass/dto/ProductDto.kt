package com.goodfeel.nightgrass.dto

import java.math.BigDecimal

data class ProductDto(
    val productId: Long,
    val productName: String,
    val description: String,
    val imageUrl: String,
    val price: BigDecimal
)
