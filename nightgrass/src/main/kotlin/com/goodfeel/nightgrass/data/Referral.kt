package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("e_mall_referral")
data class Referral(
    @Id
    val id: Long? = null, // Nullable for auto-generated ID

    @Column("sharer_id")
    val sharerId: String, // User ID of the sharer

    @Column("referral_code")
    val referralCode: String, // Unique referral code

    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now() // Timestamp of link creation
)
