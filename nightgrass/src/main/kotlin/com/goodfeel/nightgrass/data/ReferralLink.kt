package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("referral_links")
data class ReferralLink(
    @Id
    val id: Long? = null, // Nullable for auto-generated ID

    val sharerId: Long, // User ID of the sharer

    val referralCode: String, // Unique referral code

    val createdAt: LocalDateTime = LocalDateTime.now() // Timestamp of link creation
)
