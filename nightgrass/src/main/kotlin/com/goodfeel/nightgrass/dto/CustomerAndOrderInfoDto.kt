package com.goodfeel.nightgrass.dto

data class CustomerAndOrderInfoDto(
    val orderId: Long,
    val customerName: String,
    val phone: String? = null,
    val address: String
)
