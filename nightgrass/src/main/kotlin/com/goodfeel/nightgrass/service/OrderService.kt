package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.data.OrderItem
import com.goodfeel.nightgrass.dto.OrderDto
import com.goodfeel.nightgrass.dto.OrderItemDto
import com.goodfeel.nightgrass.repo.OrderItemRepository
import com.goodfeel.nightgrass.repo.OrderRepository
import com.goodfeel.nightgrass.util.OrderStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class OrderService(private val orderRepository: OrderRepository, private val orderItemRepository: OrderItemRepository) :
    IOrderService {
    // Retrieve a single order by its ID
    override fun getOrderById(orderId: Long): Mono<OrderDto> {
        return orderRepository.findById(orderId)
            .map { order -> this.mapToOrderDto(order)
            }
    }

    private fun mapToOrderDto(order: Order) = OrderDto(
            orderId = order.orderId!!,
            orderNo = order.orderNo,
            orderTotal = order.total,
            userId = order.userId,
            deliveryAddress = order.deliveryAddress,
            createdAt = order.createdAt,
            orderStatus = order.orderStatus
        )

    // Retrieve all items associated with a specific order ID
    override fun getOrderItemsByOrderId(orderId: Long): Flux<OrderItemDto> {
        return orderItemRepository.findByOrderId(orderId)
            .map { orderItem: OrderItem -> this.mapToOrderItemDto(orderItem) }
    }

    override fun findOrderById(orderId: Long): Mono<Order> {
        return orderRepository.findById(orderId)
    }

    override fun updateOrder(order: Order): Mono<Order> {
        return orderRepository.save(order)
    }

    override fun getOrderByOrderStatus(orderStatus: OrderStatus): Flux<Order> =
        orderRepository.findByOrderStatus(orderStatus)

    private fun mapToOrderItemDto(orderItem: OrderItem) = OrderItemDto(
            productName = orderItem.productName,
            imageUrl = orderItem.imageUrl,
            quantity = orderItem.quantity,
            properties = orderItem.properties,
            unitPrice = orderItem.unitPrice
        )

}
