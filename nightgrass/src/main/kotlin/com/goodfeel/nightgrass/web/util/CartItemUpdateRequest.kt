package com.goodfeel.nightgrass.web.util

data class CartItemUpdateRequest(
    val itemId: Long,
    val quantity: Int? = null,
    // the below is for check box
    val isChecked: Boolean? = null
)
