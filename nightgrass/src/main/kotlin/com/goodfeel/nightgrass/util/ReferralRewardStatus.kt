package com.goodfeel.nightgrass.util

enum class ReferralRewardStatus {
    PENDING,    // Reward initiated, waiting for validation
    APPROVED,   // Reward validated and approved
    REJECTED,   // Reward invalidated due to unmet conditions or fraud
    EXPIRED,    // Reward expired due to inactivity
    CANCELED,   // Reward manually canceled
    CLAIMED     // Reward redeemed by the sharer
}
