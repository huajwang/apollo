package com.goodfeel.nightgrass.data

import com.goodfeel.nightgrass.util.ReferralRewardStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("e_mall_referral_rewards")
data class ReferralReward(
    @Id
    val id: Long? = null, // Nullable for auto-generated ID
    val sharerId: String, // User ID of the sharer
    val orderId: Long, // Associated order ID
    val rewardAmount: BigDecimal, // 10% reward amount
    val createdAt: LocalDateTime = LocalDateTime.now(), // Timestamp of reward creation
    val referralRewardStatus: ReferralRewardStatus
)
