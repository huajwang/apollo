package com.goodfeel.nightgrass.data

import com.goodfeel.nightgrass.util.ReferralRewardStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("e_mall_referral_rewards")
data class ReferralReward(
    @Id
    val id: Long? = null, // Nullable for auto-generated ID
    @Column("sharer_id")
    val sharerId: String, // User ID of the sharer
    @Column("order_id")
    val orderId: Long, // Associated order ID
    @Column("reward_amount")
    val rewardAmount: BigDecimal, // 10% reward amount
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(), // Timestamp of reward creation
    @Column("referral_reward_status")
    val referralRewardStatus: ReferralRewardStatus
)
