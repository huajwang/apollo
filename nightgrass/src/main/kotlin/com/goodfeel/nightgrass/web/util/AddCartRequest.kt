package com.goodfeel.nightgrass.web.util

data class AddCartRequest (
    val productId: Long,
    var properties: MutableMap<String, String>?
)
