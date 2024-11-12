package com.goodfeel.nightgrass.rest;

import com.goodfeel.nightgrass.data.Order;
import com.goodfeel.nightgrass.serviceImpl.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/placeOrder")
    public Mono<Order> placeOrder(@RequestParam String userId, String deliveryAddress, String remark) {
        return orderService.createOrder(userId, deliveryAddress, remark);
    }
}
