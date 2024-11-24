package com.goodfeel.nightgrass.dto

import java.math.BigDecimal

data class OrderItemDto(
    val productName: String,
    val imageUrl: String,
    val quantity: Int,
    val properties: String,
    val unitPrice: BigDecimal
)
