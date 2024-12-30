package com.goodfeel.nightgrass.web.util

import java.math.BigDecimal

data class CheckoutRequest(
    val orderId: Long,
    val amount: BigDecimal,
    val contactName: String,
    val contactPhone: String,
    val deliveryAddress: String
)
