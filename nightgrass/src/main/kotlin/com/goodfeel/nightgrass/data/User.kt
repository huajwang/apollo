package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_user")
data class User(
    @Id
    val id: Long? = null,
    @Column("oauth_id")
    val oauthId: String? =null,
    val guestId: String? = null,
    val nickName: String? = null,
    val email: String? = null,
    var customerName: String? = null,
    var phone: String? = null,
    var address: String? = null
)
