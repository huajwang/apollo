package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.data.User;
import com.goodfeel.nightgrass.dto.OrderDto;
import com.goodfeel.nightgrass.dto.OrderItemDto;
import com.goodfeel.nightgrass.dto.UserDto;
import com.goodfeel.nightgrass.service.IOrderService;
import com.goodfeel.nightgrass.service.UserService;
import com.goodfeel.nightgrass.web.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
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

    public CheckoutController(IOrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/checkout")
    public Mono<String> showCheckoutPage(@RequestParam Long orderId, Model model) {

        logger.debug("The orderId = {}", orderId);

        Mono<UserDto> userMono = Utility.getCurrentUserId().flatMap(userService::findUserById);
        Mono<List<OrderItemDto>> orderItemsMono = orderService.getOrderItemsByOrderId(orderId).collectList();
        Mono<OrderDto> orderMono = orderService.getOrderById(orderId);

        return Mono.zip(userMono, orderItemsMono, orderMono)
                .doOnNext( tuple -> {
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
                }).thenReturn("checkout");

    }

    @PostMapping("/update-user-info")
    public Mono<ResponseEntity<UserDto>> updateCustomerInfo(@RequestBody UserDto customerInfoDto) {
        return Utility.getCurrentUserId() // Retrieve the current user’s ID
                .flatMap(userService::findUserById)
                .flatMap(user -> {
                    // Update user properties with new data from DTO
                    user.setCustomerName(customerInfoDto.getCustomerName());
                    user.setPhone(customerInfoDto.getPhone());
                    user.setAddress(customerInfoDto.getAddress());

                    // Save updated user information
                    return userService.saveUser(user);
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
