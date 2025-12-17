package com.goodfeel.nightgrass.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_address")
data class Address(
    @Id
    @Column("address_id")
    val id: Long? = null,
    @Column("user_id")
    val userId: Long,
    @Column("customer_name")
    val customerName: String? = null,
    val phone: String? = null,
    @Column("address_line")
    val addressLine: String? = null,
    val city: String? = null,
    @Column("postal_code")
    val postalCode: String? = null,
    @Column("is_default")
    val isDefault: Boolean = false
)
