package com.goodfeel.nightgrass.data.admin

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_admin")
data class Admin(
    @Id val id: Long? = null,
    val username: String,
    val password: String, // Store hashed password
    val role: String = "ROLE_ADMIN"
)
