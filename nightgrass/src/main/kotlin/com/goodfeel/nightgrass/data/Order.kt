package com.goodfeel.nightgrass.data

import com.goodfeel.nightgrass.util.OrderStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("e_mall_order")
data class Order(
    @Id val orderId: Long? = null,
    val orderNo: String,
    val userId: String,
    var deliveryAddress: String? = null,
    var contactName: String? = null,
    var contactPhone: String? = null,
    val total: BigDecimal,
    val hst: BigDecimal? = null,
    var finalTotal: BigDecimal? = null,
    val createdAt: LocalDateTime,
    var orderStatus: OrderStatus,
    val updatedDate: LocalDateTime? = null,
    val logisticsNo: String? = null,
    val deliveryDate: LocalDateTime? = null,
    val payNo: String? = null,
    val payType: String? = null,
    val remark: String? = null
)