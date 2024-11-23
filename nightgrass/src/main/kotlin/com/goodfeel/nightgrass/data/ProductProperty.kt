package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_product_property")
data class ProductProperty(
    @Id val propertyId: Long? = null,
    val productId: Long,
    val propertyName: String,
    val propertyValue: String
)
