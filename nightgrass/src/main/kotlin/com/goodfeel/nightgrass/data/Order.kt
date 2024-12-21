package com.goodfeel.nightgrass.data

import com.goodfeel.nightgrass.dto.OrderDto
import com.goodfeel.nightgrass.util.OrderStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("e_mall_order")
data class Order(
    @Id val orderId: Long? = null,
    val orderNo: String,
    var userId: String,
    var deliveryAddress: String? = null,
    var contactName: String? = null,
    var contactPhone: String? = null,
    val originalTotal: BigDecimal,
    val discountedTotal: BigDecimal,
    val hst: BigDecimal,
    val shippingFee: BigDecimal,
    var finalTotal: BigDecimal,
    val createdAt: LocalDateTime,
    var orderStatus: OrderStatus,
    val updatedDate: LocalDateTime? = null,
    val logisticsNo: String? = null,
    val deliveryDate: LocalDateTime? = null,
    val payNo: String? = null,
    val payType: String? = null,
    val remark: String? = null
) {
    fun toDto(): OrderDto {
        return OrderDto(
            orderId = this.orderId!!,
            orderNo = this.orderNo,
            userId = this.userId,
            originalTotal = this.originalTotal,
            discountedTotal = this.discountedTotal,
            hst = this.hst,
            shippingFee = this.shippingFee,
            orderTotal = this.finalTotal,
            createdAt = this.createdAt,
            orderStatus = this.orderStatus
        )
    }
}