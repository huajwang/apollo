package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Order;
import com.goodfeel.nightgrass.data.OrderItem;
import com.goodfeel.nightgrass.repo.CartItemRepository;
import com.goodfeel.nightgrass.repo.CartRepository;
import com.goodfeel.nightgrass.repo.OrderItemRepository;
import com.goodfeel.nightgrass.repo.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public Mono<Order> createOrder(String userId, String deliveryAddress, String remark) {
        // Retrieve the cart and items for the user in a reactive way
        return cartRepository.findByUserId(userId)
                .flatMap(cart ->
                        cartItemRepository.findByCartId(cart.getCartId()).collectList()
                                .flatMap(cartItems -> {
                                    // Create the Order entity
                                    Order order = new Order();
                                    order.setUserId(userId);
                                    order.setOrderId(generateOrderId());
                                    order.setTotal(cart.getTotal());
                                    order.setStatus("PENDING");
                                    order.setCreatedAt(LocalDateTime.now());
                                    order.setDeliveryAddress(deliveryAddress);
                                    order.setRemark(remark);
                                    order.setIntroducer(cart.getIntroducer());

                                    // Save the Order entity reactively
                                    return orderRepository.save(order)
                                            .flatMap(savedOrder -> {
                                                // Convert cart items to order items
                                                Flux<OrderItem> orderItems = Flux.fromIterable(cartItems)
                                                        .map(cartItem -> {
                                                            OrderItem orderItem = new OrderItem();
                                                            orderItem.setOrderId(savedOrder.getId());
                                                            orderItem.setProductId(cartItem.getProductId());
                                                            orderItem.setQuantity(cartItem.getQuantity());
                                                            orderItem.setProperties(cartItem.getProperties());
                                                            return orderItem;
                                                        });

                                                // Save all order items reactively and then return the saved order
                                                return orderItemRepository.saveAll(orderItems)
                                                        .then(Mono.just(savedOrder));
                                            });
                                })
                );
    }

    // update order status
    public Mono<Order> updateOrderStatus(Long id, String status) {
        return orderRepository.findById(id)
                .flatMap(order -> {
                    order.setStatus(status);
                    order.setUpdatedDate(LocalDateTime.now());
                    return orderRepository.save(order);
                });
    }

    // Helper method to generate a human-readable order ID with date/time and a unique suffix
    private String generateOrderId() {
        // Format current date-time to a string
        String dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        // Generate a random 4-digit number as a suffix to ensure uniqueness
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return dateTimePart + randomSuffix;
    }
}
