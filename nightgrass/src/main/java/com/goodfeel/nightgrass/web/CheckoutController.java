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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                    model.addAttribute("user", tuple.getT1());
                    model.addAttribute("orderItems", tuple.getT2());
                    model.addAttribute("order", tuple.getT3());
                    model.addAttribute("discount", "50");
                    model.addAttribute("discountedTotal", "150");
                    model.addAttribute("shippingDetails", "Delivery to garage");
                    model.addAttribute("estimatedHST", "30");
                    model.addAttribute("orderTotalFinal", "2.01");
                    model.addAttribute("STRIPE_PUBLIC_KEY", stripePublicKey);
                }).thenReturn("checkout");

    }
}
