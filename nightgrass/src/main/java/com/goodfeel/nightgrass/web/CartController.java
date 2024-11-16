package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.dto.CartItemDto;
import com.goodfeel.nightgrass.serviceImpl.CartService;
import com.goodfeel.nightgrass.web.util.CartItemUpdateRequest;
import com.goodfeel.nightgrass.web.util.CartRequest;
import com.goodfeel.nightgrass.web.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(path = "/cart")
public class CartController {

    private final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // View cart contents
    @GetMapping
    public Mono<String> viewCart(Model model) {
        Flux<CartItemDto> cartItems = cartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartService.getTotalPrice());
        model.addAttribute("STRIPE_PUBLIC_KEY", stripePublicKey); // TODO NOT NEEDED HERE?
        return Mono.just("shopping-cart"); // Thymeleaf template for viewing the cart
    }

    @PostMapping(path = "/add")
    public Mono<String> addToCart(@ModelAttribute CartRequest cartRequest, Model model) {
        return cartService.addProductToCart(cartRequest.getProductId())
                .thenReturn("redirect:/product/detail?id=" + cartRequest.getProductId());
    }

    @PostMapping(path = "/remove")
    public Mono<String> removeFromCart(@ModelAttribute CartRequest cartRequest, Model model) {
        return cartService.removeCartItemFromCart(cartRequest.getItemId())
                .then(Mono.fromRunnable(() ->
                        model.addAttribute(
                                "message",
                                "Item removed from the cart successfully!")
                ))
                .thenReturn("redirect:/cart");  // Redirect to the cart view after removal
    }

    @PostMapping("/update-quantity")
    public Mono<ResponseEntity<String>> updateQuantity(@RequestBody CartItemUpdateRequest updateRequest) {
        logger.debug("Received updateRequest request body from front end ajax call: {}", updateRequest);
        return cartService.updateQuantity(updateRequest.getItemId(), updateRequest.getQuantity())
                .map(updated -> ResponseEntity.ok("Quantity updated"))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found"));
    }

    @PostMapping("/update-total-on-checkbox")
    public Mono<ResponseEntity<Map<String, String>>> updateTotal(@RequestBody CartItemUpdateRequest request) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cart total updated on checkbox change");
        return cartService.updateCartTotal(request.getItemId(), request.getIsChecked())
                .thenReturn(ResponseEntity.ok(response));
    }

    @PostMapping("/checkout")
    public Mono<ResponseEntity<Map<String, Long>>> checkout() {
        return Utility.getCurrentUserId().flatMap( userId ->

                cartService.checkout(userId)
                        .map(order -> {
                            Map<String, Long> response = new HashMap<>();
                            response.put("orderId", order.getOrderId());
                            return ResponseEntity.ok(response);
                        })
                        .onErrorResume(e -> {
                            Map<String, Long> errorResponse = new HashMap<>();
                            errorResponse.put("orderId", null);
                            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                        })
        );
    }

    @GetMapping("/total")
    public Mono<ResponseEntity<BigDecimal>> getCartTotal() {
        return cartService.getTotalPrice()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(BigDecimal.ZERO));
    }

}
