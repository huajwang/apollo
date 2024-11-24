package com.goodfeel.nightgrass.web.util

data class AddCartRequest (
    val itemId: Long? = null,
    val productId: Long,
    val properties: MutableMap<String, String> = mutableMapOf()
)
