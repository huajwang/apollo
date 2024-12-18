package com.goodfeel.nightgrass.data

import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("e_mall_product_review")
data class ProductReview(
    val reviewId: Long? = null,
    val productId: Long,
    val reviewer: String,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
