package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("e_mall_cart")
data class Cart(
    @Id
    val cartId: Long? = null,
    var total: BigDecimal = BigDecimal.ZERO,
    val userId: String
)
