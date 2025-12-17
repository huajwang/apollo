package com.goodfeel.nightgrass.dto

/**
 * Request DTO for placing an order.
 * Can be used by both authenticated users and guests.
 */
data class PlaceOrderRequest(
    val items: List<OrderItemRequest>,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val shippingFee: Double = 0.0,
    val fullName: String,
    val phone: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val paymentMethod: String
)

data class OrderItemRequest(
    val productId: Long,
    val productName: String,
    val imageUrl: String,
    val quantity: Int,
    val price: Double,
    val properties: String? = null
)
