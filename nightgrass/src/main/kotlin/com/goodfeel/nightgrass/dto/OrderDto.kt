package com.goodfeel.nightgrass.dto

import com.goodfeel.nightgrass.util.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderDto(
    val orderId: Long,
    val orderNo: String,
    val userId: String,
    val deliveryAddress: String? = null,
    val originalTotal: BigDecimal,
    val discountedTotal: BigDecimal,
    val hst: BigDecimal,
    val shippingFee: BigDecimal,
    val orderTotal: BigDecimal,
    val createdAt: LocalDateTime,
    val orderProcessDate: LocalDateTime? = null,
    val logisticsNo: String? = null,
    val deliveryDate: LocalDateTime? = null,
    val orderStatus: OrderStatus
)
