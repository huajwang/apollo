package com.goodfeel.nightgrass.data.admin

import com.goodfeel.nightgrass.util.DiscountType
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("e_mall_discount")
data class Discount(
    val discountId: Long? = null,
    val productId: Long,
    val discountType: DiscountType,
    val discountValue: BigDecimal,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null
)
