package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.OrderItem
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface OrderItemRepository : ReactiveCrudRepository<OrderItem, Long> {
    fun findByOrderId(orderId: Long): Flux<OrderItem>
}
