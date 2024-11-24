package com.goodfeel.nightgrass.web.util

import java.math.BigDecimal

data class CheckoutRequest(
    val orderId: Long,
    var amount: BigDecimal
)
