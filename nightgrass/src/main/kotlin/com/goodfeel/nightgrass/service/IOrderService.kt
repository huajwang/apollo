package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.dto.OrderDto
import com.goodfeel.nightgrass.dto.OrderItemDto
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IOrderService {
    fun getOrderById(orderId: Long): Mono<OrderDto>
    fun getOrderItemsByOrderId(orderId: Long): Flux<OrderItemDto>
    fun findOrderById(orderId: Long): Mono<Order>
    fun updateOrder(order: Order): Mono<Order>
}
