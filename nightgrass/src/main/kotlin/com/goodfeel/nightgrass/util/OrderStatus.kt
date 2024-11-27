package com.goodfeel.nightgrass.util

enum class OrderStatus {
    PENDING,  // checkout of shopping cart, unpaid
    PROCESSING, // Order paid and is currently being processed
    SHIPPING,    // Order is being shipped to the customer
    CANCELED,   // Order has been canceled
    COMPLETED   // Order has been completed successfully
}
