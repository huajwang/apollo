package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("referral_rewards")
data class ReferralReward(
    @Id
    val id: Long? = null, // Nullable for auto-generated ID

    val sharerId: Long, // User ID of the sharer

    val orderId: Long, // Associated order ID

    val rewardAmount: BigDecimal, // 10% reward amount

    val createdAt: LocalDateTime = LocalDateTime.now() // Timestamp of reward creation
)
