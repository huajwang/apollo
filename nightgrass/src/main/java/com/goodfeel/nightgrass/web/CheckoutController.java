package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.dto.CustomerAndOrderInfoDto;
import com.goodfeel.nightgrass.dto.OrderDto;
import com.goodfeel.nightgrass.dto.OrderItemDto;
import com.goodfeel.nightgrass.dto.UserDto;
import com.goodfeel.nightgrass.service.IOrderService;
import com.goodfeel.nightgrass.service.UserService;
import com.goodfeel.nightgrass.serviceImpl.ReferralTrackingService;
import com.goodfeel.nightgrass.web.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
public class CheckoutController {

    private final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

    private final IOrderService orderService;
    private final UserService userService;
    private final ReferralTrackingService referralTrackingService;

    public CheckoutController(IOrderService orderService, UserService userService, ReferralTrackingService referralTrackingService) {
        this.orderService = orderService;
        this.userService = userService;
        this.referralTrackingService = referralTrackingService;
    }

    @GetMapping("/checkout")
    public Mono<String> processOrder(@RequestParam Long orderId, Model model, ServerWebExchange exchange) {

        logger.debug("The orderId = {}", orderId);

        Mono<UserDto> userMono = Utility.getCurrentUserId().flatMap(userService::findUserById);
        Mono<List<OrderItemDto>> orderItemsMono = orderService.getOrderItemsByOrderId(orderId).collectList();
        Mono<OrderDto> orderMono = orderService.getOrderById(orderId);

        return Mono.zip(userMono, orderItemsMono, orderMono)
                .flatMap( tuple -> {
                    UserDto user = tuple.getT1();
                    List<OrderItemDto> orderItems = tuple.getT2();
                    OrderDto order = tuple.getT3();

                    BigDecimal originalTotal = order.getOrderTotal().setScale(2, RoundingMode.HALF_UP);

                    // Apply 10% discount
                    BigDecimal discount = originalTotal.multiply(BigDecimal.valueOf(0.10))
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal discountedTotal = originalTotal.subtract(discount)
                            .setScale(2, RoundingMode.HALF_UP);

                    // Apply 13% HST on the discounted total
                    BigDecimal estimatedHST = discountedTotal.multiply(BigDecimal.valueOf(0.13))
                            .setScale(2, RoundingMode.HALF_UP);

                    // Calculate final order total
                    BigDecimal orderTotalFinal = discountedTotal.add(estimatedHST)
                            .setScale(2, RoundingMode.HALF_UP);

                    model.addAttribute("user", user);
                    model.addAttribute("orderItems", orderItems);
                    model.addAttribute("order", order);
                    model.addAttribute("discount", discount);
                    model.addAttribute("discountedTotal", discountedTotal);
                    model.addAttribute("shippingDetails", "Delivery to garage");
                    model.addAttribute("estimatedHST", estimatedHST);
                    model.addAttribute("orderTotalFinal", orderTotalFinal);
                    model.addAttribute("STRIPE_PUBLIC_KEY", stripePublicKey);

                    // Retrieve sharerId from WebSession and calculate reward
                    return exchange.getSession()
                            .flatMap(session -> {
                                String sharerId = session.getAttribute("sharerId");
                                if (sharerId != null) {
                                    BigDecimal reward = originalTotal.multiply(BigDecimal.valueOf(0.10))
                                            .setScale(2, RoundingMode.HALF_UP);
                                    // Persist or process the reward
                                    return referralTrackingService.rewardSharer(sharerId, reward, order.getOrderId());
                                }
                                return Mono.empty();
                            }).thenReturn("checkout");

                });

    }

    @PostMapping("/update-user-info")
    public Mono<ResponseEntity<UserDto>> updateCustomerInfo(@RequestBody CustomerAndOrderInfoDto customerInfoDto) {
        return Utility.getCurrentUserId() // Retrieve the current user’s ID
                .flatMap(userService::findUserById)
                .flatMap(user -> {
                    // Update user properties with new data from DTO
                    user.setCustomerName(customerInfoDto.getCustomerName());
                    user.setPhone(customerInfoDto.getPhone());
                    user.setAddress(customerInfoDto.getAddress());

                    // Save updated user information
                    return userService.updateUserInfo(user);
                })

                .flatMap(updatedUser -> {
                    // Check if an orderId was provided in the request
                    if (customerInfoDto.getOrderId() != null) {
                        // Update the specific order with the new delivery and contact info
                        return orderService.findOrderById(customerInfoDto.getOrderId())
                                .flatMap(order -> {
                                    // Update order’s contact information
                                    order.setContactName(updatedUser.getCustomerName());
                                    order.setContactPhone(updatedUser.getPhone());
                                    order.setDeliveryAddress(updatedUser.getAddress());

                                    // Save the updated order
                                    return orderService.updateOrder(order);
                                })
                                .then(Mono.just(updatedUser)); // Return user info after updating the order
                    } else {
                        // No orderId provided, so just return the updated user info
                        logger.error("No orderId is passed from checkout.html template");
                        return Mono.just(updatedUser);
                    }
                })
                .map(updatedUser -> {
                    // Convert the updated user to UserDto for response
                    UserDto updatedUserDto = new UserDto(
                            updatedUser.getCustomerName(),
                            updatedUser.getPhone(),
                            updatedUser.getAddress()
                    );
                    return ResponseEntity.ok(updatedUserDto);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

}
