package com.goodfeel.nightgrass.data

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
    val price: BigDecimal
)
