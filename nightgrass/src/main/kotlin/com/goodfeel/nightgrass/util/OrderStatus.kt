package com.goodfeel.nightgrass.util

enum class OrderStatus {
    CHECKOUT,   // Building order, not paid yet
    SUBMITTED,  // Order has been submitted for processing
    PROCESSING, // Order is currently being processed
    SHIPPING,    // Order is being shipped to the customer
    CANCELED,   // Order has been canceled
    COMPLETED   // Order has been completed successfully
}
