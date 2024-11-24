package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_product_photo")
data class ProductPhoto(
    @Id
    val id: Long? = null,
    val productId: Long,
    val photoUrl: String
)
