package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Order;
import com.goodfeel.nightgrass.data.OrderItem;
import com.goodfeel.nightgrass.dto.OrderDto;
import com.goodfeel.nightgrass.dto.OrderItemDto;
import com.goodfeel.nightgrass.repo.OrderItemRepository;
import com.goodfeel.nightgrass.repo.OrderRepository;
import com.goodfeel.nightgrass.service.IOrderService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // Retrieve a single order by its ID
    @Override
    public Mono<OrderDto> getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToOrderDto);
    }

    private OrderDto mapToOrderDto(Order order) {
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(order.getOrderId());
        orderDto.setOrderNo(order.getOrderNo());
        orderDto.setOrderTotal(order.getTotal());
        return orderDto;
    }

    // Retrieve all items associated with a specific order ID
    @Override
    public Flux<OrderItemDto> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId)
                .map(this::mapToOrderItemDto);
    }

    @Override
    public Mono<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);

    }

    @Override
    public Mono<Order> updateOrder(Order order) {
        return orderRepository.save(order);
    }

    private OrderItemDto mapToOrderItemDto(OrderItem orderItem) {
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setProductName(orderItem.getProductName());
        orderItemDto.setImageUrl(orderItem.getImageUrl());
        orderItemDto.setQuantity(orderItem.getQuantity());
        orderItemDto.setProperties(orderItem.getProperties());
        orderItemDto.setUnitPrice(orderItem.getUnitPrice());
        return orderItemDto;
    }

}
