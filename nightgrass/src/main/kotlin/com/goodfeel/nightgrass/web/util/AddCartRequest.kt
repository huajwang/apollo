package com.goodfeel.nightgrass.web.util

data class AddCartRequest (
    val productId: Long,
    val properties: MutableMap<String, String>?
)
