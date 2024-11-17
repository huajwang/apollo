package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.data.Order;
import com.goodfeel.nightgrass.dto.OrderDto;
import com.goodfeel.nightgrass.dto.OrderItemDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IOrderService {
    Mono<OrderDto> getOrderById(Long orderId);
    Flux<OrderItemDto> getOrderItemsByOrderId(Long orderId);
    Mono<Order> findOrderById(Long orderId);
    Mono<Order> updateOrder(Order order);
}
