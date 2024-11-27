package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.util.OrderStatus
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface OrderRepository : ReactiveCrudRepository<Order, Long> {
    fun findByOrderStatus(orderStatus: OrderStatus): Flux<Order>
}
