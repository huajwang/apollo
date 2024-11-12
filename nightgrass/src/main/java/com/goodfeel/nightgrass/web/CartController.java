package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.dto.CartItemDto;
import com.goodfeel.nightgrass.serviceImpl.CartService;
import com.goodfeel.nightgrass.web.util.CartRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping(path = "/cart")
public class CartController {

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
        model.addAttribute("totalPrice", cartService.getTotalPrice());
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
        return cartService.removeProductFromCart(cartRequest.getProductId())
                .then(Mono.fromRunnable(() ->
                        model.addAttribute(
                                "message",
                                "Item removed from the cart successfully!")
                ))
                .thenReturn("redirect:/cart");  // Redirect to the cart view after removal
    }
}
