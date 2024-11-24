package com.goodfeel.nightgrass.dto

import java.math.BigDecimal

data class CartItemDto(
    val itemId: Long? = null,
    val productId: Long,
    val imageUrl: String,
    val productName: String,
    val description: String,
    val quantity: Int,
    val properties: String,
    val price: BigDecimal,
    val formattedPrice: String? = null,
    val isSelected: Boolean = true
)
