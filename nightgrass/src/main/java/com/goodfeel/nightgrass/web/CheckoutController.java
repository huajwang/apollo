package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.data.User;
import com.goodfeel.nightgrass.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Controller
public class CheckoutController {

    private final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    private final IOrderService orderService;

    public CheckoutController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public Mono<String> showCheckoutPage(@RequestParam Long orderId, Model model) {

        User user = new User();
        user.setName("John Doe");
        user.setPhone("1-416-777-8888");
        user.setAddress("555 King Way, Waterloo, ON, N2K3Z5");
        model.addAttribute("user", user); // from a service or repository

        logger.debug("The orderId = {}", orderId);
        model.addAttribute("orderItems", orderService.getOrderItemsByOrderId(orderId));
        model.addAttribute("order", orderService.getOrderById(orderId));
        model.addAttribute("discount", "50");
        model.addAttribute("discountedTotal", "150");
        model.addAttribute("shippingDetails", "Delivery to garage");
        model.addAttribute("estimatedHST", "30");
        model.addAttribute("orderTotalFinal", "505");

        return Mono.just("checkout"); // "checkout" refers to the checkout.html file in resources/templates
    }
}
